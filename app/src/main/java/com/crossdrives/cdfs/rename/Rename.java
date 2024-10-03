package com.crossdrives.cdfs.rename;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.print.Printer;
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

public class Rename {
    final String TAG = "CD.Renme";
    CDFS mCDFS;
    String newName;
    CdfsItem mItem;
    List<CdfsItem> mParents;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    Printer dp = new Printer(TAG);

    public Rename(CDFS mCDFS, String name, CdfsItem item, @NonNull List<CdfsItem> parents) {
        this.mCDFS = mCDFS;
        newName = name;
        mParents = parents;
        mItem = item;
    }

    public Task<File> execute(){
        Task<com.crossdrives.driveclient.model.File> task;
        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public File call() throws Exception {
                CdfsItem whereWeAre = mParents.isEmpty()? null : mParents.get(mParents.size()-1);

                Log.d(TAG, "Fetch map...");
                //callback(Delete.State.GET_MAP_STARTED);
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(whereWeAre);
                HashMap<String, OutputStream> mapFile = mapsFuture.join();
                Log.d(TAG, "map fetched");

                //Just map to allocation container so that we can easily process later
                HashMap<String, AllocContainer> mapContainer = Mapper.reValue(mapFile, (stream)->{
                    return AllocManager.toContainer(stream);
                });

                String cdfsId = mItem.getId();
                mapContainer.forEach((k, v)->{
                    List<AllocationItem> items = v.getAllocItem();
                    items.forEach((ai)->
                    {
                        if(ai.getCdfsId().equals(cdfsId)){ai.setName(newName);}
                    });
                });

                dp.getContainer().out("New containers:", mapContainer, "");

                Log.d(TAG, "Update map file in parent...");
                MapUpdater updater = new MapUpdater(mCDFS.getDrives());
                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(mapContainer, whereWeAre);
                updateFuture.join();
                Log.d(TAG, "Parent map file updated.");


                return null;
            }
        });
        return task;
    }
}
