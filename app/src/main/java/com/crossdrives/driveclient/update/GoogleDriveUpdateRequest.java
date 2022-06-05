package com.crossdrives.driveclient.update;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.ContentRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GoogleDriveUpdateRequest extends BaseRequest implements IUpdateRequest {
    GoogleDriveClient mClient;
    String mfileID;
    String mOperation;
    String mReason;

    public GoogleDriveUpdateRequest(GoogleDriveClient mClient, String fileID, String op) {
        this.mClient = mClient;
        mfileID = fileID;
        mOperation = op;
    }


    @Override
    public void Reason(String reason) {
        mReason = reason;
    }

    @Override
    public void run(IUpdateCallBack callback) {
        Task<File> task;
        com.google.api.services.drive.model.File content = new com.google.api.services.drive.model.File();
        List<ContentRestriction> restrictions = new ArrayList<>();

        ContentRestriction restriction = new ContentRestriction();
        restriction = restriction.setReadOnly(new Boolean(true));
        if(mReason != null){
            restriction = restriction.setReason(mReason);
        }

        restrictions.add(restriction);
        content.setContentRestrictions(restrictions);

        Tasks.call(mClient.getExecutor(), new Callable<com.google.api.services.drive.model.File>() {
            @Override
            public com.google.api.services.drive.model.File call() throws Exception {
                com.google.api.services.drive.model.File file = mClient.getGoogleDriveService().files().update(mfileID, content)
                        .setFields("id, name, readOnly, reason")
                        .execute();
                //System.out.println("Folder ID: " + file.getId());
                return file;
            }
        }).addOnSuccessListener(new OnSuccessListener<com.google.api.services.drive.model.File>() {
                    @Override
                    public void onSuccess(com.google.api.services.drive.model.File file) {
                        callback.success(file);
                    }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.success(e.getMessage());
            }
        });
    }
}
