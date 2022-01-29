package com.crossdrives.driveclient;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public interface ICreateRequest{

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(ICreateCallBack<File> callback);
}
