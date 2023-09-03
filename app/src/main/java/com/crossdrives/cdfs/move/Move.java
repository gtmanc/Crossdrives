package com.crossdrives.cdfs.move;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Move {
    final String TAG = "CD.Move";
    final CDFS mCDFS;
    final String mFileID;
    final CdfsItem mSource, mDest;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    final String CONTAINER_UPDATE_ADD = "container_update_add";
    final String CONTAINER_UPDATE_REMOVE = "container_update_remove";

    public Move(CDFS cdfs, String itemCdfsId, CdfsItem source, CdfsItem dest) {
        this.mCDFS = cdfs;
        this.mFileID = itemCdfsId;
        this.mSource = source;
        this.mDest = dest;
    }

    public Task<File> execute() {
        Task<com.crossdrives.driveclient.model.File> task;

        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public com.crossdrives.driveclient.model.File call() throws Exception {
                com.crossdrives.driveclient.model.File result = new com.crossdrives.driveclient.model.File();
                com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
                file.setId(mFileID);
                result.setFile(file);

                //get the maps for both source and destination because we will need to update the maps according to
                //the change we will made
                HashMap<String, AllocContainer> containerSrc = getMapContainers(mCDFS.getDrives(), mSource);
                HashMap<String, AllocContainer> containerDest = getMapContainers(mCDFS.getDrives(), mDest);

                //extract the allocation items we are interested
//                HashMap<String, AllocContainer> filteredContainers = Mapper.reValue(mapped, (container)->{
//                    List<AllocationItem> list =
//                            container.getAllocItem().stream().filter((item)->{
//                                return item.getCdfsId().equals(mFileID);
//                            }).collect(Collectors.toList());
//
//                    AllocContainer newContainer = AllocManager.newAllocContainer();
//                    newContainer.addItems(list);
//                    return newContainer;
//                });

                //replace the parent with the specified
                HashMap<String, AllocContainer> containers = Mapper.reValue(filteredContainers, (container)->{
                    AllocContainer ac = new AllocContainer();
                    ac.setVersion(container.getVersion());
                    List<AllocationItem> itemList= container.getAllocItem();
                    List<AllocationItem> items = itemList.stream().map((item)->{
                        AllocationItem ai = AllocationItem.clone(item);
                        ai.setPath(mDest.getPath());
                        return ai;
                    }).collect(Collectors.toList());
                    return container;
                });

                MapUpdater updater = new MapUpdater(mCDFS.getDrives());

                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(containers, mDest);
                updateFuture.join();

                return result;
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

    private List<AllocationItem> getAllocItems(AllocContainer container, String id){
        return container.getAllocItem().stream().filter((item)->{
                                return item.getCdfsId().equals(id);
                            }).collect(Collectors.toList());
    }

    private List<AllocationItem> updateParentPath(List<AllocationItem> items, final String parentPath){
        return items.stream().map(item->{
            return item.setPath(parentPath);
        })
    }

    private HashMap<String, AllocContainer> updateContainers(HashMap<String, AllocContainer> containers,
                                                             final String CdfsID, final String action) {
        return Mapper.reValue(containers, container -> {
            container.
        });
    }
}
