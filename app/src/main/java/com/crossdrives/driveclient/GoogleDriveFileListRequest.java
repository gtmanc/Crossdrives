package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;
import java.util.concurrent.Callable;

public class GoogleDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "CD.GDC.GoogleDriveQueryRequest";
    GoogleDriveClient mClient;
    private String mfilterClause, mSelectClause;
    String mToken;
    int mPageSize = 0;  //0 means 'not yet assigned'

    public GoogleDriveFileListRequest(GoogleDriveClient client) {
        mClient = client;
    }

    /*
        TODO: Not yet implemented
     */
    @Override
    public IFileListRequest select(final String value) {

        return this;
    }

    @Override
    public IFileListRequest filter(String value) {
        mfilterClause = value;
        return this;
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
    public void run(IFileListCallBack<FileList, Object> callback) {
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

                Drive.Files.List list = mClient.getGoogleDriveService().files().list()
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(mToken);  //this is ok. null means 1st page;

                //apply filter?
                if(mfilterClause != null) {
                    list = list.setQ(mfilterClause);
                }

                //apply page Size? Google error message says: "Values must be within the range: [1, 1000]"
                if(mPageSize != 0) {
                    //set to a small number can be used for test of loading more data in UI handling
                     list = list.setPageSize(mPageSize);
                }

                files = list.execute();

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
