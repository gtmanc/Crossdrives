package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;

public class OneDriveDeleteRequest extends BaseRequest implements IDeleteRequest{
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

        if(item != null){
            f.getFile().setName(item.name);
            f.getFile().setId(item.id);
            callback.success(f);
        }
        else{
            callback.failure("Failed to delete item.");
        }
    }
}
