package com.crossdrives.driveclient;

public class QueryRequestBuilder extends BaseRequestBuilder implements IQueryRequestBuilder {

    public IQueryRequest buildRequest(){
        return new QueryRequest();
    }
}
