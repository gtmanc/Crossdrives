package com.crossdrives.cdfs.drivehelper;

import android.util.Log;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.model.MediaData;
import com.crossdrives.driveclient.upload.IUploadCallBack;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class Upload {
    final String TAG = "CD.drivehelp.download";
    private IDriveClient mClient;

    public Upload(IDriveClient client) {
        mClient = client;
    }

    public CompletableFuture<com.crossdrives.driveclient.model.File> runAsync(
            com.google.api.services.drive.model.File fileMetadata, File localFile){
        CompletableFuture<com.crossdrives.driveclient.model.File> future = new CompletableFuture<>();
        mClient.upload().buildRequest(fileMetadata, localFile).run(new IUploadCallBack() {
            @Override
            public void success(com.crossdrives.driveclient.model.File file) {
                future.complete(file);
            }

            @Override
            public void failure(String ex, File originalFile) {
                Log.w(TAG, "upload failed. " + ex.toString());
                future.completeExceptionally(new Throwable(ex));
            }
        });
        return future;
    }
}
