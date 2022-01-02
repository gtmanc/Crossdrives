package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;

public interface IUploadRequest {
    /*
        Media type
     */
    public void type(String type);

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IUploadCallBack callback);
}
