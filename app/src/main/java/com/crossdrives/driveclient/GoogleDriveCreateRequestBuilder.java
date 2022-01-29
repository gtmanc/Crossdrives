package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public class GoogleDriveCreateRequestBuilder extends BaseRequestBuilder implements ICreateRequestBuilder{
    GoogleDriveClient mClient;
    public GoogleDriveCreateRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public ICreateRequest buildRequest(File metaData) {
        return new GoogleDriveCreateRequest(mClient, metaData);
    }
}
