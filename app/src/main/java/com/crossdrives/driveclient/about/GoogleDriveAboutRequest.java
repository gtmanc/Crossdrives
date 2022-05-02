package com.crossdrives.driveclient.about;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;

import java.io.IOException;
import java.util.concurrent.Callable;

public class GoogleDriveAboutRequest extends BaseRequest implements IAboutRequest {
    final String TAG = "CD.GoogleDriveAboutRequest";
    GoogleDriveClient mClient;
    public GoogleDriveAboutRequest(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public void run(IAboutCallBack callback) {
        Task<About> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<About>() {
            @Override
            public About call() throws Exception {
                About about = null;

                Drive.About.Get get = mClient.getGoogleDriveService().about().get();
                get.setFields("storageQuota");
                about = get.execute();
                return about;
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<About>() {
            @Override
            public void onSuccess(About about) {
                callback.success(about);
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.failure(e.getMessage());
            }
        });

    }
}
