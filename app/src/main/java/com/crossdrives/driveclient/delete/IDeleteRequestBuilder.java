package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.model.File;

public interface IDeleteRequestBuilder {

    IDeleteRequest buildRequest(File metaData);
}
