package com.crossdrives.driveclient.create;

import com.crossdrives.driveclient.create.ICreateRequest;
import com.google.api.services.drive.model.File;

public interface ICreateRequestBuilder {

    ICreateRequest buildRequest(File metaData);
}
