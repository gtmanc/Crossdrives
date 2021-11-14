package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;

public class GoogleDriveQueryRequest extends BaseRequest implements IQueryRequest{
    GoogleDriveClient mClient;
    public GoogleDriveQueryRequest(GoogleDriveClient client) {
        mClient = client;
    }

    @Override
    public IQueryRequest select() {
        return null;
    }

    @Override
    public void run(ICallBack<FileList> callback) {
        mClient.getGoogleDriveService().

    }
}
