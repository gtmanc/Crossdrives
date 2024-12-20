package com.crossdrives.driveclient;

import com.crossdrives.driveclient.about.IAboutRequestBuilder;
import com.crossdrives.driveclient.create.ICreateRequestBuilder;
import com.crossdrives.driveclient.delete.IDeleteRequestBuilder;
import com.crossdrives.driveclient.download.IDownloadRequestBuilder;
import com.crossdrives.driveclient.get.IGetRequestBuilder;
import com.crossdrives.driveclient.list.IQueryRequestBuilder;
import com.crossdrives.driveclient.update.IUpdateRequestBuilder;
import com.crossdrives.driveclient.upload.IUploadRequestBuilder;

public interface IDriveClient {
    /*
        build client
     */
    IDriveClient build(String token);

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

    /*
        About. (Information of user's drive)
     */
    IAboutRequestBuilder about();

    /*
        Update. Update metadata or content of a file/item
     */
    IUpdateRequestBuilder update();

    /*
        Update. get meta data or content of a file/item
     */
    IGetRequestBuilder get();
}
