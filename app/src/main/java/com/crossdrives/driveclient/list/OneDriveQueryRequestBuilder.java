package com.crossdrives.driveclient.list;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;

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
