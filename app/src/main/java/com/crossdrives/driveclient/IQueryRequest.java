package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;
import com.microsoft.graph.concurrency.ICallback;


public interface IQueryRequest {

    public IQueryRequest select();


    public void run(ICallBack<FileList> callback);
}
