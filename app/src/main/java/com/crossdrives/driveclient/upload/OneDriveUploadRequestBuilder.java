package com.crossdrives.driveclient.upload;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.File;

public class OneDriveUploadRequestBuilder extends BaseRequestBuilder implements IUploadRequestBuilder {
    OneDriveClient mClient;
    public OneDriveUploadRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    @Override
    public IUploadRequest buildRequest(File metadata, java.io.File path) {
        return new OneDriveUploadRequest(mClient, metadata, path);
    }
}
