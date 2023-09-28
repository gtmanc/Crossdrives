package com.crossdrives.driveclient.update;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.driveclient.model.MetaData;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ContentRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GoogleDriveUpdateRequest extends BaseRequest implements IUpdateRequest {
    final String TAG = "CD.GoogleDriveUpdateRequest";
    GoogleDriveClient mClient;
    String mfileID;
    com.google.api.services.drive.model.File mMetaData;
    AbstractInputStreamContent mMediaContent;

    public GoogleDriveUpdateRequest(GoogleDriveClient mClient, String fileID, com.google.api.services.drive.model.File file) {
        this.mClient = mClient;
        mfileID = fileID;
        mMetaData = file;
    }

    public GoogleDriveUpdateRequest(GoogleDriveClient mClient, String fileID, com.google.api.services.drive.model.File file,
                                    AbstractInputStreamContent mediaContent) {
        this.mClient = mClient;
        mfileID = fileID;
        mMetaData = file;
        mMediaContent = mediaContent;
    }

    @Override
    public void run(IUpdateCallBack callback) {
        Task<File> task;
//        com.google.api.services.drive.model.File content = new com.google.api.services.drive.model.File();
//        List<ContentRestriction> restrictions = new ArrayList<>();
//
//        ContentRestriction restriction = new ContentRestriction();
//        restriction = restriction.setReadOnly(new Boolean(true));
//        if(mReason != null){
//            restriction = restriction.setReason(mReason);
//        }
//
//        restrictions.add(restriction);
//        //content.setContentRestrictions(restrictions);
//        content.setDescription("test description");
        Tasks.call(mClient.getExecutor(), new Callable<com.google.api.services.drive.model.File>() {
            @Override
            public com.google.api.services.drive.model.File call() throws Exception {
                Drive.Files files;
                Drive.Files.Update update;
                com.google.api.services.drive.model.File file;

                files = mClient.getGoogleDriveService().files();

                if(mMediaContent != null){
                    update = files.update(mfileID, mMetaData, mMediaContent);
                }else{
                    update = files.update(mfileID, mMetaData);
                }


                file = update.setFields("id, name, contentRestrictions")
                //.setFields("id, name")
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
                Log.w(TAG, e.getMessage());
                callback.failure(e.getMessage());
            }
        });
    }

    Drive.Files.Update updateParents(){

    }

}
