package com.crossdrives.driveclient.about;

import com.crossdrives.driveclient.upload.IUploadCallBack;

public interface IAboutRequest {
    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IAboutCallBack callback);
}
