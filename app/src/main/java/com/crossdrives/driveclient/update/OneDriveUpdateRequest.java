package com.crossdrives.driveclient.update;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemRequest;
import com.microsoft.graph.requests.DriveItemRequestBuilder;

import java.io.IOException;

public class OneDriveUpdateRequest extends BaseRequest implements IUpdateRequest {
    OneDriveClient mClient;
    String mfileID;
    com.google.api.services.drive.model.File mMetaData;
    AbstractInputStreamContent mMediaContent;

    public OneDriveUpdateRequest(OneDriveClient client, String id, File metaData) {
        this.mClient = client;
        this.mfileID = id;
        this.mMetaData = metaData;
    }

    public OneDriveUpdateRequest(OneDriveClient client, String id, File metaData, AbstractInputStreamContent mediaContent) {
        this.mClient = client;
        this.mfileID = id;
        this.mMetaData = metaData;
        this.mMediaContent = mediaContent;
    }

    @Override
    public void run(IUpdateCallBack callback) throws IOException {
        DriveItemRequest request;
        DriveItem item = new DriveItem();

        request = mClient.getGraphServiceClient().me().drive().items(mfileID)..buildRequest();
        if(mMediaContent != null){
            byte[] stream = new byte[100];
            item.
            request.put(item);
        }else{
            request.patch(item);
        }

    }
}
