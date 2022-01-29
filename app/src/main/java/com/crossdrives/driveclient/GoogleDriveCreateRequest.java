package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.File;

import java.io.IOException;

public class GoogleDriveCreateRequest extends BaseRequest implements ICreateRequest{
    final String TAG = "CD.GoogleDriveCreateRequest";
    GoogleDriveClient mClient;
    File mMetaData;
    public GoogleDriveCreateRequest(GoogleDriveClient client, File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(ICreateCallBack<File> callback) {
        try {
            Log.d(TAG, "create file: " + mMetaData.getName());
            File file = mClient.getGoogleDriveService().files().create(mMetaData)
                    .setFields("id")
                    .execute();
            callback.success(file);
        } catch (IOException e) {
            Log.w(TAG, "IOException: " + e.getMessage());
            callback.failure(e.getMessage());
        }
        //System.out.println("Folder ID: " + file.getId());
    }
}
