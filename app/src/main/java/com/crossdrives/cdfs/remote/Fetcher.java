package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Fetcher {
    final String TAG = "CD.Locker";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    HashMap<String, CompletableFuture<File>> Futures= new HashMap<>();
    ICallBackLocker<HashMap<String, String>> mCallback;

    public Fetcher(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public Task<HashMap<String, File>> fetchAll(HashMap<String, String> parent){
        Task<HashMap<String, File>> task;
       /*
            Start fetching one by one
        */
        mDrives.forEach((name, drive)->{
            Log.d(TAG, "fetch file for drives: " + name);
            CompletableFuture<File> future = new CompletableFuture<>();

            sExecutor.submit(()->{
                fetch(drive, parent.get(name), new IFetcherCallBack<File>() {
                    @Override
                    public void onCompleted(File file) {
                        future.complete(file);
                    }

                    @Override
                    public void onCompletedExceptionally(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                });
            });
            Futures.put(name, future);
        });
    }

    public Task<HashMap<String, OutputStream>> fetchAll(String parent){}

    void fetch(Drive drive, String fileID, IFetcherCallBack<File> callback){
        drive.getClient().list().buildRequest().run(new IFileListCallBack<FileList, Object>() {
            @Override
            public void success(FileList fileList, Object o) {

            }

            @Override
            public void failure(String ex) {

            }
        });

    }

}
