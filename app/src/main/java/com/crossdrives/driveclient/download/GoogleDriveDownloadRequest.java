package com.crossdrives.driveclient.download;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GoogleDriveDownloadRequest extends BaseRequest implements IDownloadRequest {
    final String TAG = "GDC.GoogleDriveDownloadRequest";
    GoogleDriveClient mClient;
    String mID;
    public GoogleDriveDownloadRequest(GoogleDriveClient client, String id) { mClient = client; mID = id;
    }

    @Override
    //public void run(IDownloadCallBack<InputStream> callback) {
    public void run(IDownloadCallBack<OutputStream> callback) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream outputStream = bos;

        //Log.d(TAG, "download: " + mID);
        String ex = null;
        try {
            mClient.getGoogleDriveService().files().get(mID)
                    .executeMediaAndDownloadTo(bos);

        } catch (IOException e) {
            bos = null;
            ex = e.toString();
        }

        //Log.d(TAG, new String(bos.toByteArray()));
        if(ex == null) {
            callback.success(bos);
        }else{
            callback.failure(ex);
        }
    }
}
