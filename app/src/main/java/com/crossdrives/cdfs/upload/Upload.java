package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.IConstant;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.IDProducer;
import com.crossdrives.cdfs.allocation.ISplitCallback;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.Splitter;
import com.crossdrives.cdfs.data.FileLocal;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.updateContent;
import com.crossdrives.cdfs.model.updateFile;
import com.crossdrives.cdfs.remote.DriveQuota;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

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
    HashMap<String, CompletableFuture<InterMediateResult>> Futures= new HashMap<>();
    //The slice not uploaded
    LinkedBlockingQueue<File> remainingQueue = new LinkedBlockingQueue<>();
    String cdfsFolderID;

    class InterMediateResult{
        int totalSlice;
        Collection<AllocationItem> items;
    }

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
                HashMap<String, Collection<AllocationItem>> uploadedItems;
                CompletableFuture<String> updateAllocationMapFuture = new CompletableFuture<>();
                final long[] uploadSize = {0};
                try {
                    uploadSize[0] = (long)inputStream.available();
                } catch (IOException e) {
                    /*
                        We have to catch the exception here because onSuccess doens't allow us to throw exception.
                        Let's terminate te upload right here since there is no trustable upload length
                     */
                    Log.d(TAG, e.getMessage());
                    return;
                }

                Log.d(TAG, "Size to upload: " + uploadSize[0]);
                //Allocator allocator = new Allocator(quotaMap, file.length());
                Allocator allocator = new Allocator(quotaMap, uploadSize[0]);
                allocation = allocator.getAllocationResult();
                allocation.forEach((driveName, allocatedLen)->{
                    CompletableFuture<String> checkFolderFuture = new CompletableFuture<>();

                    sExecutor.submit(()->{
                        Log.w(TAG, "Get CDFS folder...");
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

                    CompletableFuture<InterMediateResult> CommitAllocationMapFuture = checkFolderFuture.thenComposeAsync((cdfsFolderId)->{
                        CompletableFuture<InterMediateResult> future = new CompletableFuture<>();
                        InterMediateResult result = new InterMediateResult();
                        HashMap<String, AllocationItem> items = new HashMap<>();    //slice name, allocation item
                        final int[] totalSlice = {0};

                        ArrayBlockingQueue<File> toUploadQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                        ArrayBlockingQueue<File> toFreeupQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                        final boolean[] isAllSplittd = {false};
                        final boolean[] SplitterminateExceptionally = {false};

                        if(cdfsFolderId == null){
                            Log.w(TAG, "CDFS folder is missing!");
                            future.completeExceptionally(new Throwable("CDFS folder is missing"));
                        }
                        cdfsFolderID = cdfsFolderId;

                        Splitter splitter = new Splitter(inputStream, allocatedLen, name, MAX_CHUNK);
                        splitter.split(new ISplitCallback() {
                            @Override
                            public void start(long total) {
                                Log.d(TAG, "Split start: Drive name: " + driveName +
                                        ". allocated length: " + total);
                            }

                            @Override
                            public void progress(File slice, long len) {
                                AllocationItem item = new AllocationItem();
                                Log.d(TAG, "Split progress: Path: " + slice.getPath() + ". Name: "
                                + slice.getName());
                                toUploadQueue.add(slice);
                                totalSlice[0]++;
                                item.setSize(len);
                                item.setName(slice.getName());
                                item.setDrive(driveName);
                                item.setSequence(totalSlice[0]);
                                item.setPath(mParent.getName());
                                item.setAttrFolder(false);
                                item.setCDFSItemSize(uploadSize[0]);
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
                            /*
                                TODO: deal blocked if splitter is going to output nothing. i.e. size is 0
                             */
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
                                                Log.d(TAG, "slice uploaded. name: " + file.getOriginalLocalFile().getName()
                                                );
                                                item = items.get(file.getOriginalLocalFile().getName());
                                                if(item == null) {Log.w(TAG,"item mot found!");}
                                                item.setItemId(file.getFile().getId());

                                                toFreeupQueue.add(file.getOriginalLocalFile());
                                            }

                                            @Override
                                            public void failure(String ex, File originalFile) {
                                                Log.w(TAG, ex);
                                                remainingQueue.offer(originalFile);

                                                //future.completeExceptionally(new Throwable(ex));
                                            }
                                        });
                        }

                        Log.d(TAG, "uploads are scheduled but not yet completed!");

                        while(!isUploadCompleted(totalSlice[0], toFreeupQueue, remainingQueue));

                        Log.d(TAG, "upload is completed!");

                        Collection<File> collection = new ArrayList<>();
                        toFreeupQueue.drainTo(collection);
                        splitter.cleanup(collection);

                        result.totalSlice = totalSlice[0];
                        result.items = items.values();
                        future.complete(result);

                        return future;
                    });

                    //Put the future to the map for joined result later
                    Futures.put(driveName, CommitAllocationMapFuture);




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

                uploadedItems=getJoinedResult(Futures);
                closeStream(inputStream);

                //Stop here if any of slice is not uploaded
                if(uploadedItems == null){
                    mCallback.onFailure(new Throwable("slice of the file may not be uploaded!"));
                }
                printItems(uploadedItems);

                /*
                    Lock remote allocation maps?
                    A signed in app still can edit a locked file although it's locked. It means that locking a file
                    doesn't actually protect the content from being modified of a file as we expcted.
                    This also makes sense becuase lock/unclok as well known check out/in is not the modern
                    concept. Instead, edit a file with merge would be the best solution. i.e. Google Doc
                 */
                MapFetcher mapFetcher = new MapFetcher(drives);
                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> mapIDFuture =
                        mapFetcher.listAll();
                CompletableFuture<HashMap<String, OutputStream>> mapStreamFuture = mapFetcher.pullAll(mapIDFuture.join());
                AllocManager am = new AllocManager(mCDFS);
                HashMap<String, AllocContainer> containers = Mapper.reValue(mapStreamFuture.join(), (in)->{
                    return am.toContainer(in);
                });

                /*
                    add the uploaded items to the container
                 */
                containers.forEach((k, v)->{
                    v.addItems(uploadedItems.get(k));
                });

                HashMap<String, updateContent> localMaps = Mapper.reValue(containers, (driveName, container)->{
                    Gson gson = new Gson();
                    FileLocal creator = new FileLocal(mCDFS);
                    updateContent content = new updateContent();
                    content.setID(mapIDFuture.join().get(driveName).getId());
                    content.setMediaContent(creator.create(driveName + "_map.txt", gson.toJson(container)));
                    return content;
                });

                MapUpdater updater = new MapUpdater(drives);
                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(localMaps);
                updateFuture.join();
//                MapLocker locker = new MapLocker(drives);
//                CompletableFuture<HashMap<String, ContentRestriction>> lockStatusFuture;
//
//
//                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> lockFuture;
//                try {
//                    lockStatusFuture = locker.getStatus(maps.get());
//
//                    lockFuture = locker.lockAll(maps.get());
//
//                    lockStatusFuture.exceptionally(ex->{
//                        Log.w(TAG, "get lock status failed: " + ex.toString());
//                        return null;
//                    });
//                    lockFuture.exceptionally((ex)->{
//                        Log.w(TAG, "lock map failed: " + ex.toString());
//                        return null;
//                    });
//                } catch (ExecutionException e) {
//                    Log.w(TAG, "ExecutionException: " + e.toString());
//                } catch (InterruptedException e) {
//                    Log.w(TAG, "InterruptedException: " + e.toString());
//                }


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

    /*

     */
    HashMap<String, Collection<AllocationItem>> getJoinedResult( HashMap<String, CompletableFuture<Upload.InterMediateResult>> futures){
        Log.d(TAG, "Awaiting joined result...");
        Map<String, InterMediateResult> joined= futures.entrySet().stream().map((v)->{
            Map.Entry<String, InterMediateResult> entry = new Map.Entry<String, InterMediateResult>() {
                @Override
                public String getKey() {
                    return v.getKey();
                }

                @Override
                public InterMediateResult getValue() {
                    return v.getValue().join();
                }

                @Override
                public InterMediateResult setValue(InterMediateResult o) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        //Terminate here if any of slice is not uploaed.
        if(remainingQueue.size() != 0){
            return null;
        }

        final int totalSeg = joined.values().stream().mapToInt(interMediateResult -> interMediateResult.totalSlice).sum();
        final Collection<String> combinedID = joined.values().stream().map((interMediateResult)->{
            Collection ids =
            interMediateResult.items.stream().map((item)->{
                //Log.d(TAG, "map to item id: " + item.getItemId());
                return item.getItemId();
            }).collect(Collectors.toCollection(ArrayList::new));
            Log.d(TAG, "id list: " + ids);
            return ids;
        }).reduce(new ArrayList<String>(),(combined, current)-> {combined.addAll(current); return combined;});

        Log.d(TAG, "combined id list: " + combinedID);

        Map<String, Collection<AllocationItem>> remapped =
        joined.entrySet().stream().map(pair-> { //drivename, intermediateResult
            //Map<String, Collection<AllocationItem>> mapped = pair.getValue().items.stream().map((item) -> {
            Collection<AllocationItem> mapped = pair.getValue().items.stream().map((item) -> {
                        item.setCdfsId(IDProducer.deriveID(combinedID));
                        item.setTotalSeg(totalSeg);
                        return item;

            }).collect(Collectors.toCollection(ArrayList::new));
            Map.Entry<String, Collection<AllocationItem>> entry = new Map.Entry<String, Collection<AllocationItem>>() {
                @Override
                public String getKey() {
                    return pair.getKey();
                }

                @Override
                public Collection<AllocationItem> getValue() {
                    return mapped;
                }

                @Override
                public Collection<AllocationItem> setValue(Collection<AllocationItem> value) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        //            p.getValue().items.stream().forEach((item)->{
//                item.setTotalSeg(totalSeg);
//                item.setCdfsId(calculateID(combinedID));
//                remapped.put(p.getKey(), item);
//            });
//        });
//        remapped = joined.entrySet().stream().forEach(p->{ //drivename, intermediateResult
//            p.getValue().items.stream().forEach((item)->{
//                item.setTotalSeg(totalSeg);
//                item.setCdfsId(calculateID(combinedID));
//                remapped.put(p.getKey(), item);
//            });
//        });
        Log.d(TAG, "Items used to update the Allocation map" + remapped);
        return new HashMap<String, Collection<AllocationItem>>(remapped);
    }

    String calculateID(Collection<String> ids){return "";};

    HashMap<String, AllocationItem> Remap(String driveName, HashMap<String, AllocationItem> items){
        Map<String, AllocationItem> remapped;
        remapped = items.values().stream().map((v)->{
            Map.Entry<String, AllocationItem> entry = new Map.Entry<String, AllocationItem>() {
                @Override
                public String getKey() {
                    return driveName;
                }

                @Override
                public AllocationItem getValue() {
                    return v;
                }

                @Override
                public AllocationItem setValue(AllocationItem o) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        return new HashMap<>(remapped);
    }

    boolean isUploadCompleted(int total, ArrayBlockingQueue<File> Q1, LinkedBlockingQueue<File> Q2){
        boolean result = false;
        int number1 = Q1.size()-Q1.remainingCapacity();
        int number2 = Q2.size()-Q2.remainingCapacity();
        //Log.d(TAG, "expected: " + total + " size1: " + Q1.size() + " size2: " + Q2.size());
        if((Q1.size() + Q2.size()) >= total){
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
            Log.w(TAG, e.getMessage());
        }
    }

    //https://stackoverflow.com/questions/16369462/why-is-inputstream-close-declared-to-throw-ioexception
    /*
        We silence the exception when we are trying to close the stream because there is no way to handle it.
     */
    void closeStream(InputStream ins) {
        try {
            ins.close();
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    void printItems(HashMap<String, Collection<AllocationItem>> items) {
        Log.d(TAG, "Printing uploaded items:");
        items.forEach((k, v) -> {
            v.forEach((item) -> {
                Log.d(TAG,
                        "Drive: " + item.getDrive() +
                                ". Name: " + item.getName() +
                                ". Seq: " + item.getSequence() +
                                ". Item id: " + item.getItemId() +
                                ". cdfs id: " + item.getCdfsId() +
                                ". Parent: " + item.getPath() +
                                ". Size: " + item.getSize() +
                                ". CDFS Size: " + item.getCDFSItemSize() +
                                ". Total segs: " + item.getTotalSeg()
                );
            });
        });
    }
}
