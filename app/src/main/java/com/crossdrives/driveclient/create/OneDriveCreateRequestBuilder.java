package com.crossdrives.driveclient.create;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.driveclient.create.ICreateRequest;
import com.crossdrives.driveclient.create.ICreateRequestBuilder;
import com.crossdrives.driveclient.create.OneDriveCreateRequest;
import com.google.api.services.drive.model.File;

public class OneDriveCreateRequestBuilder extends BaseRequestBuilder implements ICreateRequestBuilder {
    OneDriveClient mClient;
    public OneDriveCreateRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    @Override
    public ICreateRequest buildRequest(File metaData) {
        return new OneDriveCreateRequest(mClient, metaData);
    }
}
