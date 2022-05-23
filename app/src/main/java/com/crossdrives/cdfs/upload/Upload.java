package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.IConstant;
import com.crossdrives.cdfs.Infrastructure;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.ISplitProgressCallback;
import com.crossdrives.cdfs.allocation.Splitter;
import com.crossdrives.cdfs.remote.DriveQuota;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Upload {
    final String TAG = "CD.Upload";
    CDFS mCDFS;
    IUploadCallbck mCallback = null;
    InputStream inputStream;
    private final String NAME_CDFS_FOLDER = IConstant.NAME_CDFS_FOLDER;
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    public Upload(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public Upload(CDFS cdfs, InputStream ins) {
        mCDFS = cdfs;
        inputStream = ins;
    }

    public void upload(File file, IUploadCallbck callback){
        mCallback = callback;
        ConcurrentHashMap<String, Drive> drives= mCDFS.getDrives();

        DriveQuota dq = new DriveQuota(drives);
        dq.fetchAllOf().addOnSuccessListener(new OnSuccessListener<HashMap<String, About.StorageQuota>>() {
            @Override
            public void onSuccess(HashMap<String, About.StorageQuota> quotaMap) {
                quotaMap.forEach((k, quota)->{
                    CompletableFuture<String> checkFolderFuture = new CompletableFuture<>();
                    sExecutor.submit(()->{
                        mCDFS.getDrives().get(k).getClient().list().buildRequest()
                                .setNextPage(null)
                                .setPageSize(0) //0 means no page size is applied
                                .filter(FILTERCLAUSE_CDFS_FOLDER)
                                //.filter("mimeType = 'application/vnd.google-apps.folder'")
                                //.filter(null)   //null means no filter will be applied
                                .run(new IFileListCallBack<FileList, Object>() {
                                    //As we specified the folder name, suppose only cdfs folder in the list.
                                    @Override
                                    public void success(FileList fileList, Object o) {
                                        String cdfsFolderId =
                                        fileList.getFiles().stream().
                                                filter((f)-> f.getName().equals(NAME_CDFS_FOLDER)).findAny().get().getName();
                                        checkFolderFuture.complete(cdfsFolderId);
                                    }

                                    @Override
                                    public void failure(String ex) {
                                        checkFolderFuture.completeExceptionally(new Throwable(ex));
                                    }
                                });
                    });

                    CompletableFuture<List<String>> CommitAllocationMapFuture = checkFolderFuture.thenCompose((cdfsFolderId)->{
                        CompletableFuture<String> future = new CompletableFuture<>();
                        HashMap<String, Long> allocation;
                        Collection<File> toUpload = new ArrayList<>();
                        Collection<File> toFreeUp = new ArrayList<>();
                        final Lock uploadLock = new ReentrantLock();
                        final Lock freeupLock = new ReentrantLock();

                        long size = 0;
                        final boolean[] isAllSplittd = {false};
                        final boolean[] terminateExceptionally = {false};

                        if(cdfsFolderId == null){
                            Log.w(TAG, "CDFS folder is missing!");
                            future.completeExceptionally(new Throwable("CDFS folder is missing"));
                        }

                        try {
                            size = (long)inputStream.available();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Size to upload: " + size);
                        //Allocator allocator = new Allocator(quotaMap, file.length());
                        Allocator allocator = new Allocator(quotaMap, size);
                        Log.d(TAG, "Drive Name: " + k + "Limit: " + quota.getLimit() + " usage: " + quota.getUsage());

                        allocation = allocator.getAllocationResult();
                        Splitter splitter = new Splitter(inputStream, allocation);
                        splitter.split(new ISplitProgressCallback() {
                            @Override
                            public void start(String name, long total) {
                                Log.d(TAG, "Split start. drive name: " + name +
                                        " allocated length: " + total);
                            }

                            @Override
                            public void progress(File slice) {
                                //upload slice to remote
                                Log.d(TAG, "Split progress: " + slice.getPath());
                                uploadLock.lock();
                                toUpload.add(slice);
                                uploadLock.unlock();
                            }

                            @Override
                            public void finish(String name, long remaining) {
                                Log.d(TAG, "split finished. drive name: " + name +
                                        " Remaining data: " + remaining);
                                isAllSplittd[0] = true;
                            }

                            @Override
                            public void onFailure(String ex) {
                                Log.d(TAG, "Split file failed! " +ex);
                                terminateExceptionally[0] = true;
                                mCallback.onFailure(new Throwable(ex));
                            }

                            @Override
                            public Collection<File> RequestDeletion() {
                                Collection<File> collection = new ArrayList<>();

                                freeupLock.lock();
                                collection.addAll(toFreeUp);
                                toFreeUp.removeAll(Arrays.asList(collection.toArray()));
                                freeupLock.unlock();
                                return collection;
                            }
                        });

                        Log.d(TAG, "Upload slice of file once available... ");
                        while(isAllSplittd[0] == false && toUpload.isEmpty()){
                            if(terminateExceptionally[0]){
                                break;
                            }

                            Log.d(TAG, "Get item in upload list...");
                            uploadLock.lock();
                            File localFile = toUpload.stream().findAny().get();
                            uploadLock.unlock();
                            Log.d(TAG, "Done!");
                            if(localFile != null){
                                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                                fileMetadata.setParents(Collections.singletonList(cdfsFolderId));

                                Log.d(TAG, "local file to upload: " + localFile.getName());
                                fileMetadata.setName(localFile.getName());
                                mCDFS.getDrives().get(k).getClient().upload().
                                        buildRequest(fileMetadata, localFile).run(new IUploadCallBack() {
                                            @Override
                                            public void success(com.crossdrives.driveclient.model.File file) {
                                                Log.d(TAG, "slice uploaded. Name: " + file.getFile().getName()
                                                        + " local name: " + file.getOriginalLocalFile());
                                                uploadLock.lock();
                                                toUpload.remove(file.getOriginalLocalFile());
                                                uploadLock.unlock();
                                                freeupLock.lock();
                                                toFreeUp.add(file.getOriginalLocalFile());
                                                freeupLock.unlock();
                                            }

                                            @Override
                                            public void failure(String ex) {
                                                Log.d(TAG, ex);
                                            }
                                        });
                            }else{
                                Log.d(TAG, "no item available in upload list!");
                            }
                        }

                        Log.d(TAG, "upload completed!");

                        return null;
                    });

                    CommitAllocationMapFuture.thenAccept((ids)->{

                    });
                    /*
                    exception handling
                    */
                    checkFolderFuture.exceptionally(ex ->{
                        Log.w(TAG, "Completed with exception in folder check: " + ex.toString());
                        return null;
                    }).handle((s, t) ->{
                        Log.w(TAG, "Exception occurred in folder check: " + t.toString());
                        return null;
                    });
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mCallback.onFailure(new Throwable(e.getMessage()));
            }
        });

//        drives.forEach((driveName, drive)->{
//            Log.d(TAG, "" + driveName);
//            drive.getClient().about().buildRequest().run(new IAboutCallBack() {
//                @Override
//                public void success(About about) {
//
//                    About.StorageQuota quota = about.getStorageQuota();
//                    Log.d(TAG, "Limit: " + quota.getLimit() + " usage: " + quota.getUsage());
//                }
//
//                @Override
//                public void failure(String ex) {
//                    Log.w(TAG, ex);
//                }
//            });
//        });
    }



}
