package com.crossdrives.driveclient.get;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;

public class GoogleDriveGetRequest extends BaseRequest implements IGoogleDriveGetRequest {
    GoogleDriveClient mClient;

    public GoogleDriveGetRequest(GoogleDriveClient mClient) {
        this.mClient = mClient;
    }

    @Override
    public void run(IGetCallBack callback) {

    }
}
