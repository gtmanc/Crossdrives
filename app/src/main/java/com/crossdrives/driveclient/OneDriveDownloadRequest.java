package com.crossdrives.driveclient;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class OneDriveDownloadRequest extends BaseRequest implements IDownloadRequest{
    final String TAG = "ODC.OneDriveDownloadRequest";
    OneDriveClient mClient;
    String mID;
    String mUrl = "/me/drive/items/{item-id}/content";
    public OneDriveDownloadRequest(OneDriveClient client, String id) { mClient = client; mID = id;   }

    @Override
    public void run(IDownloadCallBack<InputStream> callback) {
        mUrl = mUrl.replace("{item-id}", mID);
        Log.d(TAG, "Request Url: " + mUrl);
        mClient.getGraphServiceClient().customRequest(mUrl, InputStream.class)
                .buildRequest()
                .getAsync().thenAccept(stream -> {
                    if(stream == null){ Log.w(TAG, "stream is null" );}
                        callback.success((InputStream) stream);

        })
                .exceptionally(ex->{
                    Log.w(TAG, "download failed: " + ex.toString());
                    callback.failure((String)ex);
                    return null;
                });
    }
}
