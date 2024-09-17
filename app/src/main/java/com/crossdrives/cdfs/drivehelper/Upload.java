package com.crossdrives.cdfs.drivehelper;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.upload.IUploadCallBack;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class Upload {
    final String TAG = "CD.drivehelp.upload";
    private IDriveClient mClient;

    static public class AdditionalData{
        public String string;
        public File localSlice;
        public Object object;
    }

    public class Uploaded{
        public com.crossdrives.driveclient.model.File file;
        public AdditionalData data;
    }
    private AdditionalData mData;

    public Upload(IDriveClient client) {

        mClient = client;
    }

    public Upload setAddtionalData(AdditionalData data){
        mData = data;
        return this;
    }

    public CompletableFuture<Uploaded> runAsync(
            com.crossdrives.driveclient.model.File file){
        CompletableFuture<Uploaded> future = new CompletableFuture<>();

        mClient.upload().buildRequest(file.getFile(), file.getOriginalLocalFile()).run(new IUploadCallBack() {
            @Override
            public void success(com.crossdrives.driveclient.model.File file) {
                Uploaded uploaded = new Uploaded();
                uploaded.file = file;
                uploaded.data = mData;
                future.complete(uploaded);
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
