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
    public IUpdateRequest buildRequest(String fileID, File file) {
        return new OneDriveUpdateRequest(mClient, fileID, file);
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, File file, AbstractInputStreamContent mediaContent) {
        return new OneDriveUpdateRequest(mClient, fileID, file, mediaContent);
    }
}
