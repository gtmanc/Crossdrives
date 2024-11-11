package com.crossdrives.cdfs;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;

public interface IDriveAccess {
    public void listFolder(IDriveClient drive, IFileListCallBack<FileList, Object> callback, Object nextPage);

    public Task<OutputStream> download(String id);

    public Task<String> upload(File metadata, java.io.File path);

    public Task<String> create(File metadata);

    public Task<String> delete(File metadata);

}
