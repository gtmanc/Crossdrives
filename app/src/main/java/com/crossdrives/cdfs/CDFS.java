package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.driveclient.IDownloadCallBack;
import com.crossdrives.driveclient.IFileListCallBack;
import com.crossdrives.driveclient.IDriveClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CDFS {
    private static String TAG = "CDFS.CDFS";
    static List<IDriveClient> sClient = new ArrayList<>();
    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    private static FileList mFileList;
    private static OutputStream mStream;

    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private static boolean msTaskfinished = false;

    public CDFS() {

    }

    static public int addClient(IDriveClient client){
        Log.d(TAG, "Add client!");
        sClient.add(client);
        return getClient(client);
    }

    static public void removeClient(int i){
        sClient.remove(i);
    }
    static public IDriveClient getClient(int i){
        return sClient.get(i);
    }
    static public int getClient(IDriveClient client){
        return sClient.indexOf(client);
    }

    /*
    Operation: get file list
     */
    static public Task<FileList> list(Object nextPage){
        Task task;
        Log.d(TAG, "Operation: list files. nextPage: " + nextPage);
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public FileList call() throws Exception {
                msTaskfinished = false;
                /*
                    Drive client test only. Always use index 0 (i.e first one added)
                 */
                sClient.get(0).list().buildRequest()
                        .setNextPage(nextPage)
                        .setPageSize(0) //0 means no page size is applied
                        //.filter("mimeType = application/vnd.google-apps.folder and name contains 'cdfs'")
                        //.filter("mimeType = application/vnd.google-apps.folder")
                        .filter(null)   //null means no filter will be applied
                        .run(new IFileListCallBack<FileList, Object>() {
                    @Override
                    public void success(FileList fileList, Object o) {
                        exitWait();
                        mFileList = fileList;
                        Log.d(TAG, "list finished");
                    }

                    @Override
                    public void failure(String ex) {
                        exitWait();
                        Log.w(TAG, "list finished with failure!");
                    }
                });
                waitUntilFinished();
                return mFileList;
            }
        });

        return task;
    }

    /*
        Operation download content of a file
     */
    static public Task<OutputStream> download(String id) {
        Task task;
        Log.d(TAG, "Operation: download " + id);
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public OutputStream call() throws Exception {
                msTaskfinished = false;
                //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<InputStream>() {
                sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
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

    private static void exitWait(){
        msTaskfinished = true;
    }

    private static void waitUntilFinished() throws Exception{
        while(msTaskfinished == false) {
            Thread.sleep(100);
        }
    }

}