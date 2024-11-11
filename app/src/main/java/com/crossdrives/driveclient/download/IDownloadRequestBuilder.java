package com.crossdrives.driveclient.download;

import com.crossdrives.driveclient.download.IDownloadRequest;

public interface IDownloadRequestBuilder {

    IDownloadRequest buildRequest(String id);
}
