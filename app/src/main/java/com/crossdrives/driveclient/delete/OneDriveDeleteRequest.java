package com.crossdrives.driveclient.delete;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;

public class OneDriveDeleteRequest extends BaseRequest implements IDeleteRequest{
    OneDriveClient mClient;
    File mMetaData;
    public OneDriveDeleteRequest(OneDriveClient client, File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(IDeleteCallBack<File> callback) {
        DriveItem item;
        File f = new File();
        item = mClient.getGraphServiceClient().me().drive().items(mMetaData.getId())
                .buildRequest()
                .delete();

        if(item != null){
            f.setName(item.name);
            f.setId(item.id);
            callback.success(f);
        }
        else{
            callback.failure("Failed to delete item.");
        }
    }
}
