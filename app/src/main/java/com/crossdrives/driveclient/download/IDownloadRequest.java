package com.crossdrives.driveclient.download;

import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;

import java.io.OutputStream;

public interface IDownloadRequest{

    /*
        Set custom data which will send back with the callback upon completion
     */
    public IDownloadRequest setAdditionInt(int i);

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    //public void run(IDownloadCallBack<InputStream> callback);
    public void run(IDownloadCallBack<MediaData> callback);
}
