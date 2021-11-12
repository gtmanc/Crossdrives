package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.FileList;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Drive;

public class OneDriveQueryRequest extends BaseRequest implements IQueryRequest{
    private String TAG = "ODC.OneDriveQueryRequest";

    public OneDriveQueryRequest(OneDriveClient client) {
        super();
        mClient = client;
    }

    @Override
    public IQueryRequest select() {
        return this;
    }

    @Override
    public void run(ICallBack<FileList> callback) {

        mClient.getGraphServiceClient()
                .me()
                .drive()
                .buildRequest()
                .select(mDriveID)
                .get(new ICallback<Drive>() {
                    @Override
                    public void success(final Drive drive) {
                        Log.d(TAG, "Found Drive " + drive.id);
                        //displayGraphResult(drive.getRawObject());
                        Log.d(TAG, "Raw Object: " + drive.getRawObject());

                    }

                    @Override
                    public void failure(ClientException ex) {
                        //displayError(ex);
                        Log.w(TAG, "callGraphAPI failed: " + ex.toString());

                    }
                });
    }
}
