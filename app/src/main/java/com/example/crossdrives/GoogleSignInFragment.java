package com.example.crossdrives;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class GoogleSignInFragment extends Fragment {
    private String TAG = "CD.GoogleSignInFragment";
    GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 0;
//    private String mName, mMail;
//    private Uri mPhotoUri;
//    private Fragment mFragment;

    private int mSigninResult = GoogleSignInStatusCodes.SUCCESS;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.google_signin_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent signInIntent;
        Log.d(TAG, "onViewCreated");

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        signInIntent = mGoogleSignInClient.getSignInIntent();

//        mFragment = FragmentManager.findFragment(view);

        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        GoogleSignInAccount account = null;

        Log.d(TAG, "requestCode: " + requestCode);
        Log.d(TAG, "resultCode: " + resultCode);

        /*
        HTC One Android 6:
        The sign in activity is always shown even if app is already signed in
        Result code is 0 if the user
        1. enter sign in credentials (user decides to use a new account has never be signed on the device)
        2. user selects an account that has signed in on the device
        3. Press BACK in the sign in activity.
        Not yet clean in which case a NOT zero is returned.
        Samsung Gallxy S Android 6
        THe signin activity is not shown if app is signed in.
        1. resultcode:-1: enter sign in credentials (user decides to use a new account has never be signed on the device)
        2. resultcode:-1: user selects an account that has signed in on the device
        3. resultrcode is 0 if user press BACK in the sign in activity.
        Samsung A70 Android 9
        TBD
        */
        //According to the observed behavior, it's hard determine the sign in result with resultCode.
        //Use Intent as an alternative solution

        if(data != null) {
            Log.d(TAG, "handle sign flow");
            account = HandleSigninResult(data);

//            Log.d(TAG, "User name:" + mName);
//            Log.d(TAG, "User mail:" + mMail);
//            Log.d(TAG, "User photo url:" + mPhotoUri);
        }
        else{
            //Set the profile data to default
//            mName = "";
//            mMail = "";
//            mPhotoUri = null;
        }

        SignInGoogle.ReceiveSigninResult.onSignedIn(mSigninResult, this, account);
    }

    GoogleSignInAccount HandleSigninResult(Intent data) {
        GoogleSignInAccount account = null;

        mSigninResult = GoogleSignInStatusCodes.SUCCESS;

        if (data == null) {
            mSigninResult = GoogleSignInStatusCodes.SIGN_IN_FAILED;
        } else {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                account = task.getResult(ApiException.class);

            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                // Error code :12501 if user gives up sign in. e.g. press back key in signin screen
                Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
                mSigninResult = e.getStatusCode();
            }

//            if (account != null) {
//                GoogleAccountCredential credential =
//                        GoogleAccountCredential.usingOAuth2(
//                                getContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
//                credential.setSelectedAccount(account.getAccount());
//                Drive googleDriveService =
//                        new Drive.Builder(
//                                AndroidHttp.newCompatibleTransport(),
//                                new GsonFactory(),
//                                credential)
//                                .setApplicationName("Drive API Migration")
//                                .build();
//
//                if (googleDriveService == null){
//                    Toast.makeText(getContext(), "Request drive error!", Toast.LENGTH_LONG).show();
//                    Log.w(TAG, "googleDriveService is null!");
//                }

                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                // Its instantiation is required before handling any onClick actions.
                // We create DriveServiceHelper here but it will be used later by using getInstance() method
                //new DriveServiceHelper(googleDriveService);
//                DriveServiceHelper.Create(googleDriveService);

//            mName = account.getDisplayName();
//            mMail = account.getEmail();
//            mPhotoUri = account.getPhotoUrl();
//            }
        }
        return account;
    }
}
