package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public interface ICreateRequestBuilder {

    ICreateRequest buildRequest(File metaData);
}
