package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.driveclient.ICallBack;
import com.crossdrives.driveclient.IDriveClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

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
    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private static boolean msTaskfinished = false;

    public CDFS() {

    }

    static public void addClient(IDriveClient client){
        sClient.add(client);
    }

    /*
    Operation: get file list
     */
    static public Task<FileList> list(Object nextPage){
        Task task;
        Log.d(TAG, "Operation: list files");
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public FileList call() throws Exception {
                msTaskfinished = false;
                /*
                    Drive client test only. Always use index 0 (i.e first one added)
                 */
                sClient.get(0).list().buildRequest()
                        .setNextPage(nextPage)
                        .setPageSize(10)
                        .filter("mimeType = application/vnd.google-apps.folder")
                        .run(new ICallBack<FileList, Object>() {
                    @Override
                    public void success(FileList fileList, Object o) {
                        setToWait();
                        mFileList = fileList;
                        Log.d(TAG, "list finished");
                    }

                    @Override
                    public void failure(String ex) {
                        setToWait();
                        Log.w(TAG, "list finished with failure!");
                    }
                });
                waitUntilFinished();
                return mFileList;
            }
        });

        return task;
    }

    private static void setToWait(){
        msTaskfinished = true;
    }

    private static void waitUntilFinished() throws Exception{
        while(msTaskfinished == false) {
            Thread.sleep(100);
        }
    }

}
