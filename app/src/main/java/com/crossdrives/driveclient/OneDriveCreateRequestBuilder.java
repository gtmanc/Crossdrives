package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public class OneDriveCreateRequestBuilder extends BaseRequestBuilder implements ICreateRequestBuilder{
    OneDriveClient mClient;
    public OneDriveCreateRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    @Override
    public ICreateRequest buildRequest(File metaData) {
        return new OneDriveCreateRequest(mClient, metaData);
    }
}
