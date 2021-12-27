package com.crossdrives.driveclient;

import java.io.InputStream;
import java.io.OutputStream;

public interface IDownloadRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    //public void run(IDownloadCallBack<InputStream> callback);
    public void run(IDownloadCallBack<OutputStream> callback);
}
