package com.crossdrives.driveclient;

import com.microsoft.graph.options.QueryOption;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRequest implements IBaseRequest{
    static private String TAG = "ODC.BaseRequest";

    public BaseRequest() {

        //getUserDriveID();
    }
}
