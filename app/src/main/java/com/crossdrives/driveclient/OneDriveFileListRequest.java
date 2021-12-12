package com.crossdrives.driveclient;

import android.util.Log;


import com.crossdrives.transcode.BaseOperator;
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
    /*
    100 is a feeling value. May need a fine tuning in the future.
     */
    final int PAGE_SIZE = 100;
    int mPageSize = PAGE_SIZE;

    public OneDriveFileListRequest(OneDriveClient client) {
        super();
        mClient = client;
    }
    /*
        Drive item Properties: https://docs.microsoft.com/en-us/graph/api/resources/driveitem?view=graph-rest-1.0
    */

    @Override
    public IFileListRequest select(final String value) {

        addQueryOption(new QueryOption("Select", value));
        return this;
    }

    /*
       Query parameter: https://docs.microsoft.com/en-us/graph/query-parameters#filter-parameter
     */
    @Override
    public IFileListRequest filter(String value) {
        addQueryOption(new QueryOption("Filter", value));
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
                    .buildRequest()
                    .filter("startswith(name, 'VIDEO')")
                    .top(mPageSize);

                    //.filter("name in ('VIDEO0025.3gp')")
                    //.count(true);
                    //.select(null);
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

    /*
    "name contains 'cdfs'" + " and " + "mimeType ='application/vnd.google-apps.folder'"
     */
    private String getFilterClause(String Q){
        List<QueryOption> options;
        QueryOption option;
        String clause="";
        int i = 0;
        options = getQueryOptions();
        for (i = 0; i < options.size();i++){
            option = options.get(i);
            if(option.getName().equals("Filter") != true)
                break;
            //Conditional operators: or and and
            Q.indexOf();
            Q.charAt()
            //clause =

                    //query term
            //value
        }


        return clause;
    }

    private int getConditional(String s){
        int i_or, i_and, index = -1;
        i_or = s.indexOf("or");
        i_and = s.indexOf("and");

        if(i_and == -1 && i_or == -1){
            //single query term
        }else if(i_and > i_or){
            //"or" presents prior to "and"
            index = i_or;
        }
        else{
            //"and" presents prior to "or"
            index = i_and;
        }

        return index;
    }


}
