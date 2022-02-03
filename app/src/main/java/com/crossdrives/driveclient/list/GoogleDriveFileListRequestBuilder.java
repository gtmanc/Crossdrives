package com.crossdrives.driveclient.list;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveFileListRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveFileListRequestBuilder(GoogleDriveClient Client) {
        mClient = Client;
    }

    public IFileListRequest buildRequest(){
        return new GoogleDriveFileListRequest(mClient);
    }
}
