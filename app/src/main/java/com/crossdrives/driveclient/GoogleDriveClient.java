package com.crossdrives.driveclient;

import android.content.Context;
import android.util.Log;

import com.crossdrives.driveclient.about.GoogleDriveAboutRequestBuilder;
import com.crossdrives.driveclient.about.IAboutRequestBuilder;
import com.crossdrives.driveclient.create.GoogleDriveCreateRequestBuilder;
import com.crossdrives.driveclient.create.ICreateRequestBuilder;
import com.crossdrives.driveclient.delete.GoogleDriveDeleteRequestBuilder;
import com.crossdrives.driveclient.delete.IDeleteRequestBuilder;
import com.crossdrives.driveclient.download.GoogleDriveDownloadRequestBuilder;
import com.crossdrives.driveclient.download.IDownloadRequestBuilder;
import com.crossdrives.driveclient.get.GoogleDriveGetRequestBuilder;
import com.crossdrives.driveclient.get.IGetRequestBuilder;
import com.crossdrives.driveclient.list.GoogleDriveFileListRequestBuilder;
import com.crossdrives.driveclient.list.IQueryRequestBuilder;
import com.crossdrives.driveclient.update.GoogleDriveUpdateRequestBuilder;
import com.crossdrives.driveclient.update.IUpdateRequestBuilder;
import com.crossdrives.driveclient.upload.GoogleDriveUploadRequestBuilder;
import com.crossdrives.driveclient.upload.IUploadRequestBuilder;
import com.example.crossdrives.DriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveClient implements IDriveClient {
    private static String TAG = "CD.GoogleDriveClient";
    //private static Context mContext;
    private static GoogleSignInAccount mGoogleSignInAccount;
    private static String mAccessToken;
    private Drive mGgoogleDriveService;
    private DriveServiceHelper mHelper;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private static GoogleCredential mCredential;

//    static public GoogleDriveClient create(Context context, Object accessToken) throws GeneralSecurityException, IOException {
//        mContext = context;
//
//        //GoogleSignInAccount account = (GoogleSignInAccount)SignInAccount;
//
//        if(accessToken != null) {
////            GoogleAccountCredential credential =
////                    GoogleAccountCredential.usingOAuth2(
////                            context, Collections.singleton(DriveScopes.DRIVE_FILE));
////            credential.setSelectedAccount(account.getAccount());
////
////            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
////            HTTP_TRANSPORT.
////            Drive googleDriveService =
////                    new Drive.Builder(
////                            HTTP_TRANSPORT,
////                            new GsonFactory(),
////                            getCredentials())
////                            .setApplicationName("Cross Drives")
////                            .build();
//            GoogleCredential credential = new GoogleCredential().setAccessToken((String)accessToken);
//            Drive googleDriveService =
//                    new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
//                            .setApplicationName("Cross Drives")
//                            .build();
//            if (googleDriveService == null)
//                Log.w(TAG, "googleDriveService is null!");
//            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
//            // Its instantiation is required before handling any onClick actions.
//            // We create DriveServiceHelper here but it will be used later by using getInstance() method
//            //new DriveServiceHelper(googleDriveService);
//            DriveServiceHelper.Create(googleDriveService);
//        }
//
//        return null;
//    }

    /*
        Deprecated
     */
//    static public Builder builder(GoogleSignInAccount SignInAccount){
//        //mContext = context;
//        mGoogleSignInAccount = SignInAccount;
//        return new Builder();
//    }

    static public Builder builder(String accessToken){
            //mContext = context;
            mAccessToken = accessToken;
            return new Builder();
    }

    public static class Builder{
        public IDriveClient buildClient(){
            return GoogleDriveClient.fromConfig();
        }
    }
    public static IDriveClient fromConfig(){
        GoogleDriveClient gClient = new GoogleDriveClient();
        if(mAccessToken != null) {
//            GoogleAccountCredential credential =
//                    GoogleAccountCredential.usingOAuth2(
//                            mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
//            credential.setSelectedAccount(mGoogleSignInAccount.getAccount());
//            Drive googleDriveService =
//                    new Drive.Builder(
//                            AndroidHttp.newCompatibleTransport(),
//                            new GsonFactory(),
//                            credential)
//                            .setApplicationName("Cross Drive")
//                            .build();

            GoogleCredential credential = new GoogleCredential().setAccessToken((String)mAccessToken);
            setCredential(credential);
            Drive googleDriveService =
                    new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                            .setApplicationName("Cross Drives")
                            .build();
            if (googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");

            gClient.setGoogleDriveService(googleDriveService);

            if (googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");

        }

        return gClient;
    }

    @Override
    public IDriveClient build(String token) {
        return GoogleDriveClient.fromConfig();
    }

    @Override
    public IQueryRequestBuilder list() {

        return new GoogleDriveFileListRequestBuilder(this);
    }

    @Override
    public IDownloadRequestBuilder download() {

        return new GoogleDriveDownloadRequestBuilder(this);
    }

    @Override
    public IUploadRequestBuilder upload() {
        return new GoogleDriveUploadRequestBuilder(this);
    }

    @Override
    public ICreateRequestBuilder create() {
        return new GoogleDriveCreateRequestBuilder(this);
    }

    @Override
    public IDeleteRequestBuilder delete() {
        return new GoogleDriveDeleteRequestBuilder(this);
    }

    @Override
    public IAboutRequestBuilder about() {
        return new GoogleDriveAboutRequestBuilder(this);
    }

    @Override
    public IUpdateRequestBuilder update() {
        return new GoogleDriveUpdateRequestBuilder(this);
    }

    @Override
    public IGetRequestBuilder get() {
        return new GoogleDriveGetRequestBuilder(this);
    }


    private void setGoogleDriveService(Drive service){
        mGgoogleDriveService = service;
    }

    public Drive getGoogleDriveService(){
        return mGgoogleDriveService;
    }

    public Executor getExecutor(){
        return mExecutor;
    }

    public static void setCredential(GoogleCredential credential){
        mCredential = credential;
    }

    public static GoogleCredential getCredentail(){
        return mCredential;
    }
}
