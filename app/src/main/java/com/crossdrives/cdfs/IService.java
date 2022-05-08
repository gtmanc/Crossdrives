package com.crossdrives.cdfs;

import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.OutputStream;

public interface IService {
    public Task list(Object nextPage) throws MissingDriveClientException, Throwable;


    public Task upload(File file) throws MissingDriveClientException, Throwable;

    public Task<OutputStream> download(String id);
}
