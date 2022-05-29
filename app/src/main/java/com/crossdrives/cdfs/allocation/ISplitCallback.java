package com.crossdrives.cdfs.allocation;

import java.io.File;

public interface ISplitCallback {
    //Called when file is split to chunks for each allocation
    public void start(long total);

    /*
        Called after each slice (chunk) of the file will be uploaded is ready
        The slice file will be delete after the callabck returns to split
    */
    public void progress(File slice, long length);

    //called after splitting for each allocation is finished no matter error occurred or not
    public void finish(long remaining);

    public void onFailure(String ex);
}
