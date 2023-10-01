package com.crossdrives.driveclient.update;

import java.io.IOException;
import java.util.List;

public interface IUpdateRequest {

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public IUpdateRequest parentsToRemoved(List<String> parents);

    public void run(IUpdateCallBack callback) throws IOException;
}
