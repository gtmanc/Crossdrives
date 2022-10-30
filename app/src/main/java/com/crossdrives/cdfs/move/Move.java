package com.crossdrives.cdfs.move;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.delete.Delete;
import com.crossdrives.cdfs.delete.IDeleteProgressListener;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Move {
    final String TAG = "CD.Move";
    CDFS mCDFS;
    final String mFileID;
    String mParent;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    public Move(CDFS cdfs, String id, String parent) {
        this.mCDFS = cdfs;
        this.mFileID = id;
        this.mParent = parent;
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
                //Collection<Throwable> exceptions = new ArrayList<>();
                HashMap<String, CompletableFuture<String>> futures = new HashMap<>();
                Log.d(TAG, "Fetch map...");
                //callback(Delete.State.GET_MAP_STARTED);
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();
                Log.d(TAG, "map fetched");
                //callback(Delete.State.GET_MAP_COMPLETE);

                //map to allocation container so that we can easily process later
                HashMap<String, AllocContainer> mapped = Mapper.reValue(maps, (stream)->{
                    return AllocManager.toContainer(stream);
                });

                //extract the allocation items we are interested
                HashMap<String, AllocContainer> filteredContainers = Mapper.reValue(mapped, (container)->{
                    List<AllocationItem> list =
                            container.getAllocItem().stream().filter((item)->{
                                return item.getCdfsId().equals(mFileID);
                            }).collect(Collectors.toList());

                    AllocContainer newContainer = AllocManager.newAllocContainer();
                    newContainer.addItems(list);
                    return newContainer;
                });

                //replace the parent with the specified
                HashMap<String, AllocContainer> containers = Mapper.reValue(filteredContainers, (container)->{
                    AllocContainer ac = new AllocContainer();
                    ac.setVersion(container.getVersion());
                    List<AllocationItem> itemList= container.getAllocItem();
                    List<AllocationItem> items = itemList.stream().map((item)->{
                        AllocationItem ai = AllocationItem.clone(item);
                        ai.setPath(mParent);
                        return ai;
                    }).collect(Collectors.toList());
                    return container;
                });

                MapUpdater updater = new MapUpdater(mCDFS.getDrives());

                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(containers, mParent);
                updateFuture.join();

                return result;
                }
        });
        return task;
    }
}
