package com.crossdrives.cdfs;

import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;

public interface IService {
    public Task<FileList> list(Object nextPage);

    public Task<OutputStream> download(String id);
}
