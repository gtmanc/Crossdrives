package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public class GoogleDriveDeleteRequestBuilder extends BaseRequest implements IDeleteRequestBuilder{
    GoogleDriveClient mClient;
    public GoogleDriveDeleteRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IDeleteRequest buildRequest(File metaData) {
        return new GoogleDriveDeleteRequest(mClient, metaData);
    }
}
