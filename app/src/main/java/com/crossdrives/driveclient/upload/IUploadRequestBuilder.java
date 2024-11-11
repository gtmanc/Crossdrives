package com.crossdrives.driveclient.upload;

import com.crossdrives.driveclient.upload.IUploadRequest;
import com.google.api.services.drive.model.File;

public interface IUploadRequestBuilder {
    public IUploadRequest buildRequest(File metadata, java.io.File path);
}
