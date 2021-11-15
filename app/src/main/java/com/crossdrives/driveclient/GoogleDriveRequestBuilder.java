package com.crossdrives.driveclient;

import com.example.crossdrives.DriveServiceHelper;

public class GoogleDriveRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveRequestBuilder(GoogleDriveClient Client) {
        mClient = Client;
    }

    public IQueryRequest buildRequest(){
        return new GoogleDriveQueryRequest(mClient);
    }
}
