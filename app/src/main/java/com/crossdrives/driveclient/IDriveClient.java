package com.crossdrives.driveclient;

public interface IDriveClient {


    /*
        List files(items) in a folder.
     */
    IQueryRequestBuilder list();

    /*
        Download file(item) content
     */
    IDownloadRequestBuilder download();

    /*
        Upload file(item)
     */
    IUploadRequestBuilder upload();
}
