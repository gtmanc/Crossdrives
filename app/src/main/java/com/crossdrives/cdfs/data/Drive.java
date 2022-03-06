package com.crossdrives.cdfs.data;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.driveclient.IDriveClient;

public class Drive {
    private long mFreeSize;
    private long mTotalSize;
    private IDriveClient mClient;
    public AllocContainer mContainer;

    public Drive(IDriveClient client) {
        mClient = client;
    }

    public IDriveClient getClient(){
        return mClient;
    }

    public AllocContainer getContainer(){return mContainer;}
    public void addContainer(AllocContainer container){mContainer = container;}


}
