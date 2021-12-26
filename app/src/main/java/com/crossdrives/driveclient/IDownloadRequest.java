package com.crossdrives.driveclient;

import java.io.InputStream;

public interface IDownloadRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(IDownloadCallBack<InputStream> callback);
}
