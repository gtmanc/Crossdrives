package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.create.ICreateRequestBuilder;
import com.crossdrives.driveclient.create.OneDriveCreateRequestBuilder;
import com.crossdrives.driveclient.delete.IDeleteRequestBuilder;
import com.crossdrives.driveclient.delete.OneDriveDeleteRequestBuilder;
import com.crossdrives.driveclient.download.IDownloadRequestBuilder;
import com.crossdrives.driveclient.download.OneDriveDownloadRequestBuilder;
import com.crossdrives.driveclient.list.IQueryRequestBuilder;
import com.crossdrives.driveclient.list.OneDriveQueryRequestBuilder;
import com.crossdrives.driveclient.upload.IUploadRequestBuilder;
import com.crossdrives.driveclient.upload.OneDriveUploadRequestBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;;
import com.microsoft.graph.requests.GraphServiceClient;


import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class OneDriveClient implements IDriveClient {
    static private String TAG = "CD.GraphDriveClient";
    private static OneDriveClient mOneDriveClient = null;
    private GraphServiceClient mGraphServiceClient;
    private static String mToken;


    public OneDriveClient() {
    }

//    static OneDriveClient create(Object token) {
//        //IGraphServiceClient msclient = (IGraphServiceClient)SignInAccount;
//
//        callGraphAPI((String)token);
//        return null;
//    }

//    static private void callGraphAPI(String token) {
//
//        //final String accessToken = authenticationResult.getAccessToken();
//
//        GraphServiceClient graphClient =
//                GraphServiceClient
//                        .builder()
//                        .authenticationProvider(new IAuthenticationProvider() {
//                            /*
//                                TODO: We cant guarantee the token is valid each time the client is created.
//                                e.g. App is pushed to the background for longer than 5 minutes and
//                                pulled to foreground afterwards.
//                            */
//                            @NonNull
//                            @Override
//                            public CompletableFuture<String> getAuthorizationTokenAsync(@NonNull URL requestUrl) {
//                                CompletableFuture<String> future = null;
//                                future = new CompletableFuture<>();
//                                future.complete(mToken);
//                                return future;
//                            }
//                        })
//                        .buildClient();
//        graphClient
//                .me()
//                .drive()
//                .buildRequest()
//                .getAsync()
//                .thenAccept(drive -> {Log.d(TAG, "Found Drive " + drive.id);})
//                .exceptionally(ex -> {Log.w(TAG, "callGraphAPI failed: " + ex.toString()); return null;});
//    }

    public static Builder builder(String token){
        mToken = token;
        return new Builder();
    }

    public static class Builder{
        public IDriveClient buildClient(){

            return OneDriveClient.fromConfig(mToken);
        }
    }

    /*
        Get Query Request Builder
     */
    @Override
    public IQueryRequestBuilder list() {

        IQueryRequestBuilder r = null;
        if(mGraphServiceClient == null){
            Log.w(TAG, "mGraphServiceClient is null");
        }else{
            r = new OneDriveQueryRequestBuilder(this);
        }
        return r;
    }

    @Override
    public IDownloadRequestBuilder download() {
        IDownloadRequestBuilder r = null;
        if(mGraphServiceClient == null){
            Log.w(TAG, "mGraphServiceClient is null");
        }else{
            r = new OneDriveDownloadRequestBuilder(this);
        }
        return r;
    }

    @Override
    public IUploadRequestBuilder upload() {
        return new OneDriveUploadRequestBuilder(this);
    }

    @Override
    public ICreateRequestBuilder create() {
        return new OneDriveCreateRequestBuilder(this);
    }

    @Override
    public IDeleteRequestBuilder delete() {
        return new OneDriveDeleteRequestBuilder(this);
    }

    public static OneDriveClient fromConfig(String token){
        OneDriveClient oClient  = new OneDriveClient();
        GraphServiceClient gClient =
                GraphServiceClient
                        .builder()
                        .authenticationProvider(new IAuthenticationProvider() {
                            @NonNull
                            @Override
                            public CompletableFuture<String> getAuthorizationTokenAsync(@NonNull URL requestUrl) {
                                CompletableFuture<String> future = null;
                                future = new CompletableFuture<>();
                                future.complete(mToken);
                                return future;
                            }

//                            @Override
//                            public void authenticateRequest(IHttpRequest request) {
//                                Log.d(TAG, "Authenticating request," + request.getRequestUrl());
//                                request.addHeader("Authorization", "Bearer " + mToken);
//                            }
                        })
                        .buildClient();

        oClient.setGraphServiceClient(gClient);
        return oClient;
    }

    public void setGraphServiceClient(GraphServiceClient client){
        mGraphServiceClient = client;
    }

    public GraphServiceClient getGraphServiceClient(){
        return mGraphServiceClient;
    }
}
