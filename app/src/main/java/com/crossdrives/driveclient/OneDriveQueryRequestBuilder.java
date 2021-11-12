package com.crossdrives.driveclient;

public class OneDriveQueryRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {

    public OneDriveQueryRequestBuilder(OneDriveClient client) {
        mClient = client;
    }

    public IQueryRequest buildRequest(){
        return new OneDriveQueryRequest(mClient);
    }
}
