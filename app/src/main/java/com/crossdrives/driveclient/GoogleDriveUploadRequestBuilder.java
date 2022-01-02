package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public class GoogleDriveUploadRequestBuilder extends BaseRequestBuilder implements IUploadRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveUploadRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }

    public IUploadRequest buildRequest(File metadata, java.io.File path){
        return new GoogleDriveUploadRequest(mClient, metadata, path);
    }
}
