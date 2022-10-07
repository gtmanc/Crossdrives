package com.crossdrives.driveclient.delete;

import android.util.Log;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;

public class OneDriveDeleteRequest extends BaseRequest implements IDeleteRequest{
    final String TAG = "CD.OneDriveDeleteRequest";
    OneDriveClient mClient;
    com.crossdrives.driveclient.model.File mMetaData;
    public OneDriveDeleteRequest(OneDriveClient client, com.crossdrives.driveclient.model.File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(IDeleteCallBack<com.crossdrives.driveclient.model.File> callback) {
        DriveItem item;
        com.crossdrives.driveclient.model.File f = new com.crossdrives.driveclient.model.File();
        item = mClient.getGraphServiceClient().me().drive().items(mMetaData.getFile().getId())
                .buildRequest()
                .delete();

        //Some fields in the returned item could be null. e.g. name, id and so on.
        if(item != null){
            f.setString(mMetaData.getString());
            f.setInteger(mMetaData.getInteger());
            //f.getFile().setId(item.id);
            Log.d(TAG, "Resulting response. name: " + item.name + " id: " + item.id);
            callback.success(f);
        }
        else{
            callback.failure("Failure occurred during deleting item. Item returned from graph service is null.");
        }
    }
}
