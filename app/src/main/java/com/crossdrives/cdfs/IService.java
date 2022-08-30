package com.crossdrives.cdfs;

import com.crossdrives.cdfs.exception.CompletionException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.exception.PermissionException;
import com.google.android.gms.tasks.Task;

import java.io.InputStream;

public interface IService {
    public Task list(Object nextPage) throws MissingDriveClientException, Throwable;

    public Task upload(InputStream ins, String name, String parent) throws MissingDriveClientException, Throwable;

    public Task<String> download(String fileID, String parent) throws MissingDriveClientException, CompletionException, PermissionException;
}
