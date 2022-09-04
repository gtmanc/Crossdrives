package com.crossdrives.cdfs.delete;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Delete {
    static final String TAG = "CD.Delete";
    CDFS mCDFS;
    String mFileID;
    String mParent;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    public Task<String> execute(){
        Task task;

        task = task = Tasks.call(mExecutor, new Callable<String>() {

            @Override
            public String call() throws Exception {
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();
                Log.d(TAG, "map fetched");


                return null;
            }
        };

        return task;
    }
}
