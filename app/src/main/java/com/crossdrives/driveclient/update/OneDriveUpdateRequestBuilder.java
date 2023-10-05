package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.model.File;

public class OneDriveUpdateRequestBuilder extends BaseRequestBuilder implements IUpdateRequestBuilder {
    OneDriveClient mClient;

    public OneDriveUpdateRequestBuilder(OneDriveClient client) {
        this.mClient = client;
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, MetaData metaData) {
        return new OneDriveUpdateRequest(mClient, fileID, metaData);
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, MetaData metaData, AbstractInputStreamContent mediaContent) {
        return new OneDriveUpdateRequest(mClient, fileID, metaData, mediaContent);
    }
}
