package com.crossdrives.driveclient.get;

import com.crossdrives.driveclient.update.IUpdateCallBack;

public interface IGetRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */


    public void run(IGetCallBack callback);
}
