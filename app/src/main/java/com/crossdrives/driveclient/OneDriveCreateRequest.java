package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.DriveRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;

import java.util.List;

public class OneDriveCreateRequest extends BaseRequest implements ICreateRequest{
    final String TAG = "CD.OneDriveCreateRequest";
    OneDriveClient mClient;
    File mMetaData;

    public OneDriveCreateRequest(OneDriveClient client, File metaData) {
        mClient = client;
        mMetaData = metaData;
    }

    @Override
    public void run(ICreateCallBack<File> callback) {
        DriveRequestBuilder rb;
        DriveItemRequestBuilder irb;
        DriveItem createdItem;
        Folder createdFolder;
        File file = new File();
        DriveItem driveItem = new DriveItem();
        driveItem.name = mMetaData.getName();
        Folder folder = new Folder();
        driveItem.folder = folder;
        driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));

        rb = mClient.getGraphServiceClient().me().drive();
        irb = buildItemRequest(rb, mMetaData.getParents());
        createdItem = irb.children()
                .buildRequest()
                .post(driveItem);

        if(createdItem != null){
            file.setId(createdItem.id);
            file.setName(createdItem.name);
            callback.success(file);
        }
        else{
            callback.failure("Failed to create folder!");
        }

    }

    DriveItemRequestBuilder buildItemRequest(DriveRequestBuilder rb, List<String> parents){
        DriveItemRequestBuilder irb = null;

        irb = rb.root();
        if(parents == null){
            Log.d(TAG, "PID is null, create file in root");
        }
        else if(parents.size() == 0) {
            Log.d(TAG, "No PID is given, create file in root");
        }
        else{
            irb = rb.items(parents.get(0));
        }

        return irb;
    }

}
