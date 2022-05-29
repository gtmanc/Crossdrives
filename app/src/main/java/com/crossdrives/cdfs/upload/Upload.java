package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.IConstant;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.ISplitAllCallback;
import com.crossdrives.cdfs.allocation.ISplitCallback;
import com.crossdrives.cdfs.allocation.Splitter;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.remote.DriveQuota;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Upload {
    final String TAG = "CD.Upload";
    CDFS mCDFS;
    IUploadCallbck mCallback;
    InputStream inputStream;
    private final String NAME_CDFS_FOLDER = IConstant.NAME_CDFS_FOLDER;
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    String name;
    final int MAX_CHUNK = 10;
    com.google.api.services.drive.model.File mParent;
    HashMap<String, CompletableFuture<Collection<String>>> Futures= new HashMap<>();

    public Upload(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public Upload(CDFS cdfs, InputStream ins, String name, com.google.api.services.drive.model.File parent) {
        mCDFS = cdfs;
        inputStream = ins;
        this.name = name;
        mParent = parent;
    }

    public void upload(File file, IUploadCallbck callback){
        mCallback = callback;
        ConcurrentHashMap<String, Drive> drives= mCDFS.getDrives();

        DriveQuota dq = new DriveQuota(drives);
        dq.fetchAllOf().addOnSuccessListener(sExecutor, new OnSuccessListener<HashMap<String, About.StorageQuota>>() {
            @Override
            public void onSuccess(HashMap<String, About.StorageQuota> quotaMap){
                HashMap<String, Long> allocation;
                final int[] totalSlice = {0};
                final long[] size = {0};
                try {
                    size[0] = (long)inputStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Size to upload: " + size[0]);
                //Allocator allocator = new Allocator(quotaMap, file.length());
                Allocator allocator = new Allocator(quotaMap, size[0]);
                allocation = allocator.getAllocationResult();
                allocation.forEach((driveName, allocatedLen)->{
                    HashMap<String, AllocationItem> items = new HashMap<>();
                    CompletableFuture<String> checkFolderFuture = new CompletableFuture<>();
                    sExecutor.submit(()->{
                        mCDFS.getDrives().get(driveName).getClient().list().buildRequest()
                                .setNextPage(null)
                                .setPageSize(0) //0 means no page size is applied
                                .filter(FILTERCLAUSE_CDFS_FOLDER)
                                //.filter("mimeType = 'application/vnd.google-apps.folder'")
                                //.filter(null)   //null means no filter will be applied
                                .run(new IFileListCallBack<FileList, Object>() {
                                    //As we specified the folder name, suppose only cdfs folder in the list.
                                    @Override
                                    public void success(FileList fileList, Object o) {
                                        String cdfsFolderId = null;
                                        Optional<com.google.api.services.drive.model.File> optional =
                                        fileList.getFiles().stream().
                                                filter((f)-> f.getName().equals(NAME_CDFS_FOLDER)).findAny();
                                        if(optional != null){
                                            cdfsFolderId = optional.get().getId();
                                        }
                                        checkFolderFuture.complete(cdfsFolderId);
                                    }

                                    @Override
                                    public void failure(String ex) {
                                        checkFolderFuture.completeExceptionally(new Throwable(ex));
                                    }
                                });
                    });

                    CompletableFuture<Collection<String>> CommitAllocationMapFuture = checkFolderFuture.thenCompose((cdfsFolderId)->{
                        CompletableFuture<Collection<String>> future = new CompletableFuture<>();

                        ArrayBlockingQueue<File> toUploadQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                        ArrayBlockingQueue<File> toFreeupQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                        LinkedBlockingQueue<File> remainingQueue = new LinkedBlockingQueue<>();
                        Collection<String> ids = new ArrayList<>();
                        final boolean[] isAllSplittd = {false};
                        final boolean[] SplitterminateExceptionally = {false};

                        if(cdfsFolderId == null){
                            Log.w(TAG, "CDFS folder is missing!");
                            future.completeExceptionally(new Throwable("CDFS folder is missing"));
                        }

                        Splitter splitter = new Splitter(inputStream, allocatedLen, name, MAX_CHUNK);
                        splitter.split(new ISplitCallback() {
                            @Override
                            public void start(long total) {
                                Log.d(TAG, "Split start. drive name: " + driveName +
                                        " allocated length: " + total);
                            }

                            @Override
                            public void progress(File slice, long len) {
                                AllocationItem item = new AllocationItem();
                                Log.d(TAG, "Split progress: Path: " + slice.getPath() + "Name: "
                                + slice.getName());
                                toUploadQueue.add(slice);
                                totalSlice[0]++;
                                item.setSize(len);
                                item.setName(slice.getName());
                                item.setDrive(driveName);
                                item.setSequence(totalSlice[0]);
                                item.setPath(mParent.getName());
                                item.setAttrFolder(false);
                                item.setCDFSItemSize(size[0]);
                                items.put(slice.getName(), item);
                            }

                            @Override
                            public void finish(long remaining) {
                                Log.d(TAG, "split finished. drive name: " + driveName +
                                        " Remaining data: " + remaining);
                                isAllSplittd[0] = true;
                            }

                            @Override
                            public void onFailure(String ex) {
                                Log.d(TAG, "Split file failed! " +ex);
                                SplitterminateExceptionally[0] = true;
                                mCallback.onFailure(new Throwable(ex));
                            }
                        });

                        while(!isAllSplittd[0]){

                            if(SplitterminateExceptionally[0]){
                                break;
                            }

                            File localFile = takeFromQueue(toUploadQueue);
//                            if(toUploadQueue.isEmpty()){Log.d(TAG, "To upload queue is empty!");}

//                            if(localFileOptional.isPresent()){
//                                File localFile = localFileOptional.get();
                                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                                fileMetadata.setParents(Collections.singletonList(cdfsFolderId));
                                fileMetadata.setName(localFile.getName());
                                //Log.d(TAG, "local file to upload: " + localFile.getName());
                                mCDFS.getDrives().get(driveName).getClient().upload().
                                        buildRequest(fileMetadata, localFile).run(new IUploadCallBack() {
                                            @Override
                                            public void success(com.crossdrives.driveclient.model.File file) {
                                                AllocationItem item;
                                                Log.d(TAG, "slice uploaded. local name: " + file.getOriginalLocalFile().getName());
                                                item = items.get(file.getOriginalLocalFile().getName());
                                                if(item == null) {Log.w(TAG,"item mot found!");}
                                                item.setItemId(file.getFile().getId());

                                                toFreeupQueue.add(file.getOriginalLocalFile());
                                            }

                                            @Override
                                            public void failure(String ex, File originalFile) {
                                                Log.w(TAG, ex);
                                                remainingQueue.offer(originalFile);
                                            }
                                        });
                            //inUploadQueue.add(localFile);
                            //moveBetweenQueues(toUploadQueue,inUploadQueue);
                            //                           }
//                            else{
//                                Log.d(TAG, "no item available in upload list!");
//                            }
                        }

                        Log.d(TAG, "upload are scheduled but not yet completed!");

                        while(!isUploadCompleted(totalSlice[0], toFreeupQueue, remainingQueue));

                        Collection<File> collection = new ArrayList<>();
                        toFreeupQueue.drainTo(collection);
                        splitter.cleanup(collection);

                        future.complete(ids);

                        return future;
                    });

                    //Put the future to the map for joined result later
                    Futures.put(driveName, CommitAllocationMapFuture);
                    
                    CommitAllocationMapFuture.thenAccept((ids)->{
                        CompletableFuture<Collection<String>> future = new CompletableFuture<>();

                        items.forEach((k, v)->{
                            Log.d(TAG,
                                        "Drive: " + v.getDrive() +
                                        ". Name: " + v.getName() +
                                        ". Seq: " + v.getSequence() +
                                        ". Item id: " + v.getItemId() +
                                        ". Parent: " + v.getPath() +
                                        ". Size: " + v.getSize() +
                                        ". CDFS Size: " + v.getCDFSItemSize() +
                                        ". Total segs: " + v.getTotalSeg()
                            );
                        });
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

                    CommitAllocationMapFuture.exceptionally(ex ->{
                        Log.w(TAG, "Completed with exception in CommitAllocationMapFuture: " + ex.toString());
                        return null;
                    }).handle((s, t) ->{
                        Log.w(TAG, "Exception occurred in CommitAllocationMapFuture: " + t.toString());
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

    boolean isUploadCompleted(int total, ArrayBlockingQueue<File> Q1, LinkedBlockingQueue<File> Q2){
        boolean result = false;
        int number1 = Q1.size()-Q1.remainingCapacity();
        int number2 = Q2.size()-Q2.remainingCapacity();
        Log.d(TAG, "expected: " + total + " size1: " + Q1.size() + " size2: " + Q2.size());
        if((Q1.size() + Q2.size()) >= total){
            Log.d(TAG, "Upload completed!");
            result = true;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    File takeFromQueue(ArrayBlockingQueue<File> q){
        File f = null;
        try {
            f = q.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return f;
    }

    void moveBetweenQueues(ArrayBlockingQueue<File>src, ArrayBlockingQueue<File> dest, File item){
        File f;

        try {
            f = src.take();
            dest.add(f);
            src.remove(f);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
