package com.crossdrives.driveclient;

import com.microsoft.graph.requests.extensions.GraphServiceClient;

public abstract class BaseRequestBuilder implements IRequestBuilder {
    OneDriveClient mClient;

   public OneDriveClient getOneDriveClient(){
       return mClient;
   }

}
