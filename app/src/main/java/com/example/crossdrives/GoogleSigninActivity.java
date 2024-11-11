package com.example.crossdrives;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

public class GoogleSigninActivity extends ComponentActivity {
    private String TAG = "CD.GoogleSigninActivity";
    private final int RC_SIGN_IN = 0;

    private int mSigninResult = GoogleSignInStatusCodes.SUCCESS;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleSignInClient GoogleSignInClient;
        Intent signInIntent;

        setContentView(R.layout.activity_google_signin);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
//                .build();

        // Configure sign-in to request offline access to the user's ID, basic
        // profile, and Google Drive. The first time you request a code you will
        // be able to exchange it for an access token and refresh token, which
        // you should store. In subsequent calls, the code will only result in
        // an access token. By asking for profile access (through
        // DEFAULT_SIGN_IN) you will also get an ID Token as a result of the
        // code exchange.
        String serverClientId = getString(R.string.server_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_FILE))
                .requestServerAuthCode(serverClientId)
                //.requestIdToken(serverClientId)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        GoogleSignInClient = GoogleSignIn.getClient(SnippetApp.getAppContext(), gso);

        signInIntent = GoogleSignInClient.getSignInIntent();

        //startActivityForResult(signInIntent, RC_SIGN_IN);
        startSigninActivity.launch(signInIntent);
    }

    ActivityResultLauncher<Intent> startSigninActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>(){
        @Override
        public void onActivityResult(ActivityResult result) {
            GoogleSignInAccount account = null;

            Log.d(TAG, "resultCode: " + result.getResultCode());


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

            if (result.getData() != null) {
                Log.d(TAG, "handle sign flow");
                account = HandleSigninResult(result.getData());

//            Log.d(TAG, "User name:" + mName);
//            Log.d(TAG, "User mail:" + mMail);
//            Log.d(TAG, "User photo url:" + mPhotoUri);
            } else {
                //Set the profile data to default
//            mName = "";
//            mMail = "";
//            mPhotoUri = null;
            }

            SignInGoogle.ReceiveSigninResult.onSignedIn(mSigninResult, /*this,*/ account);

            finish();
        }
    });

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
                Log.w(TAG, e.getMessage());
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
