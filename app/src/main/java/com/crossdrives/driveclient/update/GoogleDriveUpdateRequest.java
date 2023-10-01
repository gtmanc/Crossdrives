package com.crossdrives.driveclient.update;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;

import java.util.List;
import java.util.concurrent.Callable;

public class GoogleDriveUpdateRequest extends BaseRequest implements IUpdateRequest {
    final String TAG = "CD.GoogleDriveUpdateRequest";
    GoogleDriveClient mClient;
    String mfileID;
    com.google.api.services.drive.model.File mMetaData;
    AbstractInputStreamContent mMediaContent;

    public List<String> oldParents;

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
    public IUpdateRequest parentsToRemoved(List<String> parents) {
        oldParents = parents;
        return this;
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

                if(mMediaContent == null){
                    Log.d(TAG, "Update meta data only.");
                }
                if(mMetaData == null){
                    Log.d(TAG, "Update content only.");
                }

                if(mMediaContent != null){
                    // Exception will be thrown if the 3rd parameter (mMediaContent) is null. This is not
                    // the java doc tells...
                    update = files.update(mfileID, mMetaData, mMediaContent);
                }else{
                    update = files.update(mfileID, mMetaData);
                }

                update = updateParentsIfNeed(update);

                file = update.setFields("id, name, parents, contentRestrictions")
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
                Log.w(TAG, "onFailure!");
                //Log.w(TAG, e.getMessage());
                callback.failure(e.toString());
            }
        });
    }

    Drive.Files.Update updateParentsIfNeed(Drive.Files.Update update){

        if(oldParents != null) {
            StringBuilder oldParents = new StringBuilder();
            for (String parent : this.oldParents) {
                oldParents.append(parent);
                oldParents.append(',');
            }
            Log.d(TAG, "oldParents: " + oldParents.toString());
            update.setRemoveParents(oldParents.toString());
        }

        if(mMetaData == null) {return update;}

        if(mMetaData.getParents() != null) {
            StringBuilder newParents = new StringBuilder();
            for (String parent : mMetaData.getParents()) {
                newParents.append(parent);
                newParents.append(',');
            }
            Log.d(TAG, "newParents: " + newParents.toString());
            // We have to remove the parents from the metadata. Otherwise, exception with 403 Forbidden with reason
            // fieldNotWritable is thrown.
            mMetaData.setParents(null);
            update.setAddParents(newParents.toString());
        }

        return update;
    }
}
