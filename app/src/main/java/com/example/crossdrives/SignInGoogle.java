package com.example.crossdrives;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

//A concrete object for execution of google sign in flow
public class SignInGoogle extends SignInManager{
    private String TAG = "CD.SignInGoogle";

    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInAccount mGoogleSignInAccount;
    Context mContext = null;
    Profile mProfile = new Profile();

    SignInGoogle(Context context)
    {
        mContext = context;
    }

    @Override
    Intent Prepare() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();

        return signInIntent;
    }

    @Override
    Profile HandleSigninResult(Intent data) {
        GoogleSignInAccount account = null;
        Profile p = mProfile;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            account = task.getResult(ApiException.class);

        }catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
                p = null;
        }

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
                            .setApplicationName("Drive API Migration")
                            .build();

            if(googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");
            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any onClick actions.
            // We create DriveServiceHelper here but it will be used later by using getInstance() method
            //new DriveServiceHelper(googleDriveService);
            DriveServiceHelper.Create(googleDriveService);
            p.Name= account.getDisplayName();
            p.Mail = account.getEmail();
            p.PhotoUri = account.getPhotoUrl();
        }
        return mProfile;
    }

    //Silence sign in. The account information and result will be provided via callback even if the user is already signed in.
    @Override
    void silenceSignIn(OnSilenceSignInfinished callback) {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);

        Task<GoogleSignInAccount> task = mGoogleSignInClient.silentSignIn();
        if (task.isSuccessful()) {
            Log.d(TAG, "User's credentials still cached");
            // There's immediate result available.
            mGoogleSignInAccount = task.getResult();
            mProfile.Name= mGoogleSignInAccount.getDisplayName();
            mProfile.Mail = mGoogleSignInAccount.getEmail();
            mProfile.PhotoUri = mGoogleSignInAccount.getPhotoUrl();
            requestDriveService(mGoogleSignInAccount);
            callback.onFinished(true, mProfile);
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            Log.w(TAG, "Silence sign in start...");
            task.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                @Override
                public void onComplete(Task<GoogleSignInAccount> task) {
                    try {
                        Log.w(TAG, "Silence sign in done!");
                        GoogleSignInAccount mGoogleSignInAccount = task.getResult(ApiException.class);
                        mProfile.Name= mGoogleSignInAccount.getDisplayName();
                        mProfile.Mail = mGoogleSignInAccount.getEmail();
                        mProfile.PhotoUri = mGoogleSignInAccount.getPhotoUrl();
                        requestDriveService(mGoogleSignInAccount);
                        callback.onFinished(true, mProfile);
                    } catch (ApiException apiException) {
                        Log.w(TAG, "Sign in failed! Error code: " + apiException.getStatusCode());
                        // You can get from apiException.getStatusCode() the detailed error code
                        // e.g. GoogleSignInStatusCodes.SIGN_IN_REQUIRED means user needs to take
                        // explicit action to finish sign-in;
                        // Please refer to GoogleSignInStatusCodes Javadoc for detail
                        mProfile = null;
                        callback.onFinished(false, null);
                    }
                }
            });
        }
    }

    private void requestDriveService(GoogleSignInAccount account){
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
                            .setApplicationName("Drive API Migration")
                            .build();

            if (googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");
            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any onClick actions.
            // We create DriveServiceHelper here but it will be used later by using getInstance() method
            //new DriveServiceHelper(googleDriveService);
            DriveServiceHelper.Create(googleDriveService);
        }
    }
}

