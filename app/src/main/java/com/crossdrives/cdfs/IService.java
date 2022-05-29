package com.crossdrives.cdfs;

import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public interface IService {
    public Task list(Object nextPage) throws MissingDriveClientException, Throwable;

    public Task upload(InputStream ins, String name, com.google.api.services.drive.model.File parent) throws MissingDriveClientException, Throwable;

    public Task<OutputStream> download(String id);
}
