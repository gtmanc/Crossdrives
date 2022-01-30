package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;

public interface IDeleteRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IDeleteCallBack<File> callback);
}
