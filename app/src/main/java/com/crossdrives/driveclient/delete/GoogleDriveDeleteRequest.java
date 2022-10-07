package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;

import java.io.IOException;

public class GoogleDriveDeleteRequest extends BaseRequest implements IDeleteRequest {
    GoogleDriveClient mClient;
    com.crossdrives.driveclient.model.File mMetaData;
    public GoogleDriveDeleteRequest(GoogleDriveClient client, com.crossdrives.driveclient.model.File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(IDeleteCallBack<com.crossdrives.driveclient.model.File> callback) {
        try {
            mClient.getGoogleDriveService().files().delete(mMetaData.getFile().getId()).execute();
            com.crossdrives.driveclient.model.File f = new com.crossdrives.driveclient.model.File();
            f.setString(mMetaData.getString());
            f.setInteger(mMetaData.getInteger());
            callback.success(f);
        } catch (IOException e) {
            //e.printStackTrace();
            callback.failure(e.getMessage());
        }
    }
}
