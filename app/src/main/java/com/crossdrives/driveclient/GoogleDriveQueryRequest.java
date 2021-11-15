package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.crossdrives.DriveServiceHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.List;

public class GoogleDriveQueryRequest extends BaseRequest implements IQueryRequest{
    private String TAG = "CD.GDC.GoogleDriveQueryRequest";
    GoogleDriveClient mClient;

    public GoogleDriveQueryRequest(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IQueryRequest select() {
        return null;
    }

    @Override
    public void run(ICallBack<FileList> callback) {
        mClient.getGDriveHelper().
                queryFiles().
                addOnSuccessListener(new OnSuccessListener<FileList>() {
                    @Override
                    public void onSuccess(FileList fileList) {
                        List<File> f = fileList.getFiles();
                        Log.d(TAG, "Size of root items: " + f.size());
                    }
                }).
                addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Get root item failed: " + e.toString());

                }
        });
    }
}
