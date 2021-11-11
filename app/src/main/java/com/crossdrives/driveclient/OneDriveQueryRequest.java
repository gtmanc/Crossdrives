package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;
import com.microsoft.graph.concurrency.ICallback;

public class OneDriveQueryRequest implements IQueryRequest{

    @Override
    public IQueryRequest select() {
        return this;
    }

    @Override
    public void run(ICallBack<FileList> callback) {


    }
}
