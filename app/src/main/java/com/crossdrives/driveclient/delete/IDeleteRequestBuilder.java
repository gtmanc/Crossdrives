package com.crossdrives.driveclient.delete;

import com.google.api.services.drive.model.File;

public interface IDeleteRequestBuilder {

    IDeleteRequest buildRequest(File metaData);
}
