package com.crossdrives.driveclient;

public class OneDriveQueryRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {

    public IQueryRequest buildRequest(){
        return new OneDriveQueryRequest();
    }
}
