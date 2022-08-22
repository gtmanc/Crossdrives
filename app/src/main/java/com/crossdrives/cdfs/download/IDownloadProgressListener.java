package com.crossdrives.cdfs.download;

import com.crossdrives.cdfs.upload.Upload;

public interface IDownloadProgressListener {
    void progressChanged(Download downloader);
}
