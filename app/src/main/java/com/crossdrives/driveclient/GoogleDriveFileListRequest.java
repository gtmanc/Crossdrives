package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

public class GoogleDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "CD.GDC.GoogleDriveQueryRequest";
    GoogleDriveClient mClient;
    List<String> mOptions= new ArrayList<>();
    String mToken;
    /*
    100 is a feeling value. May need a fine tuning in the future.
     */
    final int PAGE_SIZE = 100;

    public GoogleDriveFileListRequest(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IFileListRequest select(final String value) {
        mOptions.add(value);
        return this;
    }

    @Override
    public IFileListRequest setNextPage(Object page) {
        mToken = (String) page;
        return this;
    }

    @Override
    public void run(ICallBack<FileList, Object> callback) {


        mClient.getGDriveHelper().
                queryFiles(mToken, PAGE_SIZE).
                addOnSuccessListener(new OnSuccessListener<FileList>() {
                    @Override
                    public void onSuccess(FileList fileList) {
                        List<File> f = fileList.getFiles();
                        Log.d(TAG, "Size of root items: " + f.size());

                        callback.success(fileList, fileList.getNextPageToken());
                    }
                }).
                addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Get root item failed: " + e.toString());
                        callback.failure(e.toString());
                }
        });
    }
}
