package com.example.crossdrives;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.Drive;
import com.microsoft.graph.models.extensions.IGraphServiceClient;

public class GoogleDriveClient extends DriveClient{

    @Override
    GoogleDriveClient create(Object client) {
        IGraphServiceClient msclient = (IGraphServiceClient)client;
        GoogleSignInAccount account = (GoogleSignInAccount)client;
        DriveServiceHelper.Create(account);
        return null;
    }
}
