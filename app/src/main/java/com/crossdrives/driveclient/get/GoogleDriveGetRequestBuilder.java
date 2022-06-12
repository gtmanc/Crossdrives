package com.crossdrives.driveclient.get;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveGetRequestBuilder extends BaseRequestBuilder implements IGoogleDriveGetRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveGetRequestBuilder(GoogleDriveClient mClient) {
        this.mClient = mClient;
    }

    @Override
    public IGoogleDriveGetRequest buildRequest() {
        return null;
    }
}
