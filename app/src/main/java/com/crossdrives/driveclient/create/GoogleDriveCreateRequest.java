package com.crossdrives.driveclient.create;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.concurrent.Callable;

public class GoogleDriveCreateRequest extends BaseRequest implements ICreateRequest {
    final String TAG = "CD.GoogleDriveCreateRequest";
    GoogleDriveClient mClient;
    File mMetaData;
    public GoogleDriveCreateRequest(GoogleDriveClient client, File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(ICreateCallBack<File> callback) {
        Task<File> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<File>() {
            @Override
            public File call() throws Exception {
                File file = null;
                Log.d(TAG, "create file: " + mMetaData.getName());
                Log.d(TAG, "mineType: " + mMetaData.getMimeType());
                file = mClient.getGoogleDriveService().files().create(mMetaData)
                            .setFields("id")
                            .execute();
                //System.out.println("Folder ID: " + file.getId());
                return file;
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                callback.success(file);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, e.getMessage());
                callback.failure(e.getMessage());
            }
        });
    }
}
