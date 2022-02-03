package com.crossdrives.driveclient.download;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveDownloadRequestBuilder extends BaseRequest implements IDownloadRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveDownloadRequestBuilder(GoogleDriveClient client) { mClient = client;
    }

    @Override
    public IDownloadRequest buildRequest(String id) {
        return new GoogleDriveDownloadRequest(mClient, id);
    }
}
