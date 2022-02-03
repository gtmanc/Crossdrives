package com.crossdrives.driveclient.create;

import com.crossdrives.driveclient.create.ICreateCallBack;
import com.google.api.services.drive.model.File;

public interface ICreateRequest{

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(ICreateCallBack<File> callback);
}
