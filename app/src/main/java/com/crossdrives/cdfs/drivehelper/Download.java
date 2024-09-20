package com.crossdrives.cdfs.drivehelper;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class Download {
    final String TAG = "CD.drivehelp.download";
    private IDriveClient mClient;

    public class Downloaded{
        public OutputStream os;
        public AllocationItem item;
    }

    public Download(IDriveClient client) {
        mClient = client;
    }

    public CompletableFuture<Downloaded> runAsync(AllocationItem ai){
        CompletableFuture<Downloaded> future = new CompletableFuture<>();

        mClient.download().buildRequest(ai.getItemId())
                .run(new IDownloadCallBack<MediaData>() {
                    //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                    @Override
                    public void success(MediaData mediaData) {
                        Downloaded downloaded = new Downloaded();
                        downloaded.item = ai;
                        downloaded.os = mediaData.getOs();
                        Log.d(TAG, "download finished. Seq: " + downloaded.item.getSequence());
                        future.complete(downloaded);
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
