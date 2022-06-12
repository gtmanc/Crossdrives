package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.model.File;

public interface IUpdateRequestBuilder {

    public IUpdateRequest buildRequest(String fileID, File file);
}
