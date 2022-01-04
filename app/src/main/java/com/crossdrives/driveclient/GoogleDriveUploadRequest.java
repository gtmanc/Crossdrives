package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.concurrent.Callable;

public class GoogleDriveUploadRequest extends BaseRequest implements IUploadRequest{
    final String TAG = "GDC.GoogleDriveUploadRequest";
    GoogleDriveClient mClient;
    File mMetadata;
    java.io.File mPath;
    String mType = "text/plain";

    public GoogleDriveUploadRequest(GoogleDriveClient client, File metadata, java.io.File path) {
        mClient = client;
        mMetadata = metadata;
        mPath = path;
    }

    @Override
    public void type(String type) {
        //check valid? "image/jpeg"
        mType = type;
    }

    @Override
    public void run(IUploadCallBack callback){
        Task<File> task;
//        File fileMetadata = new File();
//        fileMetadata.setName("Allocation.cdfs");
//        java.io.File filePath = new java.io.File("files/photo.jpg");
        task = Tasks.call(mClient.getExecutor(), new Callable<File>() {
            @Override
            public File call() throws Exception {
                File file;
                FileContent mediaContent = new FileContent(mType, mPath);
                //Log.d(TAG, "Path: " + mPath);
                try {
                    file = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                } catch (IOException e) {
                    Log.w(TAG, "IO exception: " + e.toString());
                    file = null;
                }

                //System.out.println("File ID: " + file.getId());
                return file;
            }
        });

        task.addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                //call back
                if(file != null){
                    callback.success(file);
                }
                else{
                    callback.failure("Unknown failure. Could be IO exception in drive client request");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //call back
                callback.failure(e.toString());
            }
        });

    }
}
