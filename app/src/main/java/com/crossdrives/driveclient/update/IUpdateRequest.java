package com.crossdrives.driveclient.update;

import java.io.IOException;

public interface IUpdateRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */


    public void run(IUpdateCallBack callback) throws IOException;
}
