package com.crossdrives.cdfs.delete;

import com.crossdrives.cdfs.IProgressListener;
import com.crossdrives.cdfs.download.Download;

public interface IDeleteProgressListener extends IProgressListener {
    void progressChanged(Delete deleter);
}
