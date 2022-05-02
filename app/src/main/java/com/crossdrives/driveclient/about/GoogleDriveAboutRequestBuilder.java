package com.crossdrives.driveclient.about;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.IRequestBuilder;
import com.crossdrives.driveclient.upload.GoogleDriveUploadRequest;
import com.crossdrives.driveclient.upload.IUploadRequest;
import com.google.api.services.drive.model.File;

public class GoogleDriveAboutRequestBuilder extends BaseRequestBuilder implements IAboutRequestBuilder {
    GoogleDriveClient mClient;
    public GoogleDriveAboutRequestBuilder(GoogleDriveClient client) {
        mClient = client;
    }


    @Override
    public IAboutRequest buildRequest() {
        return new GoogleDriveAboutRequest(mClient);
    }
}
