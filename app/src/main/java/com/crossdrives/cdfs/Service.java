package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.list.ICallbackList;
import com.crossdrives.cdfs.list.List;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Service implements IService{
    private static String TAG = "CD.Service";
    CDFS mCDFS;
    private FileList mFileList;
    private OutputStream mStream;

    public Service(CDFS cdfs) {
        mCDFS = cdfs;
    }

    private final Executor sExecutor = Executors.newSingleThreadExecutor();

    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;

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
    public void list(Object nextPage, IServiceCallback callback) throws MissingDriveClientException {
        List list = new List(mCDFS);
        FileList fileList;

        Log.d(TAG, "Service: list files. nextPage: " + nextPage);

        mCDFS.requiresDriveClientNonNull();

        list.list(null, new ICallbackList<FileList>() {
            @Override
            public void onCompleted(FileList fileList) {
                callback.onCompleted(fileList);
            }

            @Override
            public void onCompletedExceptionally(Throwable throwable) {
                callback.onCompletedExceptionally(throwable);
            }
        });
    }

    /*
        Download content of a file
     */
    public Task<OutputStream> download(String id) {
        Task task;
        Log.d(TAG, "Operation: download " + id);
        task = Tasks.call(sExecutor, new Callable<Object>() {
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
