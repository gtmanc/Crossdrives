package com.crossdrives.driveclient;

import android.util.Log;


import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.DriveItemCollectionRequest;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OneDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "ODC.OneDriveQueryRequest";
    OneDriveClient mClient;
    DriveItemCollectionRequestBuilder mNextPageBuilder;
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
    public IFileListRequest filter(String value) {
        return null;
    }

    @Override
    public IFileListRequest setNextPage(Object page) {
        mNextPageBuilder = (DriveItemCollectionRequestBuilder)page;
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
        final List<Option> options = new LinkedList<Option>();
        DriveItemCollectionRequest request;

        if(mNextPageBuilder != null){
            request = mNextPageBuilder.buildRequest();
        }else{
//            request = mClient.getGraphServiceClient()
//                    .me()
//                    .drive()
//                    .root()
//                    .children()
//                    .buildRequest()
//                    .top(mPageSize).select(null);
            request = mClient.getGraphServiceClient()
                    .me()
                    .drive()
                    .root()
                    .children()
                    .buildRequest(options)
                    .top(mPageSize).select(null);
        }
        request.getAsync().thenAccept(DriveItemCollectionPage ->{
                        FileList fileList = new FileList();
                        List<DriveItem> items =
                        DriveItemCollectionPage.getCurrentPage();
                        List<File> files = new ArrayList<>();
                        //IDriveItemCollectionRequestBuilder b = iDriveItemCollectionPage.getNextPage();
                        Log.d(TAG, "Size of root children: " + items.size());
                        for(int i = 0; i < items.size();i++) {
                            File f = new File();
                            Log.d(TAG, "Item name: " + items.get(i).name);
                            f.setName(items.get(i).name);
                            files.add(f);
                        }
                        fileList.setFiles(files);
                        callback.success(fileList, DriveItemCollectionPage.getNextPage());
                })
                .exceptionally(ex -> {return null;});

//        request
//                .get(new ICallback<IDriveItemCollectionPage>() {
//                    @Override
//                    public void success(IDriveItemCollectionPage iDriveItemCollectionPage) {
//                        FileList fileList = new FileList();
//                        List<DriveItem> items =
//                        iDriveItemCollectionPage.getCurrentPage();
//                        List<File> files = new ArrayList<>();
//                        //IDriveItemCollectionRequestBuilder b = iDriveItemCollectionPage.getNextPage();
//                        Log.d(TAG, "Size of root children: " + items.size());
//                        for(int i = 0; i < items.size();i++) {
//                            File f = new File();
//                            Log.d(TAG, "Item name: " + items.get(i).name);
//                            f.setName(items.get(i).name);
//                            files.add(f);
//                        }
//                        fileList.setFiles(files);
//                        callback.success(fileList, iDriveItemCollectionPage.getNextPage());
//                    }
//
//
//                    @Override
//                    public void failure(ClientException ex) {
//                        Log.d(TAG, "Get root failed: " + ex.toString());
//
//                        callback.failure(ex.toString());
//                    }
//                });
    }
}
