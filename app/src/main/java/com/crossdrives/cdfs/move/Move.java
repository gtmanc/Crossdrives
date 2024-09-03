package com.crossdrives.cdfs.move;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.MetaDataUpdater;
import com.crossdrives.cdfs.allocation.QuotaEnquirer;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.drivehelper.Download;
import com.crossdrives.cdfs.drivehelper.Upload;
import com.crossdrives.cdfs.exception.InvalidArgumentException;
import com.crossdrives.cdfs.exception.ItemNotFoundException;
import com.crossdrives.cdfs.function.SliceConsumer;
import com.crossdrives.cdfs.function.SliceSupplier;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.collection.Diff;
import com.crossdrives.cdfs.util.strings.Strings;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Move {
    final String TAG = "CD.Move";
    final CDFS mCDFS;
    final CdfsItem mItems;
    final CdfsItem mSource, mDest;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    public enum State{
        GET_MAP_STARTED,
        GET_MAP_COMPLETE,
        MAP_UPDATE_STARTED,
        MAP_UPDATE_COMPLETE,
        MOVE_IN_PROGRESS,
        MOVE_COMPLETE,
    }

    Move.State mState;
    IMoveItemProgressListener mListener;
    int progressTotalSegment = 0;
    int progressTotalDeleted = 0;

    DebugPrintOut po = new DebugPrintOut();
//    private class ConsumeModel{
//        public Drive drive;
//        public com.google.api.services.drive.model.File fileMetadata;
//        public java.io.File localFile;
//    }

    File poison_pill;
    final String POISON_PILL = "PoisonPill";


    public Move(CDFS cdfs, CdfsItem item, CdfsItem source, CdfsItem dest) {
        this.mCDFS = cdfs;
        this.mItems = item;
        this.mSource = source;
        this.mDest = dest;

        poison_pill = new File();
        poison_pill.setFile(new com.google.api.services.drive.model.File());
        poison_pill.getFile().setName(POISON_PILL);
    }

    /*
        Assumptions:
        1. Only valid CFDS item is selected via UI (List screen)
        2. The dest item must be a folder
        3. The signed in driver client(s) must contain the drive client(s) that the dest folder was created by as well as the source
    */
    public Task<File> execute() {
        Task<com.crossdrives.driveclient.model.File> task;

        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public com.crossdrives.driveclient.model.File call() throws Exception {
                //get the maps for both source and destination because we will need to update the maps according to
                //the change we will made
                HashMap<String, AllocContainer> containerSrc = getMapContainers(mSource);
                HashMap<String, AllocContainer> containerDest = getMapContainers(mDest);
                //po.out("Container source:", containerSrc);
                //po.out("Container destination:", containerDest);

                String destParent = mDest.getPath().concat(mDest.getName());
                Log.d(TAG, "Dest path:" + destParent);
                //Build up the list for the items that are moved.
                //Note that an empty list could present in the returned hashmap
                HashMap<String, Collection<AllocationItem>> itemsParentPathUpdated = updateParentPathAll(getAllocItemsAllById(containerSrc, mItems.getId()), destParent);
                //print out for debug
                Log.d(TAG, "Items that parentPath updated:");
                itemsParentPathUpdated.entrySet().stream().forEach((set)->{
                    Log.d(TAG, "drive: " + set.getKey() + " list size: " + set.getValue().size());
                    set.getValue().forEach((item)-> Log.d(TAG, "name: " + item.getName() + " parent: " + item.getPath()));
                });

                //build up the new containers only for source right here. Simply remove all of the items.
                //The containers of destination will be processed later because they are more complicated.
                ContainerUtil containerUtil = new ContainerUtil();
                //containerSrc will be alterred after call of containerUtil.removeItems
                containerSrc = containerUtil.removeItems(containerSrc, itemsParentPathUpdated);
                //po.out("new container to source:", newContainerSrc);
                //po.out("original source container:", containerSrc);
//                Log.d(TAG, "drives of dest:");
//                mDest.getMap().keySet().stream().forEach((k)->Log.d(TAG, k));
//                Log.d(TAG, "moved items:");
//                movedItems.entrySet().stream().forEach((set)->{
//                    Log.d(TAG, set.getKey());
//                    set.getValue().stream().forEach((v)->Log.d(TAG, v.getName()));
//                });
                Strings srcDriveNames = new Strings(mItems.getMap().keySet());
                Collection<String> srcDrivesDestContained = srcDriveNames.contains(mDest.getMap().keySet());
                Collection<String> srcDrivesDestNotContained = srcDriveNames.notContains(mDest.getMap().keySet());
                Log.d(TAG, "Src drives contained in Dest:");

                srcDrivesDestContained.stream().forEach((s)->{
                    Log.d(TAG, s);
                });
                Log.d(TAG, "Src drives NOT contained in Dest:");
                srcDrivesDestNotContained.stream().forEach((s)->{
                    Log.d(TAG, s);
                });
                //======================================================================================
                //following are critical process which must be atomic. However, we can't guarantee this.
                //A recovery process will be employed to solve the issue.

                //if the drive clients of source and dest item are identical, simply,
                //get the items we are interested according to the CDFS item that to be moved.
                //Then update the field parentPath of the items with the dest parent path.
                //Otherwise, the further handling needs to be taken
                MapUpdater mapUpdater2 = null;
                MapUpdater mapUpdater3 = null;

                Log.d(TAG, "Update new container to source. We are in debugging. skip");
                MapUpdater mapUpdater1 = new MapUpdater(NameMatchedDrives(mSource.getMap().keySet()));
                //mapUpdater1.updateAll(newContainerSrc, mSource).join();
                //mapUpdater1.updateAll(containerSrc, mSource).join();



                if(!srcDrivesDestContained.isEmpty()){
                    Log.d(TAG, "Proceed with drives identical");
                    //
                    // The drives of source and destination are identical. Simply modify the metedata
                    //
                    HashMap<String, AllocContainer> containers = new HashMap<>();
                    containerSrc.entrySet().stream().forEach((set)->{
                        containers.put(set.getKey(), set.getValue());
                    });

                    HashMap<String, Drive> drivesInvolved = NameMatchedDrives(srcDrivesDestContained);
                    MetaDataUpdater metaDataUpdater = new MetaDataUpdater(drivesInvolved);
                    metaDataUpdater.parent(mSource.getMap(), mDest.getMap()).run(mItems.getMap()).join();

                    //Take out the containers we don't need to proceed
                    HashMap<String, AllocContainer> container = toKeyMatched(srcDrivesDestContained, containerDest);
                    //Add the parent path items
                    HashMap<String, AllocContainer> newContainerDest = containerUtil.addItems(container, itemsParentPathUpdated);
                    po.out("New container to dest:", newContainerDest);

                    mapUpdater2 = new MapUpdater(NameMatchedDrives(srcDrivesDestContained));
                    Log.d(TAG, "Update new container to dest");
                    mapUpdater2.updateAll(newContainerDest, mDest).join();
                }
                
                if(!srcDrivesDestNotContained.isEmpty()){
                    Log.d(TAG, "Proceed with the items drives NOT identical");
                    Strings destDriveNames = new Strings(mDest.getMap().keySet());
                    Collection<String> drivesUsedAllocate = destDriveNames.notContains(mItems.getMap().keySet());
                    //Filter out the items that has been processed in previous operation
//                    Map<String, List<AllocationItem>> items =
//                    Mapper.reValue(containerSrc, (key, container)->{
//                        return container.getAllocItem();
//                    }).entrySet().stream().filter((set)->{
//                        return srcDrivesDestNotContained.contains(set.getKey());
//                    }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
                    Map<String, Collection<AllocationItem>> items =
                            itemsParentPathUpdated.entrySet().stream().filter((entry)->{
                        return srcDrivesDestNotContained.contains(entry.getKey());
                    }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

                    //Debug
                    Log.d(TAG, "Items to transfer:");
                    items.entrySet().stream().forEach((set)->{
                        Log.d(TAG, "drive: " + set.getKey() + "  list:" + set.getValue().toString());
                        set.getValue().stream().forEach((item)->{Log.d(TAG, item.getName());});
                    });

                    HashMap<String, List<AllocationItem>> itemsToAllocated=
                            Mapper.reValue(new HashMap<>(items), (item)->{
                                return new ArrayList(item);});
                    HashMap<String, List<AllocationItem>> allocResult =
                            allocate(NameMatchedDrives(drivesUsedAllocate), itemsToAllocated);

                    Log.d(TAG, "Allocation result:");
                    allocResult.entrySet().stream().forEach((set)->{
                        Log.d(TAG, "drive: " + set.getKey());
                        set.getValue().stream().forEach((item)->{Log.d(TAG, item.getName());});
                    });

                    // Now, we can do the transfer according to the allocation result
                    Log.d(TAG, "Transfer items... We are in debugging. skip!");
                    Collection<File> transferResult = new ArrayList<>();
                    // transfer(itemsToAllocated, allocResult).join();

                    //Take out the containers we don't need to proceed
                    HashMap<String, AllocContainer> container = toKeyMatched(srcDrivesDestNotContained, containerDest);

                    //update the properties according to the result the transfer.
                    // The result should be identical to allocation result pass to the transfer at beginning
                    HashMap<String, Collection<AllocationItem>> updateItems = updateProperty(itemsParentPathUpdated, transferResult);
                    HashMap<String, AllocContainer> newContainerDest = containerUtil.addItems(container, updateItems);

                    mapUpdater3 = new MapUpdater(NameMatchedDrives(srcDrivesDestNotContained));
                    Log.d(TAG, "Update new container to dest. We are in debugging... skip");
                    //mapUpdater3.updateAll(newContainerDest, mDest).join();
                }

                //MapFetcher is not thread safe. #54
//                Collection<CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>>> futures =
//                new ArrayList<>();
//                futures.add(mapUpdater1.updateAll(newContainerSrc, mSource));
//                futures.add(mapUpdater2.updateAll(newContainerDest, mDest));
//                futures.stream().forEach((future)->{
//                    future.join();
//                });



                return null;
                }
        });
        return task;
    }

    private HashMap<String, AllocContainer> getMapContainers(CdfsItem parent) throws InvalidArgumentException {
        HashMap<String, OutputStream> maps;

        HashMap<String, Drive> drivesInvolved =
                NameMatchedDrives(parent.getMap().keySet());

        //No drive client to continue. Simply stop right here
        if(drivesInvolved.size()==0){
            throw new InvalidArgumentException("No drive client to continue!", new Throwable());
        }

        //callback(Delete.State.GET_MAP_STARTED);
        MapFetcher mapFetcher = new MapFetcher(drivesInvolved);
        CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(parent);
        maps = mapsFuture.join();

        //map to allocation container so that we can easily process later
        return Mapper.reValue(maps, (stream)->{
            return AllocManager.toContainer(stream);
        });
    }

    private HashMap<String, Collection<AllocationItem>> getAllocItemsAllById(final HashMap<String, AllocContainer> containers, final String id){

        return Mapper.reValue(containers, container->{
            return getAllocItemsById(container, id);
        });
    }

    private Collection<AllocationItem> getAllocItemsById(AllocContainer container, String id){
        return container.getAllocItem().stream().filter((item)->{
                                return item.getCdfsId().equals(id);
                            }).collect(Collectors.toCollection(ArrayList::new));
    }

    private HashMap<String, Collection<AllocationItem>> updateParentPathAll(HashMap<String, Collection<AllocationItem>> items, final String parentPath){

        return Mapper.reValue(items, collection->{
            Iterator<AllocationItem> iterator = collection.iterator();
            while(iterator.hasNext()){
                iterator.next().setPath(parentPath);
            }
            return collection;
        });
    }

    private HashMap<String, List<AllocationItem>> allocate(HashMap<String, Drive> drives,
                                                          HashMap<String, List<AllocationItem>> src){
        HashMap<String, About.StorageQuota> quotaMap = null;
        QuotaEnquirer enquirer = new QuotaEnquirer(drives);
        try {
            quotaMap  = Tasks.await(enquirer.getAll());
        } catch (ExecutionException | InterruptedException e) {
            Log.w(TAG, "get quota failed! " + e.getMessage());
            return null;
        }

        Allocator allocator = new Allocator(quotaMap, src);
        return allocator.allocateItems();
    }

    //
    // A blocking process
    // Input:
    //      items: source items to be transferred
    //      allocation: allocation
    private CompletableFuture<Collection<File>> transfer(
            HashMap<String, List<AllocationItem>> items, HashMap<String, List<AllocationItem>> allocation){
        LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<>();
        Boolean[] finished = new Boolean[0];
        finished[0] = false;
        HashMap<String, Drive> clients = NameMatchedDrives(items.keySet().stream().collect(Collectors.toCollection(ArrayList::new)));
        SliceSupplier<AllocationItem, Download.Downloaded> supplier = new SliceSupplier<AllocationItem, Download.Downloaded>(clients, items,
                        (ai)->{
            Download downloader = new Download(mCDFS.getDrives().get(ai.getDrive()).getClient());
            return downloader.runAsync(ai);
                        }).setCallback(new SliceSupplier.ISliceConsumerCallback<Download.Downloaded>() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSupplied(Download.Downloaded downloaded) throws IOException {
                String driveName = downloaded.item.getDrive();
                String sliceName = downloaded.item.getName();
                OutputStream os = downloaded.os;
                File file = new File();
                file.setDriveName(driveName);
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setParents(Collections.singletonList(mDest.getMap().get(driveName).get(0)));
                fileMetadata.setName(sliceName);
                file.setFile(fileMetadata);
                //This is necessary. The number will provide the information so that we can update correct items
                // in propertu update.
                file.setInteger(downloaded.item.getSequence());
                file.setOriginalLocalFile(toFile(os, sliceName, downloaded.item.getSequence()));
                queue.add(file);
            }

            @Override
            public void onCompleted(int totalSliceSupplied) {
                queue.add(poison_pill);
            }

            @Override
            public void onFailure(String reason) {

            }
        });

        SliceConsumer<File, File> consumer = new SliceConsumer<File, File>(clients,
                (file)->{
                    String driveName = file.getDriveName();
                    Upload uploader = new Upload(mCDFS.getDrives().get(driveName).getClient());
                    return uploader.runAsync(file);
                }).setCallback(new SliceConsumer.ISliceConsumerCallback<File, File>() {
            @Override
            public void onStart() {

            }

            @Override
            public File onRequested() {
                File file = queue.peek();
                if(file.getFile().getName().equals(POISON_PILL)){return null;}
                return file;
            }

            @Override
            public void onConsumed(com.crossdrives.driveclient.model.File file) {

            }

            @Override
            public void onCompleted(Collection<File> consumed) {
                finished[0] = true;
            }

            @Override
            public void onFailure(String reason) {

            }
        });

        //fire!
        supplier.run();
        consumer.run();

        return CompletableFuture.supplyAsync(()->{
            while(!finished[0]);
            return consumer.getConsumed();
        });
    }

    private java.io.File toFile(OutputStream os, String sliceName, int sliceNo) throws IOException {
        Context context = SnippetApp.getAppContext();
        FileOutputStream fOut = context.openFileOutput(sliceName, Activity.MODE_PRIVATE);
        ByteArrayOutputStream bos = (ByteArrayOutputStream)os;
        ByteArrayInputStream bis = new ByteArrayInputStream( bos.toByteArray());

        int rd_len, offset = 0;
        byte[] buf = new byte[1024];
        while ( (rd_len = bis.read(buf)) >= 0){
            fOut.write(buf, 0, rd_len);
            offset += rd_len;
        }

        bis.close();
        bos.close();
        os.close();
        fOut.close();

        return new java.io.File(context.getFilesDir().getPath() + "/" + sliceName);
    }

    /*
        The properties in the input items will be updated according to the transferred items:
        1. drive (drive name)
        2. Item ID (item id in user drive)

        Input:
        items:          items will get updated
        transferred:    items transferred

     */
    HashMap<String, Collection<AllocationItem>> updateProperty(HashMap<String, Collection<AllocationItem>> items, Collection<File> transferred){
        //remap the result to a Map so that we can easily proceed in next step
        Map<String, AllocationItem> remaped = transferred.stream().map((f)->{
            Map.Entry<String, AllocationItem> e = new Map.Entry<String, AllocationItem>() {
                @Override
                public String getKey() {
                    return f.getDriveName();
                }

                @Override
                public AllocationItem getValue() {
                    AllocationItem ai = new AllocationItem();
                    ai.setName(f.getDriveName());
                    ai.setItemId(f.getFile().getId());
                    ai.setSequence(f.getInteger());
                    return null;
                }

                @Override
                public AllocationItem setValue(AllocationItem allocationItem) {
                    return null;
                }
            };
            return e;
        }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        return com.crossdrives.cdfs.util.map.Mapper.toColletion(remaped);
    }

    /*
        Add/Remove items to/from the container
        Only the items that the drive are identical to the container will be processed.
    */
    class ContainerUtil{
        public HashMap<String, AllocContainer> addItems(HashMap<String, AllocContainer> containers,
                                                         HashMap<String, Collection<AllocationItem>> items) {
            return Mapper.reValue(containers, (key,container) -> {
                if(items.get(key) != null){ container.addItems(items.get(key));    }
                //po.out("New container to dest: ", container);
                return container;
            });
        }

        public HashMap<String, AllocContainer> removeItems(HashMap<String, AllocContainer> containers,
                                                            HashMap<String, Collection<AllocationItem>> items) {

           return Mapper.reValue(containers, (key,container) -> {
               if(items.get(key) != null){container.removeItems(items.get(key));}
                //po.out("New container to source: ", container);
               return container;
            });

        }
    }

    public Move.State getState(){return mState;}

    public int getProgressMax(){return progressTotalSegment;}

    public int getProgressCurrent(){return progressTotalDeleted;}

    private boolean isIdentical(ConcurrentHashMap<String, List<String>> m1, ConcurrentHashMap<String, List<String>> m2){
        return m1.keySet().equals(m2.keySet());
    }

    private Collection<String> stringsNotContained(Collection<String> src,
                                                   Collection<String> dest){

        return dest.stream().filter((key)->{
            return !src.contains(key);
        }).collect(Collectors.toList());
    }

    private Collection<String> stringsContained(Collection<String> src,
                                                Collection<String> dest){

        return dest.stream().filter((key)->{
            return src.contains(key);
        }).collect(Collectors.toList());
    }

    private Collection<String> diffDrive(ConcurrentHashMap<String, List<String>> m1, ConcurrentHashMap<String, List<String>> m2){
        //Collection<String> diff =
          return Diff.between(m1.keySet().stream().collect(Collectors.toList()),
                m2.keySet().stream().collect(Collectors.toList()),  (e1, e2)->{
            return e1.equals(e2);
        });
    }

    private HashMap<String, Drive> NameMatchedDrives(Collection<String> names){
        return toKeyMatched(names, mCDFS.getDrives());
    }

    private <T> HashMap<String, T> toKeyMatched(Collection<String> keys, HashMap<String, T> dest){
        HashMap<String, T> drivesInvolved = new HashMap<>();
        keys.stream().forEach((key)->{
            drivesInvolved.put(key, dest.get(key));
        });
        return drivesInvolved;
    }

    class DebugPrintOut{
        void out(String head, HashMap<String, AllocContainer> container){
            Log.d(TAG, head);
            container.entrySet().stream().forEach((set)->{
                Log.d(TAG, "drive: " + set.getKey());
                set.getValue().getAllocItem().stream().forEach((item)->{
                    Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
                });
            });
            Log.d(TAG, "");
        }

        void out(String head, AllocContainer container){
            Log.d(TAG, head);
            container.getAllocItem().stream().forEach((item)->{
                Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
            });
        }
    }
}
