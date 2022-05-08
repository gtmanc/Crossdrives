package com.crossdrives.cdfs.allocation;

import java.io.File;

public interface ISplitProgressCallback {

    // Called after each slice of the file will be uploaded is ready
    public void progress(File SplittedFile);

    public void onFailure(String ex);

}
