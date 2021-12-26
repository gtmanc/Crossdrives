package com.crossdrives.driveclient;

public class GoogleDriveFileLitRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder{
    GoogleDriveClient mClient;

    public GoogleDriveFileLitRequestBuilder(GoogleDriveClient Client) {
        mClient = Client;
    }

    public IFileListRequest buildRequest(){
        return new GoogleDriveFileListRequest(mClient);
    }
}
