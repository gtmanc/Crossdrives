package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.list.IFileListRequest;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.collect.ForwardingMapEntry;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public CompletableFuture<HashMap<String, FileList>> listForAll(HashMap<String, File> parent) throws ExecutionException, InterruptedException {
        CompletableFuture<HashMap<String, FileList>> resultFuture =
        CompletableFuture.supplyAsync(()->{
            mDrives.forEach((name, drive) -> {
                Log.d(TAG, "fetch list. Drive: " + name);
                CompletableFuture<FileList> future = new CompletableFuture<>();

                //sExecutor.submit(()->{
                helperFetchList(drive, parent.get(name).getId(), new IFetcherCallBack<FileList>() {
                    @Override
                    public void onCompleted(FileList files) {
                        future.complete(files);
                    }

                    @Override
                    public void onCompletedExceptionally(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                    //    });
                });
                fileListFutures.put(name, future);
            });

            //join
            Map<String, FileList> joined
            = fileListFutures.entrySet().stream().map((entrySet)->{
                Map.Entry<String, FileList> entry = new Map.Entry<String, FileList>() {
                    @Override
                    public String getKey() {
                        return entrySet.getKey();
                    }

                    @Override
                    public FileList getValue() {
                        return entrySet.getValue().join();
                    }

                    @Override
                    public FileList setValue(FileList fileList) {
                        return null;
                    }
                };
                return entry;
            }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

            return new HashMap<>(joined);
        });

        return resultFuture;
    }

    public CompletableFuture<HashMap<String, OutputStream>> pullAll(HashMap<String, String> fileID){
        CompletableFuture<HashMap<String, OutputStream>> resultFuture =
                CompletableFuture.supplyAsync(()->{
                    mDrives.forEach((name, drive) -> {
                        Log.d(TAG, "fetch list. Drive: " + name);
                        CompletableFuture<OutputStream> future = new CompletableFuture<>();

                        //sExecutor.submit(()->{
                        helpertFetchContent(drive, fileID.get(name), new IFetcherCallBack<OutputStream>() {
                            @Override
                            public void onCompleted(OutputStream content) {
                                future.complete(content);
                            }

                            @Override
                            public void onCompletedExceptionally(Throwable throwable) {
                                future.completeExceptionally(throwable);
                            }
                            //    });
                        });
                        ContentFutures.put(name, future);
                    });

                    //join
                    Map<String, OutputStream> joined=
                    ContentFutures.entrySet().stream().map((entrySet)->{
                        Map.Entry<String, OutputStream> entry = new Map.Entry<String, OutputStream>() {
                            @Override
                            public String getKey() {
                                return entrySet.getKey();
                            }

                            @Override
                            public OutputStream getValue() {
                                return entrySet.getValue().join();
                            }

                            @Override
                            public OutputStream setValue(OutputStream stream) {
                                return null;
                            }
                        };
                        return entry;
                    }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

                    return new HashMap<>(joined);
                });

        return resultFuture;
    }

    /*
        Get file list
     */
    void helperFetchList(Drive drive, String parentID, IFetcherCallBack<FileList> callback){
        IFileListRequest request;

        request = drive.getClient().list().buildRequest().
            setNextPage(null).
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
                callback.onCompleted(fileList);
            }

            @Override
            public void failure(String ex) {
                callback.onCompletedExceptionally(new Throwable(ex));
            }
        });

    }
    /*
        Get content of a file
     */
    void helpertFetchContent(Drive drive, String fileID, IFetcherCallBack<OutputStream> callback){
        drive.getClient().download().buildRequest(fileID).run(new IDownloadCallBack<OutputStream>() {
            @Override
            public void success(OutputStream outputStream) {
                callback.onCompleted(outputStream);
            }

            @Override
            public void failure(String ex) {
                callback.onCompletedExceptionally(new Throwable(ex));
            }
        });

    }

}
