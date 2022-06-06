package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.update.IUpdateCallBack;
import com.crossdrives.driveclient.update.IUpdateRequest;
import com.crossdrives.driveclient.update.IUpdateRequestBuilder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.ContentRestriction;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Locker {
    final String TAG = "CD.Locker";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    ICallBackLocker<HashMap<String, String>> mCallback;

    HashMap<String, CompletableFuture<File>> Futures= new HashMap<>();

    public Locker(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    Task<HashMap<String, File>> lockAll(HashMap<String, String> fileIDs, ICallBackLocker<HashMap<String, String>> callback){
        mCallback = callback;
        Task<HashMap<String, File>> task;
       /*
            Start fetching one by one
        * */
        mDrives.forEach((name, drive)->{
            Log.d(TAG, "Lock files for drives: " + name);
            CompletableFuture<File> future = new CompletableFuture<>();


            sExecutor.submit(()->{
                lock(drive, fileIDs.get(name), new ICallBackLocker<File>() {
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

        task = Tasks.call(sExecutor, new Callable<HashMap<String, File>>() {
            @Override
            public HashMap<String, File> call() throws Exception {
                Map<String, File> locked = Futures.entrySet().stream().map((entry)-> {
                    Map.Entry<String, File> entry1 = new Map.Entry<String, File>() {
                        @Override
                        public String getKey() {
                            return entry.getKey();
                        }

                        @Override
                        public File getValue() {
                            /*
                                join will block current thread
                             */
                            return entry.getValue().join();
                        }

                        @Override
                        public File setValue(File value) {
                            return null;
                        }
                    };
                    return entry1;
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));


                return new HashMap<String, File>(locked);
            }
        });

        return task;

    }

    void lock(Drive drive, String ID, ICallBackLocker<File> callback) {
        IUpdateRequest request;
        request = drive.getClient().update().buildRequest(ID, IUpdateRequestBuilder.OP_LOCK);
        request.Reason("");
        request.run(new IUpdateCallBack<File>() {
            @Override
            public void success(File file) {
                ContentRestriction restriction = file.getContentRestrictions().get(0);
                Log.d(TAG, "Locked. File name: " + file.getName() +
                        ". Reason: " + restriction.getReason() +
                ". Read only: " + restriction.getReadOnly());
                callback.onCompleted(file);
            }

            @Override
            public void failure(String ex) {
                callback.onCompletedExceptionally(new Throwable(ex));
            }
        });
    }
}
