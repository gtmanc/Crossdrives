package com.crossdrives.driveclient.upload;


import com.crossdrives.driveclient.model.File;

public interface IUploadCallBack{
    /**
     * How successful results are handled
     *
     * @param file the file uploaded
     */
    void success(File file);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex, java.io.File originalFile);
}
