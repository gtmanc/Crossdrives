package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.FileList;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.ItemActivityCollectionPage;

import java.util.List;

public class OneDriveQueryRequest extends BaseRequest implements IQueryRequest{
    private String TAG = "ODC.OneDriveQueryRequest";
    OneDriveClient mClient;

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
                .root()
                .children()
                .buildRequest()
                .get(new ICallback<IDriveItemCollectionPage>() {
                    @Override
                    public void success(IDriveItemCollectionPage iDriveItemCollectionPage) {
                        List<DriveItem> items =
                        iDriveItemCollectionPage.getCurrentPage();
                        Log.d(TAG, "Size of root children: " + items.size());
                        for(int i = 0; i < items.size();i++) {
                            Log.d(TAG, "Item name: " + items.get(i).name);
                        }
                    }

                    @Override
                    public void failure(ClientException ex) {
                        Log.d(TAG, "Get root failed: " + ex.toString());
                    }
                });
    }
}
