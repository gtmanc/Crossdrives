package com.crossdrives.driveclient;

public interface IDriveClient {


    /*
        List files(items) in a folder.
     */
    IQueryRequestBuilder list();

    /*
        Download file(items) content
     */
    IDownloadRequestBuilder download();
}
