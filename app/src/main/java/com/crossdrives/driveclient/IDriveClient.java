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
        If parent(folder) is not given, the file is uploaded to use's root.
     */
    IUploadRequestBuilder upload();

    /*
        Create folder
     */
    ICreateRequestBuilder create();

    /*
        Delete file(item)
     */
    IDeleteRequestBuilder delete();
}
