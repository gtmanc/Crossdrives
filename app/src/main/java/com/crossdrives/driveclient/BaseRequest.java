package com.crossdrives.driveclient;

import com.microsoft.graph.options.QueryOption;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRequest implements IBaseRequest{
    static private String TAG = "ODC.BaseRequest";

    List<QueryOption> mOption = new ArrayList<>();

    public BaseRequest() {

        //getUserDriveID();
    }

    public void addQueryOption(QueryOption option){
        mOption.add(option);
    }

    public List<QueryOption> getQueryOptions(){
        return mOption;
    }

}
