package com.crossdrives.cdfs.allocation;

import java.io.File;
import java.util.Collection;

public interface ISplitProgressCallback {

    //Called when file is split to chunks for each allocation
    public void start(String name, long total);

    /*
        Called after each slice (chunk) of the file will be uploaded is ready
        The slice file will be delete after the callabck returns to split
    */
    public void progress(File slice);

    //called after splitting for each allocation is finished no matter error occurred or not
    public void finish(String name, long remaining);

    public void onFailure(String ex);

}
