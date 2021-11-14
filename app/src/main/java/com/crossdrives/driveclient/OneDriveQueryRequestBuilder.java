package com.crossdrives.driveclient;

public class OneDriveQueryRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {
    OneDriveClient mClient;

    public OneDriveQueryRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    public IQueryRequest buildRequest(){
        return new OneDriveQueryRequest(mClient);
    }

    public OneDriveClient getOneDriveClient(){
        return mClient;
    }
}
