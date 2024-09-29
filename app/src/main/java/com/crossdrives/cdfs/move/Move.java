package com.crossdrives.cdfs.move;

import static com.crossdrives.cdfs.allocation.util.Mapper.toHashMap;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.MetaDataUpdater;
import com.crossdrives.cdfs.allocation.QuotaEnquirer;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.drivehelper.Delete;
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
import com.crossdrives.cdfs.util.print.Printer;
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
    Printer dp = new Printer(TAG);

    public enum State{
        GET_MAP_STARTED,
        GET_MAP_COMPLETE,
        SRC_MAP_UPDATE_STARTED,
        SRC_MAP_UPDATE_COMPLETE,
        MOVE_IN_PROGRESS,   //this is only applicable if transfer between drives is performed
        MOVE_COMPLETE,
        DEST_MAP_UPDATE_STARTED,
        DEST_MAP_UPDATE_COMPLETED
    }

    Move.State mState;
    IMoveItemProgressListener mListener;
    int progressTotal = 0;
    int progressActual = 0;

    //Model carries out all of necessary info for move
    public class TransferItemModel{
        public String destDriveName;
        public String newId;
        public java.io.File localFile;
        AllocationItem ai;
    }

    TransferItemModel poison_pill;
    final String POISON_PILL = "PoisonPill";


    public Move(CDFS cdfs, CdfsItem item, CdfsItem source, CdfsItem dest, @NonNull IMoveItemProgressListener listener) {
        this.mCDFS = cdfs;
        this.mItems = item;
        this.mSource = source;
        this.mDest = dest;
        this.mListener = listener;

        poison_pill = new TransferItemModel();
        poison_pill.ai = new AllocationItem();
        poison_pill.ai.setName(POISON_PILL);
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
                callback(State.GET_MAP_STARTED);
                HashMap<String, AllocContainer> containerSrc = getMapContainers(mSource);
                HashMap<String, AllocContainer> containerDest = getMapContainers(mDest);
                //po.out("Container source:", containerSrc);
                //po.out("Container destination:", containerDest);
                callback(State.GET_MAP_COMPLETE);

                String destParent = mDest.getPath().concat(mDest.getName());
                Log.d(TAG, "Dest path:" + destParent);
                //Build up the list for the items that are moved.
                //Note that an empty list could present in the returned hashmap
                HashMap<String, Collection<AllocationItem>> itemsParentPathUpdated = updateParentPathAll(getAllocItemsAllById(containerSrc, mItems.getId()), destParent);
                //print out for debug
                dp.getAllocationItem().out("Items that parentPath updated:", itemsParentPathUpdated, "");
//                Log.d(TAG, "Items that parentPath updated:");
//                itemsParentPathUpdated.entrySet().stream().forEach((set)->{
//                    Log.d(TAG, "drive: " + set.getKey() + " list size: " + set.getValue().size() + " cdfs name: " + item.getName());
//                    set.getValue().forEach((item)-> Log.d(TAG, "name: " +  + " parent: " + item.getPath()));
//                });

                //build up the new containers only for source right here. Simply remove all of the items.
                //The containers of destination will be processed later because they are more complicated.
                ContainerUtil containerUtil = new ContainerUtil();
                //containerSrc will be altered after call of containerUtil.removeItems
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

                Log.d(TAG, "Update new container to source...");
                callback(State.SRC_MAP_UPDATE_STARTED);
                MapUpdater mapUpdater1 = new MapUpdater(NameMatchedDrives(mSource.getMap().keySet()));
                mapUpdater1.updateAll(containerSrc, mSource).join();
                callback(State.SRC_MAP_UPDATE_COMPLETE);

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
                    dp.getContainer().out("New container to dest:", newContainerDest,"");

                    callback(State.DEST_MAP_UPDATE_STARTED);
                    mapUpdater2 = new MapUpdater(NameMatchedDrives(srcDrivesDestContained));
                    Log.d(TAG, "Update new container to dest");
                    mapUpdater2.updateAll(newContainerDest, mDest).join();
                    callback(State.DEST_MAP_UPDATE_COMPLETED);
                }
                
                if(!srcDrivesDestNotContained.isEmpty()){
                    Log.d(TAG, "Proceed with the items drives NOT identical");
                    Strings destDriveNames = new Strings(mDest.getMap().keySet());
                    Collection<String> drivesUsedAllocate =// destDriveNames.notContains(mItems.getMap().keySet());
                    mDest.getMap().keySet().stream().collect(Collectors.toCollection(ArrayList::new));
                    Log.d(TAG, "Drives used for allocate:");
                    drivesUsedAllocate.stream().forEach(n -> Log.d(TAG, n));
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
                    dp.getAllocationItem().out("Items to transfer:", new HashMap<>(items), "");
//                    Log.d(TAG, "Items to transfer:");
//                    items.entrySet().stream().forEach((set)->{
//                        Log.d(TAG, "drive: " + set.getKey() + "  list:" + set.getValue().toString());
//                        set.getValue().stream().forEach((item)->{Log.d(TAG, item.getName());});
//                    });

                    HashMap<String, List<AllocationItem>> itemsToAllocated=
                            Mapper.reValue(new HashMap<>(items), (item)->{
                                return new ArrayList(item);});

                    HashMap<String, List<AllocationItem>> allocResult =
                            allocate(NameMatchedDrives(drivesUsedAllocate), itemsToAllocated);

                    Log.d(TAG, "Allocation result: " + allocResult);
//                    allocResult.entrySet().stream().forEach((set)->{
//                        Log.d(TAG, "drive: " + set.getKey());
//                        set.getValue().stream().forEach((item)->{Log.d(TAG, item.getName());});
//                    });

                    // Now, we can do the transfer according to the allocation result
                    Log.d(TAG, "Transfering items...");
                    Collection<TransferItemModel> transferResult = transfer(itemsToAllocated, allocResult).join();


                    //update the properties according to the result the transfer.
                    // The result should be identical to allocation result pass to the transfer at beginning
                    HashMap<String, Collection<AllocationItem>> updateItems = updateProperty(transferResult);

                    dp.getAllocationItem().out("Items to dest container: ", updateItems,null);
                    //PRINT OUT
//                    Log.d(TAG, "Items to dest container:");
//                    updateItems.entrySet().stream().forEach((set)->{
//                        Log.d(TAG, "drive: " + set.getKey() + "  list:" + set.getValue().toString());
//                        set.getValue().stream().forEach((item)->{
//                            Log.d(TAG,"drive: " + item.getDrive() + " name:" + item.getName() + " seq: " + item.getSequence());
//                        });
//                    });


                    //HashMap<String, AllocContainer> container = toKeyMatched(updateItems.keySet(), containerDest);
                    containerDest = containerUtil.addItems(containerDest, updateItems);

                    mapUpdater3 = new MapUpdater(NameMatchedDrives(updateItems.keySet()));
                    Log.d(TAG, "Update new container to dest. involved drves: " + NameMatchedDrives(updateItems.keySet()));
                    mapUpdater3.updateAll(containerDest, mDest).join();
                    callback(State.MOVE_COMPLETE);
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
    private CompletableFuture<Collection<TransferItemModel>> transfer(
            HashMap<String, List<AllocationItem>> items, HashMap<String, List<AllocationItem>> allocation){
        LinkedBlockingQueue<TransferItemModel> queue = new LinkedBlockingQueue<>();
        HashMap<String, Drive> clients = NameMatchedDrives(items.keySet().stream().collect(Collectors.toCollection(ArrayList::new)));
        SliceSupplier<AllocationItem, Download.Downloaded> supplier = new SliceSupplier<AllocationItem, Download.Downloaded>(clients, items,
                        (ai)->{
            Log.d(TAG, "downloading item: " + ai.getName() + " seq: " + ai.getSequence());
            Download downloader = new Download(mCDFS.getDrives().get(ai.getDrive()).getClient());
            return downloader.runAsync(ai);
                        }).setCallback(new SliceSupplier.ISliceConsumerCallback<Download.Downloaded>() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSupplied(Download.Downloaded downloaded) {
                Log.d(TAG, "onSupplied: " + downloaded.item.getName() + " seq: " + downloaded.item.getSequence());
                String driveName = getDriveName(allocation, downloaded.item.getItemId());
                Log.d(TAG, "target drive to transfer: " + driveName);
                String sliceName = downloaded.item.getName();
                TransferItemModel tim = new TransferItemModel();
                tim.destDriveName = driveName;
                tim.ai = downloaded.item;
                try {
                    tim.localFile = toFile(downloaded.os, sliceName, downloaded.item.getSequence());
                    Log.d(TAG, "local file from os: " + tim.localFile);
                } catch (IOException e) {
                    Log.w(TAG, e.getMessage());
                    throw new RuntimeException(e);
                }
                Log.d(TAG, "put item");
                try {
                    queue.put(tim);
                } catch (InterruptedException e) {
                    Log.w(TAG, e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleted(int totalSliceSupplied) {
                Log.d(TAG, "put poison. size of q: " + queue.size());
                try {
                    queue.put(poison_pill);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(String reason) {

            }
        });

        SliceConsumer<TransferItemModel, TransferItemModel> consumer = new SliceConsumer<TransferItemModel, TransferItemModel>(clients,
                (ti)->{
                    Log.d(TAG, "Uploading...");
                    File file = new File();
                    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                    Log.d(TAG, "dest drive: " + ti.destDriveName);
                    fileMetadata.setParents(Collections.singletonList(mDest.getMap().get(ti.destDriveName).get(0)));
                    String name = ti.localFile.getName();
                    Log.d(TAG, "name: " + ti.ai.getName());
                    fileMetadata.setName(name);
                    file.setFile(fileMetadata);
                    Log.d(TAG, "local file: " + ti.localFile);
                    file.setOriginalLocalFile(ti.localFile);
                    //This is necessary. The number will provide the information so that we can update correct items
                    // in propertu update.
                    //file.setInteger(downloaded.item.getSequence());
                    Upload uploader = new Upload(mCDFS.getDrives().get(ti.destDriveName).getClient());
                    Upload.AdditionalData data = new Upload.AdditionalData();
                    data.object = ti.ai;
                    uploader.setAddtionalData(data);
                    return CompletableFuture.supplyAsync(()->{
                        //Log.d(TAG, "upload run!");
                        Upload.Uploaded uploaded = uploader.runAsync(file).join();
                        TransferItemModel r = new TransferItemModel();
                        r.destDriveName = ti.destDriveName;
                        r.newId = uploaded.file.getFile().getId();
                        r.localFile = uploaded.file.getOriginalLocalFile();
                        Log.d(TAG, "uploaded done. dest drive: " + r.destDriveName + " Id: " + r.newId);
                        r.ai = (AllocationItem) uploaded.data.object;
                        return r;
                    });
                }).setCallback(new SliceConsumer.ISliceConsumerCallback<TransferItemModel, TransferItemModel>() {
            @Override
            public void onStart() {

            }

            @Override
            public TransferItemModel onRequested(){
                Log.d(TAG, "onRequested");
                TransferItemModel ti;

                try {
                    ti = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Log.d(TAG, "slice to consumed: " + ti.ai.getName());
                if(ti.ai.getName().equals(POISON_PILL)){return null;}
                return ti;
            }

            @Override
            public void onConsumed(TransferItemModel ti) {
                Log.d(TAG, "onConsumed: " + ti.ai.getName() + " seq: " + ti.ai.getSequence());
                progressActual++;
                callback(State.MOVE_IN_PROGRESS);
                Context context = SnippetApp.getAppContext();
                //Log.d(TAG, "deleting file: " + ti.localFile.getName());
                if(!context.deleteFile(ti.localFile.getName())){Log.w(TAG, "failed to delete local file: " + ti.ai.getName());}
                //Log.d(TAG, "deleted.");
            }

            @Override
            public void onCompleted(Collection<TransferItemModel> consumed) {
                Log.d(TAG, "onCompleted. Number of consumed: " + consumed.size());
            }

            @Override
            public void onFailure(String reason) {
                Log.w(TAG, "onFailure: " + reason);
            }
        });

        //fire!
        Log.d(TAG, "FIRE!!!");
        progressTotal = items.size();
        callback(State.MOVE_IN_PROGRESS);

        Collection<CompletableFuture> futures = new ArrayList<>();
        futures.add(supplier.run());
        futures.add(consumer.run());
        return CompletableFuture.supplyAsync(()->{
            //Collection<CompletableFuture<Delete.Deleted>> futureDeletion = new ArrayList<>();
            futures.stream().forEach((f)->f.join());
            deleteItems(consumer.getConsumed()).stream().forEach((f)->f.join());

//            consumer.getConsumed().stream().forEach((ti)->{
//                String driveName = ti.ai.getDrive();
//                Log.d(TAG, "delete item. drive: " + driveName +" name: " + ti.ai.getName());
//                Delete deleter = new Delete(mCDFS.getDrives().get(driveName).getClient());
//                File file = new File();
//                com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
//                metaData.setId(ti.ai.getItemId());
//                file.setFile(metaData);
//                futureDeletion.add(deleter.runSync(file));
//            });
//
//            futureDeletion.stream().forEach((f)->f.join());

            return consumer.getConsumed();//.stream().map((ti)-> ti.file).collect(Collectors.toCollection(ArrayList::new));
        });
    }

    private java.io.File toFile(OutputStream os, String sliceName, int sliceNo) throws IOException {
        Context context = SnippetApp.getAppContext();
        String name = sliceName + "_" + Integer.toString(sliceNo);
        FileOutputStream fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
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

        return new java.io.File(context.getFilesDir().getPath() + "/" + name);
    }

    private String getDriveName(HashMap<String, List<AllocationItem>> r, String id){
        return r.entrySet().stream().filter((entry)->{
            return entry.getValue().stream().filter((allocated)->{
                return allocated.getItemId().equals(id);
            }).findAny().get() != null;
        }).findAny().get().getKey();
    }

    private Collection<CompletableFuture<Delete.Deleted>> deleteItems(Collection<TransferItemModel> items){
        Collection<CompletableFuture<Delete.Deleted>> futureDeletion = new ArrayList<>();

        items.stream().forEach((ti)->{
            String driveName = ti.ai.getDrive();
            Log.d(TAG, "delete item. drive: " + driveName +" name: " + ti.ai.getName());
            Delete deleter = new Delete(mCDFS.getDrives().get(driveName).getClient());
            File file = new File();
            com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
            metaData.setId(ti.ai.getItemId());
            file.setFile(metaData);
            futureDeletion.add(deleter.runSync(file));
        });

        return futureDeletion;
    }

    /*
        The properties in the input items will be updated according to the transferred items:
        1. drive (drive name)
        2. Item ID (item id in user drive)

        Input:
        items:          items will get updated
        transferred:    items transferred

     */
    HashMap<String, Collection<AllocationItem>> updateProperty(Collection<TransferItemModel> transferred){

//        Map<String, AllocationItem> remaped = transferred.stream().map((ti)->{
//            Map.Entry<String, AllocationItem> e = new Map.Entry<String, AllocationItem>() {
//                @Override
//                public String getKey() {
//                    return ti.destDriveName;
//                }
//
//                @Override
//                public AllocationItem getValue() {
//                    ti.ai.setDrive(ti.destDriveName);
//                    ti.ai.setItemId(ti.newId);
//                    return ti.ai;
//                }
//
//                @Override
//                public AllocationItem setValue(AllocationItem allocationItem) {
//                    return null;
//                }
//            };
//            return e;
//        }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));
//
//        return com.crossdrives.cdfs.util.map.Mapper.toColletion(remaped);

        return toHashMap(transferred.stream().map((ti)->{
            ti.ai.setDrive(ti.destDriveName);
            ti.ai.setItemId(ti.newId);
            return ti.ai;
        }).collect(Collectors.toCollection(ArrayList::new)));
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

    public int getProgressMax(){return progressTotal;}

    public int getProgressCurrent(){return progressActual;}

    void callback(com.crossdrives.cdfs.move.Move.State state){
        mState = state;
        mListener.progressChanged(this);
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
}
