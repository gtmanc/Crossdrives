package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.Infrastructure;
import com.crossdrives.cdfs.allocation.Names;
import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.IDProducer;
import com.crossdrives.cdfs.allocation.ISplitAllCallback;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.allocation.Splitter;
import com.crossdrives.cdfs.data.LocalFileCreator;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.model.UpdateContent;
import com.crossdrives.cdfs.allocation.QuotaEnquirer;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.util.ApplicableDriveListBuilder;
import com.crossdrives.cdfs.util.Delay;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.Wait;
import com.crossdrives.cdfs.util.collection.Allocation;
import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Upload {
    final String TAG = "CD.Upload";
    CDFS mCDFS;
    private final String NAME_CDFS_FOLDER = IConstant.NAME_CDFS_FOLDER;
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    final int MAX_CHUNK = 10;
    HashMap<String, CompletableFuture<InterMediateResult>> Futures= new HashMap<>();
    String cdfsFolderID;
    State mState;
    IUploadProgressListener mListener;
    int progressTotalSegment;
    int progressTotalUploaded;
    final String POISON_PILL = "PoisonPill";

    /*
        Result of splitting file and slice upload for each drive
     */
    class InterMediateResult{
        Collection<AllocationItem> items;
        Collection<Integer> errors = new ArrayList<>();
    }

    interface Error{
        int ERR_UPLOAD_CONTENT = 1; //error occurred during upload slice of file
        int ERR_QUEUE_TAKE = 2;     //blocking take is interrupted
        int ERR_SPLIT = 3;     //error occurred during splitting
    }

    /*
        Progress states
     */
    public enum State{
        GET_REMOTE_QUOTA_STARTED,
        GET_REMOTE_QUOTA_COMPLETE,
        PREPARE_LOCAL_FILES_STARTED,
        PREPARE_LOCAL_FILES_COMPLETE,
        MEDIA_IN_PROGRESS,
        MEDIA_COMPLETE,
        MAP_UPDATE_STARTED,
        MAP_UPDATE_COMPLETE;
    }

    public Upload(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public CompletableFuture<File> upload(File file, String name, List<CdfsItem> parents, IUploadProgressListener listener) throws FileNotFoundException {
        InputStream ins = new FileInputStream(file);
        return upload(ins, name, parents, listener);
    }

    /*
        Input:
        ins:        the content to upload in InputStream
        CdfsName:   name for CDFS.
        parents:    a list for parents. actually, the upload only needs the parent. Just keeps as it is for the future.
        listener:   for UI
    */
    public CompletableFuture<File> upload(InputStream ins, String CdfsName, @NonNull List<CdfsItem> parents, IUploadProgressListener listener)  {
        CompletableFuture<File> resultFuture = new CompletableFuture<>();
        CompletableFuture<File> workingFuture = CompletableFuture.supplyAsync(()->{
            ConcurrentHashMap<String, Drive> drives;
            mListener = listener;
            CdfsItem whereWeAre = parents.isEmpty() ? null : parents.get(parents.size()-1);

            String pathParent = Names.CompletePath(whereWeAre);
            HashMap<String, About.StorageQuota> quotaMap = null;
            HashMap<String, CompletableFuture<Integer>> splitCompleteFutures = new HashMap<>();

            //Limit the drive to be the ones that the folder(parent) was created. The drive signed in after the
            //folder is created will be removed.
            Log.d(TAG, "Signed in drives:" + mCDFS.getDrives());
            drives = ApplicableDriveListBuilder.build(mCDFS.getDrives(), whereWeAre);
            Log.d(TAG, "drives to proceed:" + drives);
            QuotaEnquirer enquirer = new QuotaEnquirer(drives);
            //Both queues need to be global as splitter will use
            HashMap<String, ArrayBlockingQueue<File>> toUploadQueueMap = new HashMap<>();
            HashMap<String, LinkedBlockingQueue<File>> remainingQueueMap = new HashMap<>();
            drives.keySet().forEach((driveName)->{
                ArrayBlockingQueue<File> toUploadQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                LinkedBlockingQueue<File> remainingQueue = new LinkedBlockingQueue<>();
                toUploadQueueMap.put(driveName, toUploadQueue);
                remainingQueueMap.put(driveName, remainingQueue);
            });

            callback(State.GET_REMOTE_QUOTA_STARTED);

            try {
                quotaMap  = Tasks.await(enquirer.getAll());
            } catch (ExecutionException | InterruptedException e) {
                Log.w(TAG, "get quota failed! " + e.getMessage());
                resultFuture.completeExceptionally(e);
                return null;
            }

            callback(State.GET_REMOTE_QUOTA_COMPLETE);

            HashMap<String, Long> allocation;
            HashMap<String, Collection<AllocationItem>> uploadedItems;
            final long[] uploadSize = {0};

            //number of slice produced by splitter. i.e. equivalent to totalSeg in CDFS item
            final int[] SeqNum = {AllocationItem.SEQ_INITIAL};

            //number of slice produced by splitter for each drive. Only used for check whether all scheduled
            //upload are completed so that we can close the upload thread for each drive.
            HashMap<String, Integer> totalSlicePerDrive = new HashMap<>();
            final boolean[] isSplitCompleted = {false};
            HashMap<String, Boolean> isSplitExceptionally = new HashMap<>();
            try {
                uploadSize[0] = (long)ins.available();
            } catch (IOException e) {
                Log.w(TAG, "Invalid upload size! " + e.getMessage());
                resultFuture.completeExceptionally(e);
                return null;
            }
            Log.d(TAG, "Number of Parents :" + parents.size());
            Log.d(TAG, "Path:" + pathParent + " Size to upload: " + uploadSize[0]);

            //Allocator allocator = new Allocator(quotaMap, file.length());
            Allocator allocator = new Allocator(quotaMap, uploadSize[0]);
            allocation = allocator.getAllocationResult();
            callback(State.PREPARE_LOCAL_FILES_STARTED);

            HashMap<String, AllocationItem> items = new HashMap<>();    //slice name, allocation item
            Splitter splitter = new Splitter(ins, allocation, CdfsName, MAX_CHUNK);
            splitter.splitAll(new ISplitAllCallback() {
                @Override
                public void start(String driveName, long total) {
                    Log.d(TAG, "Split start. Drive: " + driveName + ". allocated length: " + total);
                    //Should be fine that we initialize it here because the map will be read when 1st
                    //slice has been uploaded. Usually, upload is slower than the start callback of splitter
                    //even if upload is failed.
                    totalSlicePerDrive.putIfAbsent(driveName, 0);
                }

                @Override
                public void progress(String driveName, File slice, long len) {
                    AllocationItem item = new AllocationItem();
                    Log.d(TAG, "Split progress. Drive: " + driveName + ". Name: " + slice.getName());
                    toUploadQueueMap.get(driveName).add(slice);
                    remainingQueueMap.get(driveName).add(slice);    //the added item will be removed once upload successes

                    int slicePerDrive = totalSlicePerDrive.get(driveName);
                    slicePerDrive++;
                    totalSlicePerDrive.replace(driveName, slicePerDrive);
                    item.setSize(len);
                    item.setName(CdfsName);
                    item.setDrive(driveName);
                    /*
                        We have to assign the sequence number right here because the sequence
                        carries the order of the slices. It is used in compose of downloaded
                        slices in CDFS download. Note sequence starts with 1 rather than 0.
                    */
                    item.setSequence(SeqNum[0]);
                    SeqNum[0]++;  //TODO: do we have concurrent issue?

                    item.setPath(pathParent);
                    item.setAttrFolder(false);
                    item.setCDFSItemSize(uploadSize[0]);
                    items.put(slice.getName(), item);
                }

                @Override
                public void finish(String driveName, long remaining) {
                    File file = new File(POISON_PILL);
                    Log.d(TAG, "split for a drive done. Drive: " + driveName);
                    if(remaining!=0){Log.w(TAG, "split may completed with something wrong. remaining: " + remaining);};
                    //add poisonPill to inform the upload threads for each drive that the splitter is no longer to produce
                    //slice of file
                    toUploadQueueMap.get(driveName).add(file);
                }

                public void onFailurePerDrive(String driveName, String ex) {
                    Log.w(TAG, "Split file failed! Drive: " + driveName + ". " + ex);
                    isSplitExceptionally.put(driveName, true);
                    resultFuture.completeExceptionally(new Throwable(ex));
                }

                @Override
                public void completedAll(){
                    Log.d(TAG, "split for all drives completed.");
                    isSplitCompleted[0] = true;
                }
                @Override
                public void onFailure(String ex){
                    Log.w(TAG, "Split file failed!" + ex);
                }
            });


            allocation.entrySet().stream().filter((e)->e.getValue()>0).forEach((set)->{
                String driveName = set.getKey();
                Log.d(TAG, "Upload process ["  + driveName + "] starts");
                isSplitExceptionally.put(driveName, false);
                long allocatedLen = set.getValue();
                MapFetcher mapFetcher = new MapFetcher(drives);
                CompletableFuture<com.google.api.services.drive.model.File> checkFolderFuture =
                        mapFetcher.getBaseFolder(driveName);

                /*
                    Upload process for each drive. There could be number of threads run in parallel
                    1. Main working - CommitAllocationMapFuture
                    2. Splitter
                    3. Multiple drive client uploads
                 */
                CompletableFuture<InterMediateResult> CommitAllocationMapFuture = checkFolderFuture.thenComposeAsync((cdfsFolder)->{
                    CompletableFuture<InterMediateResult> future = new CompletableFuture<>();
                    // callback of splitter and drive client write error
                    // Main working thread writes totalSlice and items
                    InterMediateResult result = new InterMediateResult();

                    ArrayBlockingQueue<File> toFreeupQueue = new ArrayBlockingQueue<>(MAX_CHUNK);
                    int[] cntFailed = {0};
                    if(cdfsFolder == null){
                        Log.w(TAG, "CDFS folder is missing! Drive: " + driveName);
                        future.completeExceptionally(new Throwable("CDFS folder is missing. Drive: " + driveName));
                    }
                    cdfsFolderID = cdfsFolder.getId();

                    /*
                        Wait all upload works of the slice are committed to drive clients
                     */
                    while(true) {
                        //Error occurred or exception throw by splitter?
                        if(isSplitExceptionally.get(driveName)){
                            result.errors.add(Error.ERR_SPLIT);
                            break;
                        }

                        File localFile = null;
                        try {
                            localFile = toUploadQueueMap.get(driveName).take();
                        }catch (InterruptedException e) {
                            result.errors.add(Error.ERR_QUEUE_TAKE);
                            break;
                        }
                        //takeFromQueue(toUploadQueueMap.get(driveName));
                        //if(toUploadQueue.isEmpty()){Log.d(TAG, "To upload queue is empty!");}

                        //No more slice of file can be produced by splitter?
                        if (localFile.getName().equals(POISON_PILL)){
                            break;
                        }

//                      if(localFileOptional.isPresent()){
//                      File localFile = localFileOptional.get();
                        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();

                        // It looks strange. Google no longer support multiple folder starting with API v3. However, it still requires a list for parent.
                        //fileMetadata.setParents(Collections.singletonList(buildParentDriveIdList(driveName, parents).get(0)));
                        fileMetadata.setParents(Collections.singletonList(whereWeAre.getMap().get(driveName).get(0)));

                        fileMetadata.setName(localFile.getName());
                        Log.d(TAG, "Drive: " + driveName + ". local file to upload: " + localFile.getName());
                        mCDFS.getDrives().get(driveName).getClient().upload().
                                buildRequest(fileMetadata, localFile).run(new IUploadCallBack() {
                                    @Override
                                    public void success(com.crossdrives.driveclient.model.File file) {
                                        AllocationItem item;
                                        Log.d(TAG, "slice uploaded. Drive: " + driveName + ". name: " + file.getOriginalLocalFile().getName()
                                        );
                                        item = items.get(file.getOriginalLocalFile().getName());
                                        if(item == null) {Log.w(TAG,"item mot found! Drive: " + driveName);}
                                        item.setItemId(file.getFile().getId());
                                        progressTotalUploaded++;
                                        callback(State.MEDIA_IN_PROGRESS);
                                        toFreeupQueue.add(file.getOriginalLocalFile());
                                        LinkedBlockingQueue<File> remainingQueue = remainingQueueMap.get(driveName);
//                                        List l = Arrays.asList(remainingQueue.toArray());
//                                        Collection<File> collection = new ArrayList<File>(l);
//                                        remainingQueue.remove(collection.stream().filter((e)->{
//                                            return e.getName().equals(file.getOriginalLocalFile().getName());}).findAny().get());
                                        if(!Allocation.removeByName(remainingQueue, file.getOriginalLocalFile().getName())){
                                            Log.w(TAG, "Item may not removed from remaining list as expectedly!");
                                        }
                                    }

                                    @Override
                                    public void failure(String ex, File originalFile) {
                                        Log.w(TAG, "Slice upload failed! Drive: " + driveName + ". local file: " + originalFile.getName()
                                        + " " + ex);
                                        //remainingQueue.offer(originalFile);
                                        cntFailed[0]++;
                                        result.errors.add(Error.ERR_UPLOAD_CONTENT);
                                    }
                                });
                    }

                    Log.d(TAG, "Drive: " + driveName + ". slice upload is scheduled but not yet completed!");

                    Wait wait = new Wait(totalSlicePerDrive.get(driveName));
                    while(!wait.isCompleted(toFreeupQueue.size(), cntFailed[0]));

                    //while(!isUploadCompleted(totalSlicePerDrive.get(driveName), toFreeupQueue, cntFailed[0]));

                    Log.d(TAG, "Drive: " + driveName + ". slice upload is completed!");

                    Collection<File> collection = new ArrayList<>();
                    toFreeupQueue.drainTo(collection);
                    //It's okay to give a empty collection if there is no item in the queue. i.e. upload
                    //is terminated at the beginning.
                    splitter.cleanup(collection);
                    remainingQueueMap.get(driveName).drainTo(collection);
                    splitter.cleanup(collection);

                    //extract the items for the drive
                    Stream<AllocationItem> s = items.values().stream().filter((item)->item.getDrive().equals(driveName));
                    result.items = s.collect(Collectors.toCollection(ArrayList::new));
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
                });

                CommitAllocationMapFuture.exceptionally(ex ->{
                    Log.w(TAG, "Completed with exception in CommitAllocationMapFuture: " + ex.toString());

                    return null;
                });

            });

            /*
                Wait joined intermediate result.
                1. Wait for all of the split slices are available because we want to callback to UI
                as soon as the splitter finishes.
                2. Wait for intermediate results are available.
            */
            while(!isSplitCompleted[0]){
                Log.d(TAG, "isSplitCompleted: " + isSplitCompleted[0]);
                Delay.delay(2000);
            };

            progressTotalSegment = totalSlicePerDrive.values().stream().mapToInt((Integer::intValue)).sum();
            callback(State.PREPARE_LOCAL_FILES_COMPLETE);
            Log.d(TAG, "Callback to UI fr progress. Total segment: " + progressTotalSegment);
            callback(State.MEDIA_IN_PROGRESS);  //callback to UI to start the update of progress. i.e. progress 0%
            int totalSeg = SeqNum[0]-1;
            if(progressTotalSegment != totalSeg){
                Log.w(TAG, "Number of total slice or sequence number may not be correct! Total slice: " +
                        progressTotalSegment + " SeqNum: " + SeqNum[0]);
            }
            HashMap<String, InterMediateResult> joined = getJoinedResult(Futures);
            callback(State.MEDIA_COMPLETE);

            //Terminate here if any of slice is not uploaded or any error occurred during split.
            if(!checkResult(joined)){
                //TODO: cleanp orphan files
                Log.w(TAG, "Error occurred during split and upload!");
                resultFuture.completeExceptionally(new Throwable("Error occurred during split and upload!"));
                return null;
            }

//            if(SplitterminateExceptionally[0]){
//                //TODO: cleanp orphan files
//                Log.w(TAG, "Error occurred during splitting the file!");
//                resultFuture.completeExceptionally(new Throwable("Error occurred during splitting the file!"));
//                return null;
//            }
//
//            Log.d(TAG, "size of remaining queue: " + remainingQueue.size());
//            if(remainingQueue.size() != 0){
//                //TODO: cleanp orphan files
//                Log.w(TAG, "slice of the file may not be uploaded!");
//                resultFuture.completeExceptionally(new Throwable("slice of the file may not be uploaded!"));
//                return null;
//            }

            uploadedItems = completeItems(totalSeg, joined);
            printItems(uploadedItems);

            /*
                Lock remote allocation maps?
                A signed in app still can edit a locked file although it has been locked. It means
                that locking a file doesn't actually protect the content from being modified of a file
                as we expected.
                This also makes sense becuase lock/unlcok as well known check out/in is not the modern
                concept. Instead, update a file from multiple source would be the best solution. i.e. Google/MS Word Doc
            */
            /*
                build up the drive list only contains the drive which we need to continue
             */
            ConcurrentHashMap<String, Drive> reducedDriveList = new ConcurrentHashMap<>();
            uploadedItems.entrySet().stream().forEach((set)->{
                reducedDriveList.put(set.getKey(), drives.get(set.getKey()));
            });

            Log.d(TAG, "reduced drive list: " + reducedDriveList);
            MapFetcher mapFetcher = new MapFetcher(reducedDriveList);

            callback(State.MAP_UPDATE_STARTED);
            CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> mapIDFuture = mapFetcher.listAll(whereWeAre);
            HashMap<String, com.google.api.services.drive.model.File> mapIDs = mapIDFuture.join();
            if(mapIDs.values().stream().anyMatch((v)->v==null)){
                Log.w(TAG, "map is missing!");
                resultFuture.completeExceptionally(new Throwable("map is missing!"));
                //TODO: clean up orphan files
                return null;
            }

            CompletableFuture<HashMap<String, OutputStream>> mapStreamFuture = mapFetcher.pullAll(whereWeAre);
            AllocManager am = new AllocManager();
            HashMap<String, AllocContainer> containers = Mapper.reValue(mapStreamFuture.join(), (in)->{
                return am.toContainer(in);
            });


            containers.forEach((k, v)->{
                v.addItems(uploadedItems.get(k));
            });

//                MapLocker locker = new MapLocker(drives);
//                CompletableFuture<HashMap<String, ContentRestriction>> lockStatusFuture = locker.getStatus(mapIDs);
//                lockStatusFuture.join().entrySet().stream().forEach((set)->
//                        Log.d(TAG,"Drive: " + set.getKey() + " read only? " + set.getValue().getReadOnly()));


            Log.d(TAG, "generate local map files...");
            HashMap<String, UpdateContent> localMaps = Mapper.reValue(containers, (driveName, container)->{
                Gson gson = new Gson();
                LocalFileCreator creator = new LocalFileCreator(SnippetApp.getAppContext());
                UpdateContent content = new UpdateContent();
                content.setID(mapIDFuture.join().get(driveName).getId());
                String localMapName = driveName + "_map.txt";
                content.setMediaContent(creator.create(localMapName, gson.toJson(container)));
                return content;
            });

            Log.d(TAG, "update local map files...");
            MapUpdater updater = new MapUpdater(reducedDriveList);

            //TODO: what will happen if two source update the same file in remote?
            CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                    = updater.updateAll(localMaps);

            updateFuture.join();

            callback(State.MAP_UPDATE_COMPLETE);

            Log.d(TAG, "update is completed successfully.");
            resultFuture.complete(null);
            return null;    //not sure whether we really have to return something...
        });

        /*
            exceptionally is called no matter what kind type of exception is thrown.
            i.e. checked or run time exception.
         */
        workingFuture.exceptionally((ex)->{
            Log.w(TAG, "work future exceptionally: " + ex.getMessage());
            //Log.w(TAG, Arrays.stream(ex.getStackTrace()).findFirst().toString());
            ex.printStackTrace();
            resultFuture.completeExceptionally(ex);
            return null;
        });
        return resultFuture;
    }

    public State getState(){return mState;}

    public int getProgressMax(){return progressTotalSegment;}

    public int getProgressCurrent(){return progressTotalUploaded;}

    HashMap<String, InterMediateResult> getJoinedResult( HashMap<String, CompletableFuture<Upload.InterMediateResult>> futures){
        Log.d(TAG, "Awaiting joined result...");

        return Mapper.reValue(futures, (future)->{
            return future.join();
        });
    }

    /*
        Return:
            false:  any of the errors presents
            true:   no error presents
     */
    boolean checkResult(HashMap<String, InterMediateResult> joined){
        boolean conclusion = false;

        if(joined.values().stream().allMatch((result)->{
            return result.errors.isEmpty();})){
            conclusion = true;
        };
        return conclusion;
    }

    /*
        The CDFSID and totalSeg are added to the joined result in this method
     */

    HashMap<String, Collection<AllocationItem>> completeItems(int totalSeg, HashMap<String, InterMediateResult> joined){
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

        /*
            fill the totalSlice and CDFS ID to all of the items and map to the the result
         */
        HashMap<String, Collection<AllocationItem>> remapped= Mapper.reValue(joined, (interMediateResult)->{
            return interMediateResult.items.stream().map((item) -> {
                item.setCdfsId(IDProducer.deriveID(combinedID));
                item.setTotalSeg(totalSeg);
                return item;

            }).collect(Collectors.toCollection(ArrayList::new));
        });

        Log.d(TAG, "Items used to update the Allocation map" + remapped);
        return remapped;
    }


    void callback(State state){
        mState = state;
        mListener.progressChanged(this);
    }



    /*
        This is only required for Google drive. Microsoft use path instead.
        Input: CDFS items contains the folder where we will upload to.
        Inputting an empty item list or null to get meta data of base folder (CDFS).
        An empty list is returned if base folder is given.
     */
    @NonNull List<String> buildParentDriveIdList(@NonNull String driveName, @Nullable List<CdfsItem> items){
        List<String> idList = new ArrayList<>();


        if(items.size() == 0 || items == null)  //root?
        {
            Log.d(TAG, "Build id list for base.");
            Infrastructure builder = new Infrastructure(driveName, mCDFS.getClient(driveName), mCDFS);
            idList.add(builder.getMetaDataBase(driveName).join().getId());
        }else {
            Log.d(TAG, "Build id list for parents.");
            items.stream().forEachOrdered(item->{
                idList.add(item.getMap().get(driveName).get(0));
            });
        }

        Log.d(TAG, "Print out id list:");
        idList.forEach((id)->{
            Log.d(TAG, id);
        });

        Log.d(TAG, "id list build ok. Size: " + idList.size());
        return idList;
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
