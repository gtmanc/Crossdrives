package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public interface IDeleteRequestBuilder {

    IDeleteRequest buildRequest(File metaData);
}
