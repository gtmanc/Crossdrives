package com.crossdrives.driveclient.list;

import android.util.Log;


import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.transcode.BaseTranscoder;
import com.crossdrives.transcode.GoogleQueryTerm;
import com.crossdrives.transcode.GraphTranscoder;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.DriveItemCollectionRequest;
import com.microsoft.graph.requests.DriveItemCollectionRequestBuilder;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.DriveRequest;
import com.microsoft.graph.requests.DriveRequestBuilder;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OneDriveFileListRequest extends BaseRequest implements IFileListRequest {
    private String TAG = "CD.OneDriveQueryRequest";
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
    public void run(IFileListCallBack<FileList, Object> callback) {
        final List<Option> options = new LinkedList<Option>();
        DriveItemCollectionRequest request;

        String parent;
        DriveRequestBuilder drb;
        DriveItemRequestBuilder dirb;

        if(mNextPageBuilder != null){
            request = mNextPageBuilder.buildRequest();
        }else{
            drb = mClient.getGraphServiceClient()
                    .me()
                    .drive();
            parent = getParent(mfilterClause);
            if(parent != null){
                dirb = drb.items(parent);
            }
            else{
                dirb = drb.root();
            }

            request = dirb
                    .children()
                    .buildRequest();
        }
        //apply filter?
        Log.d(TAG, "Apply filter: " + mfilterClause);
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
                            Log.d(TAG, "Item id: " + items.get(i).id);
                            f.setName(items.get(i).name);
                            f.setId(items.get(i).id);
                            files.add(f);
                        }
                        fileList.setFiles(files);
                        Log.d(TAG, "Next page: " + DriveItemCollectionPage.getNextPage());
                        callback.success(fileList, DriveItemCollectionPage.getNextPage());
                })
                .exceptionally(ex -> {Log.w(TAG, "Get root failed: " + ex.toString());
                        callback.failure(ex.toString());return null;
                });


    }
    /*
            Query term: parents
            e.g. To query item in a folder
            Google: '123456' in parents
            Graph: not a query string. This method simply return the parent ID. The ID will be used in
            graph sdk call ();

    */
    private String getParent(String google_qs) {
        String s = null;
        int i;
        StringBuffer graph_qs = new StringBuffer("");

        Log.d(TAG, "get parents. Given string: " + google_qs);
        //Exit if query term 'parents' doesn't present
        if(!google_qs.contains("Parents")) {
            Log.d(TAG, "Query term 'parents' not found:" + google_qs);
            return null;
        }
        //Make sure "in" operator present
        i = google_qs.indexOf("in");
        if(i > 0 ){
            graph_qs = graph_qs.append(google_qs);
            s = graph_qs.substring(0, i);
        }

        Log.d(TAG, "Parent ID: " + s);
        return s;
    }
}
