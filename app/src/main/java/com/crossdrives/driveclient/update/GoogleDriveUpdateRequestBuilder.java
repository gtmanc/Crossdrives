package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.File;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;

public class GoogleDriveUpdateRequestBuilder extends BaseRequestBuilder implements IUpdateRequestBuilder {
    GoogleDriveClient mClient;

    public GoogleDriveUpdateRequestBuilder(GoogleDriveClient client) {mClient = client;
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, com.google.api.services.drive.model.File metaData) {
        return new GoogleDriveUpdateRequest(mClient, fileID, metaData);
    }

    @Override
    public IUpdateRequest buildRequest(String fileID, com.google.api.services.drive.model.File metaData, AbstractInputStreamContent mediaContent) {
        return new GoogleDriveUpdateRequest(mClient, fileID, metaData, mediaContent);
    }
}
