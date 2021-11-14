package com.crossdrives.driveclient;

public class GoogleDriveRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }

    public IQueryRequest buildRequest(){
        return new GoogleDriveQueryRequest(mClient);
    }
}
