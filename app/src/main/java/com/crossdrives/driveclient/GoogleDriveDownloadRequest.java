package com.crossdrives.driveclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class GoogleDriveDownloadRequest extends BaseRequest implements IDownloadRequest{
    GoogleDriveClient mClient;
    String mID;
    public GoogleDriveDownloadRequest(GoogleDriveClient client, String id) { mClient = client; mID = id;
    }

    @Override
    //public void run(IDownloadCallBack<InputStream> callback) {
    public void run(IDownloadCallBack<OutputStream> callback) {
        OutputStream outputStream = new ByteArrayOutputStream();
        String ex = null;
        try {
            mClient.getGoogleDriveService().files().get(mID)
                    .executeMediaAndDownloadTo(outputStream);

        } catch (IOException e) {
            outputStream = null;
            ex = e.toString();
        }

        if(ex == null) {
            callback.success(outputStream);
        }else{
            callback.failure(ex);
        }
    }

       //https://stackoverflow.com/questions/5778658/how-to-convert-outputstream-to-inputstream
    InputStream covertOutputStreamToInputStream(OutputStream os){
        return null;

    }
}
