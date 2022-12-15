package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.list.IFileListRequest;
import com.crossdrives.driveclient.model.MediaData;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.collect.ForwardingMapEntry;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class Fetcher {
    final String TAG = "CD.Fetcher";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    HashMap<String, CompletableFuture<FileList>> fileListFutures = new HashMap<>();
    HashMap<String, CompletableFuture<OutputStream>> ContentFutures = new HashMap<>();

    ICallBackLocker<HashMap<String, String>> mCallback;

    public Fetcher(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }
    CompletableFuture<FileList> blockedFuture;
    public CompletableFuture<HashMap<String, FileList>> listAll(HashMap<String, File> parent) {
        CompletableFuture<HashMap<String, FileList>> resultFuture;

        mDrives.forEach((name, drive) -> {
            CompletableFuture<FileList> future
                                = helperFetchList(drive, parent.get(name).getId());
            fileListFutures.put(name, future);
        });

        resultFuture = CompletableFuture.supplyAsync(()->{
            return Mapper.reValue(fileListFutures, (future)->{
                FileList fl = future.join();
                return fl;
            });
        });

        return resultFuture;
    }

    public CompletableFuture<FileList> list(String driveName, String parent) {

        return helperFetchList(mDrives.get(driveName), parent);
    }

    public CompletableFuture<HashMap<String, OutputStream>> pullAll(HashMap<String, String> fileID){
        CompletableFuture<HashMap<String, OutputStream>> resultFuture =
                CompletableFuture.supplyAsync(()->{
                    mDrives.forEach((name, drive) -> {
                        Log.d(TAG, "fetch Content. Drive: " + name);

                        //sExecutor.submit(()->{
                        CompletableFuture<OutputStream> future = helpertFetchContent(drive, fileID.get(name));
                        ContentFutures.put(name, future);
                    });

                    return Mapper.reValue(ContentFutures, (future)->{
                        return future.join();
                    });
                });

        return resultFuture;
    }

    /*
        Get file list
     */
    CompletableFuture<FileList> helperFetchList(Drive drive, String parentID){
        IFileListRequest request;
        CompletableFuture<FileList> resultFuture = new CompletableFuture<>();

        request = drive.getClient().list().buildRequest().
            setNextPage(null).  //TODO: #42
            setPageSize(0); //0 means no page size is applied. The behavior depends on the drive vendor

        //Set filter only if parent i not an empty string. An empty string indicated that items in root is required.
        //In this case, no filter is needed.
        if(!parentID.equals("")) {
            String query = "'" + parentID + "' in parents";
            Log.d(TAG, "Set filter: " +query);
            request.filter(query);
        }

        request.run(new IFileListCallBack<FileList, Object>() {
                @Override
                public void success(FileList fileList, Object o) {
                    resultFuture.complete(fileList);
                }

                @Override
                public void failure(String ex) {
                    resultFuture.completeExceptionally(new Throwable(ex));
                }
        });

        return resultFuture;
    }
    /*
        Get content of a file
     */
    CompletableFuture<OutputStream> helpertFetchContent(Drive drive, String fileID){
        CompletableFuture<OutputStream> resultFuture = new CompletableFuture<>();
        drive.getClient().download().buildRequest(fileID).run(new IDownloadCallBack<MediaData>() {
            @Override
            public void success(MediaData mediaData) {
                Log.d(TAG, "OK!. Content is downloaded.");
                resultFuture.complete(mediaData.getOs());
            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "fetch content failed: " + ex);
                resultFuture.completeExceptionally(new Throwable(ex));
            }
        });

        return resultFuture;
    }

}
