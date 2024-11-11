package com.crossdrives.driveclient.about;

import android.util.Log;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.About;

public class OneDriveAboutRequest extends BaseRequest implements IAboutRequest {
    private String TAG = "CD.OneDriveAboutRequest";
    OneDriveClient mClient;

    public OneDriveAboutRequest(OneDriveClient mClient) {
        this.mClient = mClient;
    }

    @Override
    public void run(IAboutCallBack callback) {
        mClient.getGraphServiceClient().me().drive().buildRequest().getAsync().thenAccept((metadata)->{
            About about = new About();
            About.StorageQuota quota = new About.StorageQuota();

            quota.setUsage(metadata.quota.used);
            quota.setLimit(metadata.quota.total);
            about.setStorageQuota(quota);
//            Log.d(TAG, metadata.quota.total.toString());
//            Log.d(TAG, metadata.quota.used.toString());
            callback.success(about);
        }).exceptionally(ex->{
            callback.failure(ex.getMessage());
            return null;
        });
    }
}
