package com.crossdrives.driveclient.about;

import com.crossdrives.driveclient.BaseRequestBuilder;
import com.crossdrives.driveclient.OneDriveClient;

public class OneDriveAboutRequestBuilder extends BaseRequestBuilder implements IAboutRequestBuilder {
    OneDriveClient mClient;

    public OneDriveAboutRequestBuilder(OneDriveClient mClient) {
        this.mClient = mClient;
    }

    @Override
    public IAboutRequest buildRequest() {
        return new OneDriveAboutRequest(mClient);
    }
}
