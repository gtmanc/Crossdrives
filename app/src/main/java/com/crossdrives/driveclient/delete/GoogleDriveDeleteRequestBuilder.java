package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;

public class GoogleDriveDeleteRequestBuilder extends BaseRequest implements IDeleteRequestBuilder {
    GoogleDriveClient mClient;
    public GoogleDriveDeleteRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IDeleteRequest buildRequest(File metaData) {
        return new GoogleDriveDeleteRequest(mClient, metaData);
    }
}
