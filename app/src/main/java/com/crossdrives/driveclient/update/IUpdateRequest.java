package com.crossdrives.driveclient.update;

public interface IUpdateRequest {

    public void Reason(String reason);


    /*
        Execute the request

     * @param callback gets called when response got from remote
     */


    public void run(IUpdateCallBack callback);
}
