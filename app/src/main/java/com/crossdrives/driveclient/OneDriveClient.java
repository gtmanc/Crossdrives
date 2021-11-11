package com.crossdrives.driveclient;

import android.util.Log;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

public class OneDriveClient implements IDriveClient {
    static private String TAG = "CD.GraphDriveClient";
    private static OneDriveClient mOneDriveClient = null;


    public OneDriveClient() {
    }

    static OneDriveClient create(Object token) {
        //IGraphServiceClient msclient = (IGraphServiceClient)SignInAccount;

        callGraphAPI((String)token);
        return null;
    }


    static private void callGraphAPI(String token) {

        //final String accessToken = authenticationResult.getAccessToken();

        IGraphServiceClient graphClient =
                GraphServiceClient
                        .builder()
                        .authenticationProvider(new IAuthenticationProvider() {
                            @Override
                            public void authenticateRequest(IHttpRequest request) {
                                Log.d(TAG, "Authenticating request," + request.getRequestUrl());
                                request.addHeader("Authorization", "Bearer " + token);
                            }
                        })
                        .buildClient();
        graphClient
                .me()
                .drive()
                .buildRequest()
                .get(new ICallback<Drive>() {
                    @Override
                    public void success(final Drive drive) {
                        Log.d(TAG, "Found Drive " + drive.id);
                        //displayGraphResult(drive.getRawObject());
                        Log.d(TAG, "Raw Object: " + drive.getRawObject());

                    }

                    @Override
                    public void failure(ClientException ex) {
                        //displayError(ex);
                        Log.w(TAG, "callGraphAPI failed: " + ex.toString());

                    }
                });
    }

    public static Builder builder(String token){ return new Builder();}

    public static class Builder{
        public IDriveClient buildClient(){
            return new OneDriveClient();
        }
    }

    /*
        Get Query builder
     */
    @Override
    public IQueryRequestBuilder query() {
        return new OneDriveQueryRequestBuilder();
    }
}
