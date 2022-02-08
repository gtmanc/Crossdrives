package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Infrastructure {
    final private static String TAG = "CD.Infrastructure";
    IDriveClient mClient;
    private FileList mFileList;
    private OutputStream mStream;
    private String mFileId;
    IFileListCallBack<FileList, Object> mCallback;

    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    /*
        Strings
     */
    private final String CDFS_FOLDER = "CDFS";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = 'application/vnd.google-apps.folder' " +
            "and name = '" + CDFS_FOLDER + "'";

    /*
    A flag used to wait until the drive client callback gets called. Always set to false each time an operation
    is performed.
    */
    private boolean mResponseGot = false;

    public Infrastructure(IDriveClient client) {
        mClient = client;
        //mCallback = callback;
    }

    public void checkAndBuild() {
        CompletableFuture<String> checkFolderFuture = new CompletableFuture<>();

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
            }
            return future;
        });

        CompletableFuture  downloadAllocationFileFuture = checkAllocationFileFuture.thenAccept(id->{
            CompletableFuture<String> future = new CompletableFuture<>();
            if(id != null){
                Log.d(TAG, "download allocation file");
                mClient.download().buildRequest(id)
                        .run(new IDownloadCallBack<OutputStream>() {
                            @Override
                            public void success(OutputStream outputStream) {
                                handleResultDownload(outputStream);
                            }

                            @Override
                            public void failure(String ex) {

                            }
                        });
            }else{
                Log.d(TAG, "Allocation file is missing. Create the file.");
            }

        });

        /*
            exception handling
         */
        checkFolderFuture.exceptionally(
                // TODO
                ex ->{return null;});
        checkAllocationFileFuture.exceptionally(
                ex ->{return null;}
        );

        downloadAllocationFileFuture.exceptionally(
                ex -> {return null;});
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
