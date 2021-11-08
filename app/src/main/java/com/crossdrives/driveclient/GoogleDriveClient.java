package com.crossdrives.driveclient;

import android.content.Context;
import android.util.Log;

import com.example.crossdrives.DriveServiceHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class GoogleDriveClient extends DriveClient {
    private static String TAG = "CD.GoogleDriveClient";
    private static Context mContext;

    static public GoogleDriveClient create(Context context, Object SignInAccount) {
        mContext = context;

        GoogleSignInAccount account = (GoogleSignInAccount)SignInAccount;

        if(account != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            context, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Cross Drive")
                            .build();

            if (googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");
            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any onClick actions.
            // We create DriveServiceHelper here but it will be used later by using getInstance() method
            //new DriveServiceHelper(googleDriveService);
            DriveServiceHelper.Create(googleDriveService);
        }

        return null;
    }

//    Builder builder = new Builder() {
//        private final GoogleDriveClient mGoogleDriveClient = new GoogleDriveClient();
//
//        @Override
//        public DriveClient build(Object auth) {
//
//            GoogleSignInAccount account = (GoogleSignInAccount)auth;
//
//            if(account != null) {
//                GoogleAccountCredential credential =
//                        GoogleAccountCredential.usingOAuth2(
//                                mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
//                credential.setSelectedAccount(account.getAccount());
//                Drive googleDriveService =
//                        new Drive.Builder(
//                                AndroidHttp.newCompatibleTransport(),
//                                new GsonFactory(),
//                                credential)
//                                .setApplicationName("Cross Drive")
//                                .build();
//
//                if (googleDriveService == null)
//                    Log.w(TAG, "googleDriveService is null!");
//                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
//                // Its instantiation is required before handling any onClick actions.
//                // We create DriveServiceHelper here but it will be used later by using getInstance() method
//                //new DriveServiceHelper(googleDriveService);
//                DriveServiceHelper.Create(googleDriveService);
//            }
//            return mGoogleDriveClient;
//        }
//    };
}
