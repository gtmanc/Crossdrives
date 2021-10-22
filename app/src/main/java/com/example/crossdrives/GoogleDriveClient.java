package com.example.crossdrives;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.microsoft.graph.models.extensions.IGraphServiceClient;

import java.util.Collections;

public class GoogleDriveClient extends DriveClient{
    private String TAG = "CD.GoogleDriveClient";
    private Context mContext;

    public GoogleDriveClient(Context context) {
        mContext = context;
    }

    @Override
    GoogleDriveClient create(Object SignInAccount) {


        GoogleSignInAccount account = (GoogleSignInAccount)SignInAccount;

        if(account != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
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
}
