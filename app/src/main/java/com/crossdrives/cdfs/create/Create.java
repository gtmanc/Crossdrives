package com.crossdrives.cdfs.create;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.delete.Delete;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Create {
    static final String TAG = "CD.Create";
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    CDFS mCDFS;
    final String mFileID;
    String mParent;

    public Create(CDFS mCDFS, String mFileID, String mParent) {
        this.mCDFS = mCDFS;
        this.mFileID = mFileID;
        this.mParent = mParent;
    }

    public Task<File> execute(){
        Task<com.crossdrives.driveclient.model.File> task;

        task = Tasks.call(mExecutor, new Callable<File>() {
            @Override
            public File call() throws Exception {
                Log.d(TAG, "Fetch map...");
                //callback(Delete.State.GET_MAP_STARTED);
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();
                Log.d(TAG, "map fetched");
                //callback(Delete.State.GET_MAP_COMPLETE);
                return null;
            }
        });

        return task;
    }
}
