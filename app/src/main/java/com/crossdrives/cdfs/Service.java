package com.crossdrives.cdfs;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.crossdrives.cdfs.allocation.Result;
import com.crossdrives.cdfs.exception.CompletionException;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.InvalidArgumentException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.list.ICallbackList;
import com.crossdrives.cdfs.list.List;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.cdfs.upload.Upload;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    Interface for application.
    Google play service task api is adopted so that the application can decide the thread can be used
    to run.
 */
public class Service implements IService{
    private static String TAG = "CD.Service";
    CDFS mCDFS;
    private FileList mFileList;
    private OutputStream mStream;

    final String NOTIFICATION_CH_ID = "0" ;

    public Service(CDFS cdfs) {
        mCDFS = cdfs;
    }

    //private final Executor sExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    /*
        TODO: We should allow to run the operations in parallel.
        Maybe the lock for list can be removed.
     */
    final Lock listLock = new ReentrantLock();
    final Lock uploadLock = new ReentrantLock();
    final Condition queryFinished  = listLock.newCondition();
    final Condition uploadFinished  = listLock.newCondition();
    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;

    IUploadProgressListener uploadProgressListener;

    /*
        Operation: get file list
         */
//    public Task<FileList> list(Object nextPage){
//        Task task;
//
//
//        Log.d(TAG, "Service: list files. nextPage: " + nextPage);
//        task = Tasks.call(sExecutor, new Callable<Object>() {
//            @Override
//            public FileList call() throws Exception {
//                msTaskfinished = false;
//                /*
//                    Drive client test only. Always use index 0 (i.e first one added)
//                 */
//                mCDFS.getDrives().values().iterator().next().getClient().list().buildRequest()
//                        //sClient.get(0).list().buildRequest()
//                        .setNextPage(nextPage)
//                        .setPageSize(0) //0 means no page size is applied
//                        //.filter("mimeType = application/vnd.google-apps.folder and name contains 'cdfs'")
//                        //.filter("mimeType = application/vnd.google-apps.folder")
//                        .filter(null)   //null means no filter will be applied
//                        .run(new IFileListCallBack<FileList, Object>() {
//                            @Override
//                            public void success(FileList fileList, Object o) {
//                                exitWait();
//                                mFileList = fileList;
//                                Log.d(TAG, "list finished");
//                            }
//
//                            @Override
//                            public void failure(String ex) {
//                                exitWait();
//                                Log.w(TAG, "list finished with failure!");
//                            }
//                        });
//                waitUntilFinished();
//                return mFileList;
//            }
//        });
//
//        return task;
//    }
    @Override
    public Task<com.crossdrives.cdfs.Result> list(Object nextPage) throws MissingDriveClientException, GeneralServiceException{
        List list = new List(mCDFS);
        final FileList[] fileList = {null};
        final Throwable[] throwables = {null};
        final java.util.List<Result>[] allocCheckResults = new java.util.List[]{null};
        com.crossdrives.cdfs.Result result = new com.crossdrives.cdfs.Result();
        Task task;


        Log.d(TAG, "Service: list files. nextPage: " + nextPage);

        mCDFS.requiresDriveClientNonNull();

        task = Tasks.call(mExecutor, new Callable<Object>() {
            @Override
            public com.crossdrives.cdfs.Result call() throws Exception {
//                listLock.lock();
                CompletableFuture<FileList> future = new CompletableFuture<>();
                list.list(null, new ICallbackList<FileList>() {
                    @Override
                    public void onSuccess(FileList files) {
                        fileList[0] = files;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.complete(files);
                    }
                    @Override
                    public void onCompleteExceptionally(FileList files, java.util.List<Result> results) {
                        fileList[0] = files;
                        allocCheckResults[0] = results;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.complete(files);
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        throwables[0] = throwable;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.completeExceptionally(throwable);
                    }
                });

//                queryFinished.await();
//                listLock.unlock();
                future.join();

                if(throwables[0] != null){
                    throw new GeneralServiceException("", throwables[0]);
                }

                result.setFileList(fileList[0]);
                result.setResults(allocCheckResults[0]);
                return result;
            }
        });

        return task;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel 0";//getString(R.string.channel_name);
            String description = "This is channel 0";//(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CH_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = SnippetApp.getAppContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    /*
        ins: input stream for the content to upload
        name: the name in CDFS space
        parent: the target folder name of the upload
     */
    public Task upload(InputStream ins, String name, String parent) throws Exception {
        Upload upload = new Upload(mCDFS);
        Task task;
        final Throwable[] throwables = {null};

        NotificationCompat.Builder builder = new NotificationCompat.Builder(SnippetApp.getAppContext(), NOTIFICATION_CH_ID)
                .setSmallIcon(R.drawable.ic_baseline_cloud_circle_24)
                .setContentTitle("CDS-upload")
                .setContentText("Upload is in progress")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        createNotificationChannel();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(SnippetApp.getAppContext());
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());

        Log.d(TAG, "CDFS Service: Upload");

        mCDFS.requiresDriveClientNonNull();

        if(ins == null){throw new InvalidArgumentException("input stream for the file to pload is null"
                , new Throwable(""));}

        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public File call() throws CompletionException, GeneralServiceException {
                IUploadProgressListener listener = defaultUploadProgressListener;
                if(uploadProgressListener != null)
                    listener = uploadProgressListener;

                CompletableFuture<File> future =
                upload.upload(ins, name, parent, listener);

                future.exceptionally((ex)->{
                    Log.w(TAG, "Upload completed exceptionally. " + ex.getMessage());
                    ex.printStackTrace();
                    throwables[0] = new Throwable(ex);
                    return null;
                });

                /*
                    Here we use join() instead of get() because we don't need to take care interrupt
                    or timeout
                 */
                File f = future.join();

                if(throwables[0] != null){
                    throw new GeneralServiceException("", throwables[0]);
                }
                return f;
            }
        });

        return task;
    }

    public void setUploadProgressLisetener(IUploadProgressListener listener){
        uploadProgressListener = listener;
    }

    IUploadProgressListener defaultUploadProgressListener = new IUploadProgressListener() {
        @Override
        public void progressChanged(Upload uploader) {
            Log.d(TAG, "Upload progress " + uploader.getState());
        }
    };
    /*
        Download content of a file
     */
    public Task<OutputStream> download(String id) {
        Task task;
        Log.d(TAG, "Operation: download " + id);
        task = Tasks.call(mExecutor, new Callable<Object>() {
            @Override
            public OutputStream call() throws Exception {
                msTaskfinished = false;
                mCDFS.getDrives().values().iterator().next().getClient().download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                    //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                    @Override
                    //public void success(InputStream inputStream){
                    public void success(OutputStream os){
                        exitWait();
                        mStream = os;

                        Log.d(TAG, "download finished.");
//                        try {
//                            os.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }

                    @Override
                    public void failure(String ex) {
                        exitWait();
                        Log.w(TAG, "download failed" + ex.toString());
                    }
                });
                waitUntilFinished();
                if(mStream == null){ Log.w(TAG, "stream is null" );}
                return mStream;
            }
        });

        return task;
    }

    private void exitWait(){
        msTaskfinished = true;
    }

    private void waitUntilFinished() throws Exception{
        while(msTaskfinished == false) {
            Thread.sleep(100);
        }
    }
}
