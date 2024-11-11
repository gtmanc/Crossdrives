package com.crossdrives.driveclient.download;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.driveclient.download.IDownloadRequest;
import com.crossdrives.driveclient.download.IDownloadRequestBuilder;
import com.crossdrives.driveclient.download.OneDriveDownloadRequest;

public class OneDriveDownloadRequestBuilder extends BaseRequestBuilder implements IDownloadRequestBuilder {
    OneDriveClient mClient;
    public OneDriveDownloadRequestBuilder(OneDriveClient client) { mClient = client;}

    @Override
    public IDownloadRequest buildRequest(String id) {
        return new OneDriveDownloadRequest(mClient, id);
    }
}
