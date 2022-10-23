package com.crossdrives.cdfs;

import com.crossdrives.cdfs.delete.Delete;

public interface IProgressListener {
    void progressChanged(Delete deleter);
}
