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
    MetaData mMetaData;
    AbstractInputStreamContent mMediaContent;

    //public List<String> oldParents;

    public GoogleDriveUpdateRequest(GoogleDriveClient mClient, String fileID, MetaData metaData) {
        this.mClient = mClient;
        mfileID = fileID;
        mMetaData = metaData;
    }

    public GoogleDriveUpdateRequest(GoogleDriveClient mClient, String fileID, MetaData metaData,
                                    AbstractInputStreamContent mediaContent) {
        this.mClient = mClient;
        mfileID = fileID;
        mMetaData = metaData;
        mMediaContent = mediaContent;
    }

//    @Override
//    public IUpdateRequest parentsToRemoved(List<String> parents) {
//        oldParents = parents;
//        return this;
//    }

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
                com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();

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
                    update = files.update(mfileID, file, mMediaContent);
                }else{
                    update = files.update(mfileID, file);
                }

                // The parents must be null because it's not allowed to write directly. Otherwise, exception with 403 Forbidden with reason
                // fieldNotWritable is thrown.
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
        List<String> list;

        if(mMetaData == null) {return update;}

        if(mMetaData.getParents() == null) {return update;}

        list = mMetaData.getParents().toRemove;
        if( list != null) {
            StringBuilder oldParents = new StringBuilder();
            for (String parent : list) {
                oldParents.append(parent);
                oldParents.append(',');
            }
            Log.d(TAG, "oldParents to remove: " + oldParents.toString());
            update.setRemoveParents(oldParents.toString());
        }

        list = mMetaData.getParents().toAdd;
        if(list != null) {
            StringBuilder newParents = new StringBuilder();
            for (String parent : list) {
                newParents.append(parent);
                newParents.append(',');
            }
            Log.d(TAG, "newParents to add: " + newParents.toString());
            update.setAddParents(newParents.toString());
        }

        return update;
    }
}
