package com.crossdrives.driveclient.about;

import com.crossdrives.driveclient.upload.IUploadRequest;
import com.google.api.services.drive.model.File;

public interface IAboutRequestBuilder {
    public IAboutRequest buildRequest();
}
