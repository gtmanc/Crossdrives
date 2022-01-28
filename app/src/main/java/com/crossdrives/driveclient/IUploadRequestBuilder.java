package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public interface IUploadRequestBuilder {
    public IUploadRequest buildRequest(File metadata, java.io.File path);
}
