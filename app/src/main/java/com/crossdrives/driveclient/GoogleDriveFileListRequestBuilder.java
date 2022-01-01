package com.crossdrives.driveclient;

public class GoogleDriveFileListRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveFileListRequestBuilder(GoogleDriveClient Client) {
        mClient = Client;
    }

    public IFileListRequest buildRequest(){
        return new GoogleDriveFileListRequest(mClient);
    }
}
