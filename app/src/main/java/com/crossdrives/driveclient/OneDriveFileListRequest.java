package com.crossdrives.driveclient;

import android.util.Log;


import com.crossdrives.transcode.BaseTranscoder;
import com.crossdrives.transcode.GraphTranscoder;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.DriveItemCollectionRequest;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OneDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "ODC.OneDriveQueryRequest";
    OneDriveClient mClient;
    DriveItemCollectionRequestBuilder mNextPageBuilder;
    private String mfilterClause, mSelectClause;
    int mPageSize = 0;  //0 means 'not yet assigned'

    public OneDriveFileListRequest(OneDriveClient client) {
        super();
        mClient = client;
    }
    /*
        Drive item Properties: https://docs.microsoft.com/en-us/graph/api/resources/driveitem?view=graph-rest-1.0
    */
    /*
        TODO: Not yet implemented
     */
    @Override
    public IFileListRequest select(final String value) {

        return this;
    }

    /*
       Query parameter: https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter
     */
    @Override
    public IFileListRequest filter(String value) {
        String s = value;
        if(value != null){
            BaseTranscoder transcoder = new GraphTranscoder();
            s = transcoder.execute(value);
        }
        mfilterClause = s;
        return this;
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
        //TODO: behavior of google page size is different from graph as the name is 'top' rather than PageSize
        //Here we only adapt it with number of size
        if(size != 0) {
            mPageSize = size + 1;
        }

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
                    .buildRequest();
        }
        //apply filter?
        if(mfilterClause != null){
            request.filter(mfilterClause);
        }
        //apply top?
        if(mPageSize != 0){
            request.top(mPageSize);
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
                        Log.d(TAG, "Next page: " + DriveItemCollectionPage.getNextPage());
                        callback.success(fileList, DriveItemCollectionPage.getNextPage());
                })
                .exceptionally(ex -> {Log.d(TAG, "Get root failed: " + ex.toString()); return null;});


    }



}
