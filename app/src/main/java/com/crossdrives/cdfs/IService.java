package com.crossdrives.cdfs;

import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;

public interface IService {
    public void list(Object nextPage, IServiceCallback callback) throws MissingDriveClientException;

    public Task<OutputStream> download(String id);
}
