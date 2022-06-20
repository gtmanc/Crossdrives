package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.get.IGetCallBack;
import com.crossdrives.driveclient.update.IUpdateCallBack;
import com.crossdrives.driveclient.update.IUpdateRequest;
import com.google.api.services.drive.model.ContentRestriction;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Locker {
    final String TAG = "CD.Locker";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    HashMap<String, CompletableFuture<File>> Futures= new HashMap<>();
    String mReason;

    final String OP_LOCK = "lock";
    final String OP_UNLOCK = "unlock";

    public Locker(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, File>> lockAll(HashMap<String, String> fileIDs){

        return changeLockAll(fileIDs, OP_LOCK);
    }

    public CompletableFuture<HashMap<String, File>> unlockAll(HashMap<String, String> fileIDs){

        return changeLockAll(fileIDs, OP_UNLOCK);
    }

    public CompletableFuture<HashMap<String, ContentRestriction>> getStatusAll(HashMap<String, String> fileIDs){
        HashMap<String, CompletableFuture<File>> Futures= new HashMap<>();
        mDrives.forEach((name, drive)->{
            CompletableFuture<File> future = new CompletableFuture<>();
            drive.getClient().get().buildRequest(fileIDs.get(name)).run(new IGetCallBack<File>() {
                @Override
                public void success(File file) {
                    future.complete(file);
                }

                @Override
                public void failure(String ex) {
                    future.completeExceptionally(new Throwable(ex));
                }
            });
            Futures.put(name, future);
        });
        CompletableFuture<HashMap<String, ContentRestriction>> resultFuture = CompletableFuture.supplyAsync(()->{
            Map<String, ContentRestriction> get = Futures.entrySet().stream().map((set)->{
                Map.Entry<String, ContentRestriction> entry = new Map.Entry<String, ContentRestriction>() {
                    @Override
                    public String getKey() {
                        return set.getKey();
                    }

                    @Override
                    public ContentRestriction getValue() {
                        return set.getValue().join().getContentRestrictions().get(0);
                    }

                    @Override
                    public ContentRestriction setValue(ContentRestriction value) {
                        return null;
                    }
                };
                return entry;
            }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
            return new HashMap<>(get);
        });

        return resultFuture;
    }

    Locker Reason(String reason){
        mReason = reason;
        return this;
    }

    CompletableFuture<HashMap<String, File>> changeLockAll(HashMap<String, String> fileIDs, String operation){
        mDrives.forEach((name, drive)->{
            Log.d(TAG, "Lock files for drives: " + name);
            CompletableFuture<File> future = new CompletableFuture<>();


            //sExecutor.submit(()->{
            changeLock(drive, fileIDs.get(name), operation, new ICallBackLocker<File>() {
                @Override
                public void onCompleted(File file) {
                    future.complete(file);
                }

                @Override
                public void onCompletedExceptionally(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
            //});
            Futures.put(name, future);
        });
        CompletableFuture<HashMap<String, File>> resultFuture = CompletableFuture.supplyAsync(()->{
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
            return new HashMap<>(locked);
        });

        return resultFuture;
    }

    void changeLock(final Drive drive, final String fileID, final String operation, ICallBackLocker<File> callback) {
        IUpdateRequest request;
        File file = new File();
        List<ContentRestriction> restrictions = new ArrayList<>();
        ContentRestriction restriction = new ContentRestriction();

        if(operation.equals(OP_LOCK)){
            restriction.setReadOnly(new Boolean(true));
        }else if(operation.equals(OP_UNLOCK)){
            restriction.setReadOnly(new Boolean(false));
        }else{
            Log.w(TAG, "Something wrong when updating a file. Missing specified operation!");
            return;
        }

        if(mReason != null){
            restriction.setReason(mReason);
        }

        restrictions.add(restriction);
        file.setContentRestrictions(restrictions);

        Log.d(TAG, "Lock file. ID: " + fileID);
        request = drive.getClient().update().buildRequest(fileID, file);
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
