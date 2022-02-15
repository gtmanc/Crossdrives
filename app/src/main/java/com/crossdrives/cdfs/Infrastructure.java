package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Infrastructure{
    final private static String TAG = "CD.Infrastructure";
    IDriveClient mClient;
//    private FileList mFileList;
//    private OutputStream mStream;
//    private String mFileId;
    //IFileListCallBack<FileList, Object> mCallback;
    String mDriveName;
    String mFolderID;
    final String NAME_ALLOCATION_FILE = "allocation.cdfs";


    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    /*
        Strings
     */
    private final String CDFS_FOLDER = "CDFS";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = 'application/vnd.google-apps.folder' " +
            "and name = '" + CDFS_FOLDER + "'";
    private CDFS mCDFS;

    /*
    A flag used to wait until the drive client callback gets called. Always set to false each time an operation
    is performed.
    */
    private boolean mResponseGot = false;

    public Infrastructure(String name, IDriveClient client, CDFS cdfs) {
        mClient = client;
        mDriveName = name;
        mCDFS = cdfs;
    }

    public void checkAndBuild() {
        CompletableFuture<String> checkFolderFuture = new CompletableFuture<>();

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
                            String id = null;
                            id = handleResultGetCDFSFolder(fileList);
                            checkFolderFuture.complete(id);
                        }

                        @Override
                        public void failure(String ex) {
                            checkFolderFuture.completeExceptionally(new Throwable(""));
                        }
                    });
        });

        /*
            Check allocation file
         */
        CompletableFuture<String> checkAllocationFileFuture = checkFolderFuture.thenCompose(id -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            String query = "'" + id + "' in parents";

            if(id != null){
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
                                id = handleResultGetAllocationFile(fileList);
                                future.complete(id);
                            }

                            @Override
                            public void failure(String ex) {
                                future.completeExceptionally(new Throwable(""));
                            }
                        });
            }else {
                Log.d(TAG, "CDFS folder is missing. Start to create necessary files");
                future.complete(null);
            }
            return future;
        });
        /*
            Download allocation file
         */
        CompletableFuture<Boolean>  downloadAllocationFileFuture = checkAllocationFileFuture.thenCompose(id->{
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            if(id != null){
                Log.d(TAG, "download allocation file");
                mClient.download().buildRequest(id)
                        .run(new IDownloadCallBack<OutputStream>() {
                            @Override
                            public void success(OutputStream outputStream) {
                                handleResultDownload(outputStream);
                                future.complete(false);
                            }

                            @Override
                            public void failure(String ex) {
                                future.completeExceptionally(new Throwable(""));
                            }
                        });
            }else{
                Log.d(TAG, "Allocation file is missing");
                future.complete(true);
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
        downloadAllocationFileFuture.thenAccept((result)->{
            String json = null;
            if(result){
                Log.d(TAG, "Create necessary files");
                json = AllocManager.newAllocation();
                Log.d(TAG, "Json: " + json);
                //write to local
                FileLocal fc = new FileLocal(mCDFS);
                fc.create(NAME_ALLOCATION_FILE, json);
                Log.d(TAG, "Creataion finished");
                json = fc.read(NAME_ALLOCATION_FILE);
                Log.d(TAG, "read back: " + json); //{"items":[],"version":1}

                //
                /*
                    upload necessary files to remote cdfs folder. If folder doens't 
                    It's observed that the behavior of uploading file with the same name of existing file
                    varies cross drives. e.g. Onedrive always overwrite the existing one. Google drive create a
                    new one instead
                */
            }
        }).handle((s, t) -> {
            Log.w(TAG, "Exception occurred in handle download result: " + t.toString());
            return null;
        });
        /*
            exception handling
         */
        checkFolderFuture.exceptionally(
                // TODO
                ex ->{Log.w(TAG, ex.toString()); return null;});
        checkAllocationFileFuture.exceptionally(
                ex ->{Log.w(TAG, ex.toString()); return null;}
        );

        downloadAllocationFileFuture.exceptionally(
                ex -> {Log.w(TAG, ex.toString()); return null;});

        downloadAllocationFileFuture.handle((s, t) -> {
            Log.w(TAG, "Exception occurred: " + t.toString());
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
        String id = null;
        if(fileList.getFiles().size() > 0) {
            if (fileList.getFiles().get(0).getName().compareToIgnoreCase("allocation.cdfs") == 0) {
                id = fileList.getFiles().get(0).getId();
            } else {
                Log.w(TAG, "No allocation file in cdfs folder!");
            }
        }
        return id;
    }

    private void handleResultDownload(OutputStream outputStream){
        AllocManager am = new AllocManager();
        AllocContainer ac;
        ac = am.toContainer(outputStream);
        mCDFS.mDrives.get(mDriveName).addContainer(ac);
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


//    /*
//        Download content of a file
//     */
//    @Override
//    public Task<OutputStream> download(String id) {
//        Task task;
//        Log.d(TAG, "Operation: download " + id);
//        task = Tasks.call(sExecutor, new Callable<Object>() {
//            @Override
//            public OutputStream call() throws Exception {
//                msTaskfinished = false;
//                mDrives.values().iterator().next().download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
//                    //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
//                    @Override
//                    //public void success(InputStream inputStream){
//                    public void success(OutputStream os){
//                        exitWait();
//                        mStream = os;
//
//                        Log.d(TAG, "download finished.");
////                        try {
////                            os.close();
////                        } catch (IOException e) {
////                            e.printStackTrace();
////                        }
//                    }
//
//                    @Override
//                    public void failure(String ex) {
//                        exitWait();
//                        Log.w(TAG, "download failed" + ex.toString());
//                    }
//                });
//                waitUntilFinished();
//                if(mStream == null){ Log.w(TAG, "stream is null" );}
//                return mStream;
//            }
//        });
//
//        return task;
//    }
//
//    @Override
//    public Task<String> upload(File metadata, java.io.File path) {
//
//        Task task;
//        Log.d(TAG, "CDFS: upload file. " + path.toString());
//        task = Tasks.call(sExecutor, new Callable<Object>() {
//            @Override
//            public String call() throws Exception {
//
//                msTaskfinished = false;
//                /*
//                    Drive client test only. Always use index 0 (i.e first one added)
//                 */
//                mDrives.values().iterator().next().upload().buildRequest(metadata, path).run(new IUploadCallBack() {
//                    @Override
//                    public void success(File file) {
//                        exitWait();
//                        mFileId = file.getId();
//                        Log.d(TAG, "Upload file OK. ID: " + file.getId());
//                    }
//
//                    @Override
//                    public void failure(String ex) {
//                        exitWait();
//                        Log.w(TAG, "Failed to upload file: " + ex.toString());
//                    }
//                });
//                waitUntilFinished();
//                return mFileId;
//            }
//        });
//        return task;
//    }
//
//    @Override
//    public Task<String> create(File metadata) {
//
//        Task task;
//        Log.d(TAG, "CDFS create file: " + metadata.getName());
//        task = Tasks.call(sExecutor, new Callable<Object>() {
//            @Override
//            public String call() throws Exception {
//
//                msTaskfinished = false;
//                /*
//                    Drive client test only. Always use index 0 (i.e first one added)
//                 */
//                mDrives.values().iterator().next().create().buildRequest(metadata).run(new ICreateCallBack<File>() {
//                    @Override
//                    public void success(File file) {
//                        exitWait();
//                        mFileId = file.getId();
//                        Log.d(TAG, "Create file OK. ID: " + file.getId());
//                    }
//
//                    @Override
//                    public void failure(String ex) {
//                        exitWait();
//                        Log.w(TAG, "Failed to create file: " + ex.toString());
//                    }
//                });
//                waitUntilFinished();
//                return mFileId;
//            }
//        });
//        return task;
//    }
//
//    @Override
//    public Task<String> delete(File metadata) {
//
//        Task task;
//        Log.d(TAG, "CDFS delete file: " + metadata.getName());
//        task = Tasks.call(sExecutor, new Callable<Object>() {
//            @Override
//            public String call() throws Exception {
//
//                msTaskfinished = false;
//                /*
//                    Drive client test only. Always use index 0 (i.e first one added)
//                 */
//                mDrives.values().iterator().next().delete().buildRequest(metadata).run(new IDeleteCallBack<File>() {
//                    @Override
//                    public void success(File file) {
//                        exitWait();
//                        mFileId = file.getId();
//                        Log.d(TAG, "Delete file OK. ID: " + file.getId());
//                    }
//
//                    @Override
//                    public void failure(String ex) {
//                        exitWait();
//                        Log.w(TAG, "Failed to delete file: " + ex.toString());
//                    }
//                });
//                waitUntilFinished();
//                return mFileId;
//            }
//        });
//        return task;
//    }
}
