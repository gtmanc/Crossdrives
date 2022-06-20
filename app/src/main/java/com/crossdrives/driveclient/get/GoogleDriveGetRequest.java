package com.crossdrives.driveclient.get;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.util.concurrent.Callable;

public class GoogleDriveGetRequest extends BaseRequest implements IGetRequest {
    GoogleDriveClient mClient;
    String mFileID;

    public GoogleDriveGetRequest(GoogleDriveClient mClient, String fileID) {
        this.mClient = mClient;
        mFileID = fileID;
    }

    @Override
    public void run(IGetCallBack callback) {

        Tasks.call(mClient.getExecutor(), new Callable<File>() {
            @Override
            public File call() throws Exception {
                File file;
                file = mClient.getGoogleDriveService().files().get(mFileID).setFields("id, name, contentRestrictions").execute();
                return file;
            }
        }).addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                callback.success(file);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.failure(e.getMessage());
            }
        });
    }
}
