package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public class OneDriveDeleteRequestBuilder extends BaseRequestBuilder implements IDeleteRequestBuilder{
    OneDriveClient mClient;
    public OneDriveDeleteRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    @Override
    public IDeleteRequest buildRequest(File metaData) {
        return new OneDriveDeleteRequest(mClient, metaData);
    }
}
