package com.crossdrives.driveclient;

public class OneDriveQueryRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {
    OneDriveClient mClient;

    public OneDriveQueryRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    public IFileListRequest buildRequest(){
        return new OneDriveFileListRequest(mClient);
    }

    public OneDriveClient getOneDriveClient(){
        return mClient;
    }
}
