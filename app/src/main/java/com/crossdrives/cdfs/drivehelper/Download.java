package com.crossdrives.cdfs.drivehelper;

import android.util.Log;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;

import java.util.concurrent.CompletableFuture;

public class Download {
    final String TAG = "CD.drivehelp.download";
    private IDriveClient mClient;

    public Download(IDriveClient client) {
        mClient = client;
    }

    CompletableFuture<MediaData> runAsync(String id, int seq){
        CompletableFuture<MediaData> future = new CompletableFuture<>();

        mClient.download().buildRequest(id).setAdditionInt(seq)
                .run(new IDownloadCallBack<MediaData>() {
                    //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                    @Override
                    public void success(MediaData mediaData) {
                        Log.d(TAG, "download finished. Seq: " + mediaData.getAdditionInt());
                        future.complete(mediaData);

                    }

                    @Override
                    public void failure(String ex) {
                        Log.w(TAG, "download failed. " + ex.toString());
                        future.completeExceptionally(new Throwable(ex));
                    }
                });
        return future;
    }
}
