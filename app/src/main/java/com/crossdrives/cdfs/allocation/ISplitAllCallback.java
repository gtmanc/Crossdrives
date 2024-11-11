package com.crossdrives.cdfs.allocation;

import java.io.File;
import java.util.Collection;

public interface ISplitAllCallback {

    //called upon the split has stated for each drive
    public void start(String driveName, long totalRemainng);

    /*
        Called after each slice (chunk) of the file is ready in local storage as a file
        The slice file should be deleted after the slice is uploaded to remote drive
    */
    public void progress(String driveName, File slice, long length);

    //called after splitting for each allocation is finished no matter error occurred or not
    public void finish(String name, long remaining);

    //called when all of the planned are split with or without error
    public void completedAll();

    public void onFailurePerDrive(String driveName, String ex);

    public void onFailure(String ex);
}
