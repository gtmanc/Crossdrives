package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequest;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionRequestBuilder;

import java.util.ArrayList;
import java.util.List;

public class OneDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "ODC.OneDriveQueryRequest";
    OneDriveClient mClient;
    IDriveItemCollectionRequestBuilder mNextPageBuilder;
    /*
    100 is a feeling value. May need a fine tuning in the future.
     */
    final int PAGE_SIZE = 100;
    int mPageSize = PAGE_SIZE;

    public OneDriveFileListRequest(OneDriveClient client) {
        super();
        mClient = client;
    }

    @Override
    public IFileListRequest select(final String value) {
        return this;
    }

    @Override
    public IFileListRequest setNextPage(Object page) {
        mNextPageBuilder = (IDriveItemCollectionRequestBuilder)page;
        return this;
    }

    /*
     */
    @Override
    public IFileListRequest setPageSize(int size) {
        mPageSize = size;
        return this;
    }

    @Override
    public void run(ICallBack<FileList, Object> callback) {
        IDriveItemCollectionRequest request;

        if(mNextPageBuilder != null){
            request = mNextPageBuilder.buildRequest();
        }else{
            request = mClient.getGraphServiceClient()
                    .me()
                    .drive()
                    .root()
                    .children()
                    .buildRequest()
                    .top(mPageSize);
        }

        request
                .get(new ICallback<IDriveItemCollectionPage>() {
                    @Override
                    public void success(IDriveItemCollectionPage iDriveItemCollectionPage) {
                        FileList files = new FileList();
                        List<DriveItem> items =
                        iDriveItemCollectionPage.getCurrentPage();

                        //IDriveItemCollectionRequestBuilder b = iDriveItemCollectionPage.getNextPage();

                        Log.d(TAG, "Size of root children: " + items.size());
                        for(int i = 0; i < items.size();i++) {
                            Log.d(TAG, "Item name: " + items.get(i).name);
                            files.set(items.get(i).name, null);
                        }

                        callback.success(files, iDriveItemCollectionPage.getNextPage());
                    }


                    @Override
                    public void failure(ClientException ex) {
                        Log.d(TAG, "Get root failed: " + ex.toString());

                        callback.failure(ex.toString());
                    }
                });
    }
}
