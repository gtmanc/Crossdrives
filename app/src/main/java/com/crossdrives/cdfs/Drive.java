package com.crossdrives.cdfs;

import com.crossdrives.driveclient.IDriveClient;

public class Drive {
    private long mFreeSize;
    private long mTotalSize;
    private IDriveClient mClient;
    public Drive(IDriveClient client) {
        mClient = client;
    }

    IDriveClient getClient(){
        return mClient;
    }

}
