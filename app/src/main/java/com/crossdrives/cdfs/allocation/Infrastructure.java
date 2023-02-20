package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.IAllocManager;
import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.data.FileLocal;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.collection.Files;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.create.ICreateCallBack;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.model.MediaData;
import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Infrastructure{
    final private static String TAG = "CD.Infrastructure";
    IDriveClient mClient;
//    private FileList mFileList;
//    private OutputStream mStream;
//    private String mFileId;
    //IFileListCallBack<FileList, Object> mCallback;
    String mDriveName;
    private final String NAME_ALLOCATION_ROOT = Names.allocFile(null);

    private class Result{
        public String folder;
        public String file;
        public boolean valid;

        private Result() {
            folder = null;
            file = null;
            valid = false;
        }
    }


    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    /*
        Strings
     */
    private final String NAME_CDFS_FOLDER = IConstant.NAME_CDFS_FOLDER;
    private final String MINETYPE_FOLDER = IConstant.MIMETYPE_FOLDER;
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";

    private CDFS mCDFS;
    private ConcurrentHashMap<String, Drive> mDrives;
    /*
    A flag used to wait until the drive client callback gets called. Always set to false each time an operation
    is performed.
    */
    private boolean mResponseGot = false;

    public Infrastructure(String name, IDriveClient client, CDFS cdfs) {
        mClient = client;
        mDriveName = name;
        mCDFS = cdfs;
        mDrives = cdfs.getDrives();
    }

    public void checkAndBuild() {
        CompletableFuture<Result> checkFolderFuture = new CompletableFuture<>();

        /*
            Check CDFS folder
        */
        sExecutor.submit(() -> {
            Log.d(TAG, "Check CDFS folder: " + FILTERCLAUSE_CDFS_FOLDER);
            mClient.list().buildRequest()
                    //sClient.get(0).list().buildRequest()
                    .setNextPage(null)
                    .setPageSize(0) //0 means no page size is applied
                    .filter(FILTERCLAUSE_CDFS_FOLDER)
                    //.filter("mimeType = 'application/vnd.google-apps.folder'")
                    //.filter(null)   //null means no filter will be applied
                    .run(new IFileListCallBack<FileList, Object>() {
                        //As we specified the folder name, suppose only cdfs folder in the list.
                        @Override
                        public void success(FileList fileList, Object o) {
                            Result result = new Result();
                            result.folder = handleResultGetCDFSFolder(fileList);
                            checkFolderFuture.complete(result);
                        }

                        @Override
                        public void failure(String ex) {
                            checkFolderFuture.completeExceptionally(new Throwable(ex));
                        }
                    });
        });

        /*
            Check allocation file
         */
        CompletableFuture<Result> checkAllocationFileFuture = checkFolderFuture.thenCompose(result -> {
            CompletableFuture<Result> future = new CompletableFuture<>();
            String query = "'" + result.folder + "' in parents";

            if(result.folder != null){
                Log.d(TAG, "Check allocation file. Query:  " + query);
                mClient.list().buildRequest()
                        //sClient.get(0).list().buildRequest()
                        .setNextPage(null)
                        .setPageSize(0) //0 means no page size is applied
                        //.filter("name = 'allocation.cdfs'" + "in 'CDFS'")
                        .filter(query)
                        //.filter(null)   //null means no filter will be applied
                        .run(new IFileListCallBack<FileList, Object>() {
                            @Override
                            public void success(FileList fileList, Object o) {
                                String id = null;
                                result.file = handleResultGetAllocationFile(fileList);
                                future.complete(result);
                            }

                            @Override
                            public void failure(String ex) {
                                future.completeExceptionally(new Throwable(""));
                            }
                        });
            }else {
                /*
                    Directly pass the result to next stage. The ID of allocation file will be null.
                 */
                Log.d(TAG, "CDFS folder is missing");
                future.complete(result);
            }
            return future;
        });
        /*
            Download allocation file
         */
        CompletableFuture<Result>  downloadAllocationFileFuture = checkAllocationFileFuture.thenCompose(result->{
            CompletableFuture<Result> future = new CompletableFuture<>();
            if(result.file != null){
                Log.d(TAG, "download root allocation file");
                mClient.download().buildRequest(result.file)
                        .run(new IDownloadCallBack<MediaData>() {

                            @Override
                            public void success(MediaData mediaData) {
                                result.valid = handleResultDownload(mediaData.getOs());
                                future.complete(result);
                            }

                            @Override
                            public void failure(String ex) {
                                future.completeExceptionally(new Throwable(""));
                            }
                        });
            }else{
                Log.d(TAG, "Allocation file is missing");
                result.valid = false;
                future.complete(result);
//                AllocContainer map = new AllocContainer();
//                AllocationItem item1 = new AllocationItem();
//                AllocationItem item2 = new AllocationItem();
//                String jsonStr;
//                Gson gson = new Gson();
//                item1.setBrand("Google");
//                item2.setBrand("Microsoft");
//                map.setVersion(1);
//                map.setAllocItem(item1);
//                map.setAllocItem(item2);
//                Log.d(TAG, "Json by Gson: " + gson.toJson(map));
//                //Result : {"items":[{"brand":"Google","seqNum":0,"size":0,"totalSeg":0},{"brand":"Microsoft","seqNum":0,"size":0,"totalSeg":0}],"version":1}
//                Log.d(TAG, "Parse json string...");
//                jsonStr = gson.toJson(map);
//                //test whether version still can be read if layout is incompatible with container
//                jsonStr = jsonStr.replace("brand", "my_brand");
//                map = gson.fromJson(jsonStr, AllocContainer.class);
//                Log.d(TAG, "version:" + map.getVersion());
//                Log.d(TAG, "item brand:" + map.getAllocItem().get(0).getBrand());

            }
            return future;
        });

        /*
            Create necessary file and upload to the remote
         */
        CompletableFuture<Result> createFolderFuture = downloadAllocationFileFuture.thenCompose((result)->{
            CompletableFuture<Result> future = new CompletableFuture<>();

            if(result.folder == null) {
                /*
                    create folder
                 */
                File fileMetadata = new File();
                fileMetadata.setName(NAME_CDFS_FOLDER);
                fileMetadata.setMimeType(MINETYPE_FOLDER);
                mClient.create().buildRequest(fileMetadata).run(new ICreateCallBack<File>() {
                    @Override
                    public void success(File file) {
                        Log.d(TAG, "Create file OK. ID: " + file.getId());
                        result.folder = file.getId();
                        future.complete(result);
                    }

                    @Override
                    public void failure(String ex) {
                        Log.w(TAG, "Failed to create file: " + ex.toString());
                        future.completeExceptionally(new Throwable(ex.toString()));
                    }
                });
            }else {
                Log.d(TAG, "Folder exists.");
                future.complete(result);
            }
            return future;
        });

        CompletableFuture<Result> createFilesFuture = createFolderFuture.thenCompose(result -> {
            CompletableFuture<Result> future = new CompletableFuture<>();

            if(result.file == null){
                String json;
                File fileMetadata = new File();
                FileLocal fc = new FileLocal(SnippetApp.getAppContext());
                AllocManager am = new AllocManager(mCDFS);
                /*
                    Generate local allocation file in the folder create at previous stage.
                    Then, upload it to the remote.
                */
                Log.d(TAG, "Create allocation file");

                /*
                    Set drive name now is adding so that allocation manager knows which test items
                    should be created. Only keep this if you want to do the test.
                 */
                //am.setDriveNameForTest(mDriveName);

                json = am.newAllocation();

                //Log.d(TAG, "Json: " + json);
                //write to local
                fc.create(NAME_ALLOCATION_ROOT, json);
                Log.d(TAG, "Creataion finished");
                json = fc.read(NAME_ALLOCATION_ROOT);
                Log.d(TAG, "read back: " + json); //{"items":[],"version":1}
                /*
                    upload necessary files to remote cdfs folder. If folder doesn't
                    It's observed that the behavior of uploading file with the same name of existing file
                    varies cross drives. e.g. Onedrive always overwrite the existing one. Google drive create a
                    new one instead
                */
                //fileMetadata.setParents(Collections.singletonList("16IhpPc0_nrrDplc73YIevRI8C27ir1JG")); //cdfs
                //fileMetadata.setParents(Collections.singletonList("CD26537079F955DF!5758"));  //AAA
                java.io.File path = new java.io.File(SnippetApp.getAppContext().getFilesDir() + "/" + NAME_ALLOCATION_ROOT);
                fileMetadata.setParents(Collections.singletonList(result.folder));
                //fileMetadata.setParents(null); //Set parent to null if you want to upload file to root
                fileMetadata.setName(NAME_ALLOCATION_ROOT);
                mClient.upload().buildRequest(fileMetadata, path).run(new IUploadCallBack() {
                    @Override
                    public void success(com.crossdrives.driveclient.model.File file) {
                        Log.d(TAG, "Upload file OK. ID: " + file.getFile().getId());
                        result.file = file.getFile().getId();
                        future.complete(result);
                    }

                    @Override
                    public void failure(String ex, java.io.File originalFile) {
                        Log.w(TAG, "Failed to upload file: " + ex.toString());
                        future.completeExceptionally(new Throwable(ex.toString()));
                    }
                });
            }else{
                Log.d(TAG, "Allocation.cdfs exist");
                future.complete(result);
            }
            return future;
        });

        createFilesFuture.thenAccept(result -> {
           /*
                Infrastructure supposes to be constructed
            */
            if(result.folder == null)
            {
                Log.w(TAG, "folder ID is null!");
            }
            if(result.file == null){
                Log.w(TAG, "file ID is null!");
            }
            else{
                Log.d(TAG, "build infrastructure completed");
            }
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

        checkAllocationFileFuture.exceptionally(ex ->{
            Log.w(TAG, "Completed with exception in allocation file check: " + ex.toString());
            return null;}
        ).handle((s, t)->{
            Log.w(TAG, "Exception occurred in file check: " + t.toString());
            return null;
        });

        downloadAllocationFileFuture.exceptionally(ex -> {
            Log.w(TAG, "Completed with exception in download allocation file: " + ex.toString());
            return null;
        }).handle((s, t)->{
            Log.w(TAG, "Exception occurred in download allocation file: " + t.toString());
            return null;
        });

        createFolderFuture.exceptionally( ex ->{
            Log.w(TAG, "Completed with exception in create afolder: " + ex.toString());
            return null;
        }).handle((s, t) -> {
            Log.w(TAG, "Exception occurred in create folder " + t.toString());
            return null;
        });
        createFilesFuture.exceptionally(ex->{
            Log.w(TAG, "Completed with exception in create and upload allocation file: " + ex.toString());
            return null;
        }).handle((s, t) ->{
            Log.w(TAG, "Exception occurred in create and upload allocation file " + t.toString());
            return null;
        });
    }

    private String handleResultGetCDFSFolder(FileList fileList){
        String id = null;
//      for(int i = 0 ; i < fileList.getFiles().size(); i++){
//          fileList.getFiles().get(i).getName().compareToIgnoreCase("cdfs");
//      }
        if(fileList.getFiles().size() > 0) {
            if (fileList.getFiles().get(0).getName().compareToIgnoreCase("cdfs") == 0) {
                id = fileList.getFiles().get(0).getId();
            }
        }
        else{
            Log.w(TAG, "No CDFS folder!");
            //Terminate the flow and start to create the necessary files.
        }

        return id;
    }

    private String handleResultGetAllocationFile(FileList fileList){
        AtomicReference<String> id = new AtomicReference<>();
        Optional<File> files = null;
//        if(fileList.getFiles().size() > 0) {
//            if (fileList.getFiles().get(0).getName().compareToIgnoreCase(NAME_ALLOCATION_ROOT) == 0) {
//                id = fileList.getFiles().get(0).getId();
//            } else {
//                Log.w(TAG, "No root allocation file in cdfs folder!");
//            }
//        }
        int num = fileList.getFiles().size();
        if(num > 0) {Log.d(TAG, "Files found in CDFS folder. Number of file: " + num);}

        files = fileList.getFiles().stream().filter((file)->{
            String name = file.getName();
            Log.d(TAG, "File found: " + name);
            return name.compareToIgnoreCase(NAME_ALLOCATION_ROOT) == 0 ?  true : false;
        }).findAny();
        if(!files.isPresent()){Log.w(TAG, "No root allocation file presents!");}
        files.ifPresent((file) -> {
            Log.d(TAG, "Root allocation file presents.");
            id.set(file.getId());});
        return id.get();
    }

    /*
        Input:
        outputStream: the stream to the allocation file content.
        return: indicates whether the content is valid or not.
     */
    private boolean handleResultDownload(OutputStream outputStream){
        AllocContainer ac;
        AllocManager am = new AllocManager(mCDFS);
        ac = am.toContainer(outputStream);
        if(am.checkCompatibility(ac) == IAllocManager.ERR_COMPATIBILITY_SUCCESS){
            /*
                This is no longer because the remote allocation will be synchronized in the List.
             */
            //am.saveNewAllocation(ac, mDriveName);
        }else{
            Log.w(TAG, "allocation version is not compatible!");
        }

        mCDFS.getDrives().get(mDriveName).addContainer(ac);
        return true;
    }




    private void init(){
        mResponseGot = false;
    }

    private void await() throws InterruptedException {
        while(mResponseGot == true){ Thread.sleep(200); };
    }

    private void signal(){
        mResponseGot = true;
    }



    /*
        Get meta data of base folder in all available user's drives
        Return: meta data of the base folders
    */
    public CompletableFuture<HashMap<String, File>> getMetaDataBaseAll(){
        CompletableFuture<HashMap<String, File>> metaDataFutures = new CompletableFuture<>();
        HashMap<String, CompletableFuture<File>> metaDataMap = new HashMap<>();

        mDrives.keySet().stream().forEach((key)->{
            metaDataMap.put(key, getMetaDataBase(key));
        });

        metaDataFutures = CompletableFuture.supplyAsync(()->{
          return Mapper.reValue(metaDataMap, (future)->{
              return future.join();
          });
        });

        return metaDataFutures;
    }


    /*
        get metadata of base folder (CDFS) in user drive.
        Return: meta data of the base folder
     */
    public CompletableFuture<File> getMetaDataBase(String driveName){
        Fetcher fetcher= new Fetcher(mDrives);
        CompletableFuture<FileList> fileListFuture;

        fileListFuture =  fetcher.list(driveName, "");

        CompletableFuture<File> folder =
                CompletableFuture.supplyAsync(()->{
                    File f = Files.getFolder(fileListFuture.join(), Names.baseFolder());
                    Log.d(TAG, "CDFS folder found. ID: " + f.getId());
                    return f;
                });

        return folder;
    }
}