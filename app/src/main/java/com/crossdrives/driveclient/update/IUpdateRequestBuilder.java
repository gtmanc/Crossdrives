package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.model.File;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;

public interface IUpdateRequestBuilder {

    public IUpdateRequest buildRequest(String fileID, com.google.api.services.drive.model.File file);

    public IUpdateRequest buildRequest(String fileID, com.google.api.services.drive.model.File file, AbstractInputStreamContent mediaContent);
}
