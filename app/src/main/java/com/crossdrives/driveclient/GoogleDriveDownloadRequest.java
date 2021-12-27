package com.crossdrives.driveclient;

import java.io.InputStream;

public class GoogleDriveFileDownloadRequest extends BaseRequest implements IDownloadRequest{
    GoogleDriveClient mClient;
    public GoogleDriveFileDownloadRequest(GoogleDriveClient client) { mClient = client;
    }

    @Override
    public void run(IDownloadCallBack<InputStream> callback) {

    }
}
