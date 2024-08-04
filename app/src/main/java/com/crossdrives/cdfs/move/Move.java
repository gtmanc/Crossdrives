package com.crossdrives.cdfs.move;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.MetaDataUpdater;
import com.crossdrives.cdfs.allocation.QuotaEnquirer;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.exception.InvalidArgumentException;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.collection.Diff;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Move {
    final String TAG = "CD.Move";
    final CDFS mCDFS;
    final CdfsItem mFileID;
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

    public Move(CDFS cdfs, CdfsItem item, CdfsItem source, CdfsItem dest) {
        this.mCDFS = cdfs;
        this.mFileID = item;
        this.mSource = source;
        this.mDest = dest;
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
                HashMap<String, Collection<AllocationItem>> updatedItemLists = updateParentPathAll(getAllocItemsAllById(containerSrc, mFileID.getId()), destParent);
                //print out for debug
                updatedItemLists.entrySet().stream().forEach((set)->{
                    Log.d(TAG, "Items that parentPath updated. drive: " + set.getKey());
                    set.getValue().forEach((item)-> Log.d(TAG, "name: " + item.getName() + " parent: " + item.getPath()));
                });

                //build up the new containers for source and destination that we will use to update the
                //allocation maps in both source and dest parent.
                ContainerUtil containerUtil = new ContainerUtil();
                HashMap<String, AllocContainer> newContainerSrc = containerUtil.removeItems(containerSrc, updatedItemLists);
                //The dest containers need further modification once the IDs of new transferred items are available after the
                // transfer finishes if the drive of source and destination is not the same.
                HashMap<String, AllocContainer> newContainerDest = containerUtil.addItems(containerDest, updatedItemLists);

                po.out("New container to source:", newContainerSrc);
                po.out("New container to dest:", newContainerDest);

                Collection<String> srcDrivesDestContained = drivesContained(mSource.getMap().keySet(), mDest.getMap().keySet());
                Collection<String> srcDrivesDestNotContained = driveNotContained(mSource.getMap().keySet(), mDest.getMap().keySet());

                //======================================================================================
                //following are critical process which must be atomic. However, we can't guarantee this.
                //A recovery process will be employed to solve the issue.

                //if the drive clients of source and dest item are identical, simply,
                //get the items we are interested according to the CDFS item that to be moved.
                //Then update the field parentPath of the items with the dest parent path.
                //Otherwise, the further handling needs to be taken
                MapUpdater mapUpdater2 = null;
                MapUpdater mapUpdater3 = null;
                if(!srcDrivesDestContained.isEmpty()){
                    //
                    // The drives of source and destination are identical. Simply modify the metedata
                    //
                    HashMap<String, AllocContainer> containers = new HashMap<>();
                    containerSrc.entrySet().stream().forEach((set)->{
                        containers.put(set.getKey(), set.getValue());
                    });

                    HashMap<String, Drive> drivesInvolved = getDriveClientInvolved(srcDrivesDestContained);
                    MetaDataUpdater metaDataUpdater = new MetaDataUpdater(drivesInvolved);
                    metaDataUpdater.parent(mSource.getMap(), mDest.getMap()).run(mFileID.getMap()).join();
                    mapUpdater2 = new MapUpdater(getDriveClientInvolved(srcDrivesDestContained));
                }
                
                if(!srcDrivesDestNotContained.isEmpty()){
                    //
                    // Deal with the case that there are items in drives of the source which are missing in the destination.
                    //
                    Collection<String> drivesUsedAllocate = driveNotContained(mDest.getMap().keySet(), mSource.getMap().keySet());
                    //Filter out the items that has been processed in previous operation
                    Map<String, List<AllocationItem>> items = Mapper.reValue(containerSrc, (key, container)->{
                        return container.getAllocItem();
                    }).entrySet().stream().filter((set)->{
                        return drivesUsedAllocate.contains(set.getKey());
                    }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

                    HashMap<String, List<AllocationItem>> itemsToAllocated= new HashMap<>(items);
                    HashMap<String, List<AllocationItem>> allocResult =
                            allocate(getDriveClientInvolved(drivesUsedAllocate), itemsToAllocated);

                    // Now, we can do the transfer according to the allocation result
                    transfer(itemsToAllocated, allocResult).join();

                    mapUpdater3 = new MapUpdater(getDriveClientInvolved(srcDrivesDestNotContained));
                }

                MapUpdater mapUpdater1 = new MapUpdater(getDriveClientInvolved(mSource.getMap().keySet()));

                //MapFetcher is not thread safe. #54
//                Collection<CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>>> futures =
//                new ArrayList<>();
//                futures.add(mapUpdater1.updateAll(newContainerSrc, mSource));
//                futures.add(mapUpdater2.updateAll(newContainerDest, mDest));
//                futures.stream().forEach((future)->{
//                    future.join();
//                });

                Log.d(TAG, "Update new container to source");
                mapUpdater1.updateAll(newContainerSrc, mSource).join();
                Log.d(TAG, "Update new container to dest");
                mapUpdater2.updateAll(newContainerDest, mDest).join();
                return null;
                }
        });
        return task;
    }

    private HashMap<String, AllocContainer> getMapContainers(CdfsItem parent){
        HashMap<String, OutputStream> maps;

        HashMap<String, Drive> drivesInvolved =
        getDriveClientInvolved(parent.getMap().keySet());

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


    private HashMap<String, List<AllocationItem>>> transfer(
            HashMap<String, List<AllocationItem>> items, HashMap<String, List<AllocationItem>> allocation){

        return
            ArrayBlockingQueue

    }

    class ContainerUtil{
        public HashMap<String, AllocContainer> addItems(HashMap<String, AllocContainer> containers,
                                                         HashMap<String, Collection<AllocationItem>> items) {
            return Mapper.reValue(containers, (key,container) -> {
                container.addItems(items.get(key));

                //po.out("New container to dest: ", container);
                return container;
            });
        }

        public HashMap<String, AllocContainer> removeItems(HashMap<String, AllocContainer> containers,
                                                            HashMap<String, Collection<AllocationItem>> items) {
            return Mapper.reValue(containers, (key,container) -> {
                container.removeItems(items.get(key));
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

    private Collection<String> driveNotContained(Collection<String> src,
                                                 Collection<String> dest){

        return dest.stream().filter((key)->{
            return !src.contains(key);
        }).collect(Collectors.toList());
    }

    private Collection<String> drivesContained(Collection<String> src,
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

    private HashMap<String, Drive> getDriveClientInvolved(Collection<String> drives){
        HashMap<String, Drive> drivesInvolved = new HashMap<>();
        drives.stream().forEach((key)->{
            drivesInvolved.put(key, mCDFS.getDrives().get(key));
        });
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
        }

        void out(String head, AllocContainer container){
            Log.d(TAG, head);
            container.getAllocItem().stream().forEach((item)->{
                Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
            });
        }
    }
}
