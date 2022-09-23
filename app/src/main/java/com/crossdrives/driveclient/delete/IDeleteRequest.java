package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.model.File;

public interface IDeleteRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IDeleteCallBack<File> callback);
}
