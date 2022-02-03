package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.google.api.services.drive.model.File;

import java.io.IOException;

public class GoogleDriveDeleteRequest extends BaseRequest implements IDeleteRequest {
    GoogleDriveClient mClient;
    File mMetaData;
    public GoogleDriveDeleteRequest(GoogleDriveClient client, File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(IDeleteCallBack<File> callback) {
        try {
            mClient.getGoogleDriveService().files().delete(mMetaData.getId()).execute();
            callback.success(mMetaData);
        } catch (IOException e) {
            //e.printStackTrace();
            callback.failure(e.getMessage());
        }
    }
}
