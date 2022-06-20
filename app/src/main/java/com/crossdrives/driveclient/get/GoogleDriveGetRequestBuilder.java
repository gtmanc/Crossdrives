package com.crossdrives.driveclient.get;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveGetRequestBuilder extends BaseRequestBuilder implements IGetRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveGetRequestBuilder(GoogleDriveClient mClient) {
        this.mClient = mClient;
    }

    @Override
    public IGetRequest buildRequest(String fileID) {
        return new GoogleDriveGetRequest(mClient, fileID);
    }
}
