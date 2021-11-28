package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GoogleDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "CD.GDC.GoogleDriveQueryRequest";
    GoogleDriveClient mClient;
    List<String> mOptions= new ArrayList<>();
    String mToken;
    /*
    100 is a feeling value. May need a fine tuning in the future.
     */
    final int PAGE_SIZE = 100;
    int mPageSize = PAGE_SIZE;

    public GoogleDriveFileListRequest(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IFileListRequest select(final String value) {
        mOptions.add(value);
        return this;
    }

    @Override
    public IFileListRequest filter(String value) {
        return null;
    }

    @Override
    public IFileListRequest setNextPage(Object page) {
        mToken = (String) page;
        return this;
    }

    @Override
    public IFileListRequest setPageSize(int size) {
        mPageSize = size;
        return this;
    }

    @Override
    public void run(ICallBack<FileList, Object> callback) {
        Task<FileList> task;
//        mClient.getGDriveHelper().
//                queryFiles(mToken, mPageSize).
//                addOnSuccessListener(new OnSuccessListener<FileList>() {
//                    @Override
//                    public void onSuccess(FileList fileList) {
//                        List<File> f = fileList.getFiles();
//                        Log.d(TAG, "Size of root items: " + f.size());
//
//                        callback.success(fileList, fileList.getNextPageToken());
//                    }
//                }).
//                addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.d(TAG, "Get root item failed: " + e.toString());
//                        callback.failure(e.toString());
//                }
//        });

        task = Tasks.call(mClient.getExecutor(), new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                FileList files = null;
                files = mClient.getGoogleDriveService().files().list()
                        .setQ("name contains 'cdfs'" + " and " + "mimeType ='application/vnd.google-apps.folder'")
                        //.setQ("mimeType ='application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(mToken)
                        //set to a small number can be used for test of loading more data in UI handling
                        .setPageSize(mPageSize)
                        .execute();
                return files;
            }
        });

        task.addOnSuccessListener(new OnSuccessListener<FileList>() {
            @Override
            public void onSuccess(FileList fileList) {
                List<File> f = fileList.getFiles();
                Log.d(TAG, "Size of root items: " + f.size());

                callback.success(fileList, fileList.getNextPageToken());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Get root item failed: " + e.toString());
                callback.failure(e.toString());
            }
        });
    }
}
