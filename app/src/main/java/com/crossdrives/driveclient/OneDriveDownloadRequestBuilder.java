package com.crossdrives.driveclient;

public class OneDriveDownloadRequestBuilder extends BaseRequestBuilder implements IDownloadRequestBuilder {
    OneDriveClient mClient;
    public OneDriveDownloadRequestBuilder(OneDriveClient client) { mClient = client;}

    @Override
    public IDownloadRequest buildRequest(String id) {
        return new OneDriveDownloadRequest(mClient, id);
    }
}
