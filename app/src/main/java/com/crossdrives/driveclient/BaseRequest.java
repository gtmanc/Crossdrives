package com.crossdrives.driveclient;

import android.util.Log;

import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

public abstract class BaseRequest implements IBaseRequest{
    static private String TAG = "ODC.BaseRequest";
    OneDriveClient mClient;
    String mDriveID;

    public BaseRequest() {
        getUserDriveID();
    }

    private void getUserDriveID(){
        mClient.getGraphServiceClient()
                .me()
                .drive()
                .buildRequest()
                .get(new ICallback<Drive>() {
                    @Override
                    public void success(final Drive drive) {
                        mDriveID = drive.id;
                        Log.d(TAG, "Found Drive " + drive.id);
                        //displayGraphResult(drive.getRawObject());
                        //Log.d(TAG, "Raw Object: " + drive.getRawObject());
                    }

                    @Override
                    public void failure(ClientException ex) {
                        //displayError(ex);
                        mDriveID = null;
                        Log.w(TAG, "callGraphAPI failed: " + ex.toString());

                    }
                });
    }

    public OneDriveClient getOneDriveClient(){
        return mClient;
    }

    public String getDriveID(){ return mDriveID; }
}
