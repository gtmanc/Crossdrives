package com.crossdrives.driveclient;

public class GoogleDriveRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveRequestBuilder(GoogleDriveClient Client) {
        mClient = Client;
    }

    public IFileListRequest buildRequest(){
        return new GoogleDriveFileListRequest(mClient);
    }
}
