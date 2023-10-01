package com.crossdrives.cdfs.move;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.MetaDataUpdater;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.model.UpdateFile;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Move {
    final String TAG = "CD.Move";
    final CDFS mCDFS;
    final CdfsItem mFileID;
    final CdfsItem mSource, mDest;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    DebugPrintOut po = new DebugPrintOut();

    public Move(CDFS cdfs, CdfsItem item, CdfsItem source, CdfsItem dest) {
        this.mCDFS = cdfs;
        this.mFileID = item;
        this.mSource = source;
        this.mDest = dest;
    }

    public Task<File> execute() {
        Task<com.crossdrives.driveclient.model.File> task;

        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public com.crossdrives.driveclient.model.File call() throws Exception {
                //get the maps for both source and destination because we will need to update the maps according to
                //the change we will made
                HashMap<String, AllocContainer> containerSrc = getMapContainers(mCDFS.getDrives(), mSource);
                HashMap<String, AllocContainer> containerDest = getMapContainers(mCDFS.getDrives(), mDest);
                //po.out("Container source:", containerSrc);
                //po.out("Container destination:", containerDest);

                //Get the items we are interest according to the CDFS item that to be moved.
                //Then update the field parentPath of the items with the dest parent path
                HashMap<String, Collection<AllocationItem>> updatedItemLists = updateParentPathAll(getAllocItemsAllById(containerSrc, mFileID.getId()), mDest.getPath());
                //print out for debug
                updatedItemLists.entrySet().stream().forEach((set)->{
                    Log.d(TAG, "Items that parentPath updated. drive: " + set.getKey());
                    set.getValue().forEach((item)-> Log.d(TAG, "name: " + item.getName() + " parent: " + item.getPath()));
                });

                //build up the new containers for source and destination that we will use to update the
                //allocation maps in both source and dest parent.
                ContainerUtil containerUtil = new ContainerUtil();
                HashMap<String, AllocContainer> newContainerSrc = containerUtil.removeItems(containerSrc, updatedItemLists);
                HashMap<String, AllocContainer> newContainerDest = containerUtil.addItems(containerDest, updatedItemLists);

                po.out("New container to source:", newContainerSrc);
                po.out("New container to dest", newContainerDest);
                //following are critical process which must be atomic. However, we can't guarantee this.
                //A recovery process will be employed to solve the issue.
                MetaDataUpdater metaDataUpdater = new MetaDataUpdater(mCDFS.getDrives());
                metaDataUpdater.parent(mSource.getMap(), mDest.getMap()).run(mFileID.getMap()).join();

                MapUpdater mapUpdater1 = new MapUpdater(mCDFS.getDrives());
                MapUpdater mapUpdater2 = new MapUpdater(mCDFS.getDrives());
//                Collection<CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>>> futures =
//                new ArrayList<>();
//                futures.add(mapUpdater1.updateAll(newContainerSrc, mSource));
//                futures.add(mapUpdater2.updateAll(newContainerDest, mDest));
//                futures.stream().forEach((future)->{
//                    future.join();
//                });

                mapUpdater1.updateAll(newContainerSrc, mSource).join();
                mapUpdater2.updateAll(newContainerDest, mDest).join();
                return null;
                }
        });
        return task;
    }

    private HashMap<String, AllocContainer> getMapContainers(HashMap<String, Drive> drives, CdfsItem parent){
        HashMap<String, CompletableFuture<String>> futures = new HashMap<>();
        HashMap<String, OutputStream> maps;

        //callback(Delete.State.GET_MAP_STARTED);
        MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
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

    class ContainerUtil{
        public HashMap<String, AllocContainer> addItems(HashMap<String, AllocContainer> containers,
                                                         HashMap<String, Collection<AllocationItem>> items) {
            return Mapper.reValue(containers, (key,container) -> {
                Log.d(TAG, "Remove items: " + items.get(key));
                container.addItems(items.get(key));
                Log.d(TAG, container.toString());

                po.out("New container to dest: ", container);
                Log.d(TAG, "version: " + container.getVersion());
                return container;
            });
        }

        public HashMap<String, AllocContainer> removeItems(HashMap<String, AllocContainer> containers,
                                                            HashMap<String, Collection<AllocationItem>> items) {
            return Mapper.reValue(containers, (key,container) -> {
                Log.d(TAG, "Remove items: " + items.get(key));
                container.removeItems(items.get(key));
                po.out("New container to source: ", container);
                Log.d(TAG, "version: " + container.getVersion());
                return container;
            });
        }
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
