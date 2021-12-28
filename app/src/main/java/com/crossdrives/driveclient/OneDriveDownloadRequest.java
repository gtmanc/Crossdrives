package com.crossdrives.driveclient;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OneDriveDownloadRequest extends BaseRequest implements IDownloadRequest{
    final String TAG = "ODC.OneDriveDownloadRequest";
    OneDriveClient mClient;
    String mID;
    //String mUrl = "/me/drive/items/{item-id}/content";
    String mUrl = "/me/drive/root/children/Allocation.txt/content";
    public OneDriveDownloadRequest(OneDriveClient client, String id) { mClient = client; mID = id;   }

    @Override
    //public void run(IDownloadCallBack<InputStream> callback) {
    public void run(IDownloadCallBack<OutputStream> callback) {
        mUrl = mUrl.replace("{item-id}", mID);
        Log.d(TAG, "Request Url: " + mUrl);
        /*
            TODO: clarification
            null is received if OutputStream.class is specified. Not yet clear whether
            Graph support responseType OutputStream. i.e. Changing permission scope to Files.ReadWrite.All
            doesn't help
         */
        mClient.getGraphServiceClient().customRequest(mUrl, InputStream.class)
        //mClient.getGraphServiceClient().customRequest(mUrl, OutputStream.class)
                .buildRequest()
                .getAsync().thenAccept(in -> {
                    OutputStream out = new ByteArrayOutputStream();
                    if(in == null){ Log.w(TAG, "In stream is null" );}
                        //callback.success((OutputStream) stream);
                    try {
                        IOUtils.copy((InputStream) in, out);
                    } catch (IOException e) {
                        Log.w(TAG, "transfer input to output stream not work!" );
                    }
                    callback.success(out);
        })
                .exceptionally(ex->{
                    Log.w(TAG, "download failed: " + ex.toString());
                    callback.failure((String)ex);
                    return null;
                });
    }
}
