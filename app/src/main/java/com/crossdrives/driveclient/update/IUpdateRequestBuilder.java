package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.model.File;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.ContentRestriction;

import java.util.List;

public interface IUpdateRequestBuilder {


    public IUpdateRequest buildRequest(String fileID, MetaData metaData);

    public IUpdateRequest buildRequest(String fileID, MetaData metaData, AbstractInputStreamContent mediaContent);
}
