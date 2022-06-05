package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveUpdateRequestBuilder extends BaseRequestBuilder implements IUpdateRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveUpdateRequestBuilder(GoogleDriveClient client) {mClient = client;
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, String op) {
        return new GoogleDriveUpdateRequest(mClient, fileID, op);
    }
}
