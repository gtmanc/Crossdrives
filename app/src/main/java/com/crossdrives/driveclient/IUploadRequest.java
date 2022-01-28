package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;

public interface IUploadRequest {
    /*
        Media type
     */
    public void meidaType(String type);

    /*
        Upload type
     */
    public void uploadType(String type);

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IUploadCallBack callback);
}
