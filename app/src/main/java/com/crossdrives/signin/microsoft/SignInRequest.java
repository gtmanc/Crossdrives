package com.crossdrives.signin.microsoft;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.msgraph.SharedPrefsUtil;
import com.crossdrives.msgraph.SnippetApp;
import com.crossdrives.signin.IPhotoDownloadedListener;
import com.crossdrives.signin.ISignInFinihedListener;
import com.crossdrives.signin.ISignInRequest;
import com.crossdrives.signin.ISignOutFinishedListener;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalServiceException;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class SignInRequest implements ISignInRequest {
    private String TAG = "Signin.MS.SignInRequest";
    static SignInRequest mSignInRequest = null;
    Context mContext = null;
    CompletableFuture<AuthenticationHelper> AuthenticationHelperCreatedFuture = null;
    private AuthenticationHelper mAuthHelper = null;
    private Activity mActivity = null;

    private ISignInFinihedListener mSignInFinishedListener;
    public SignInRequest(){;
        mContext = SnippetApp.getAppContext();
        AuthenticationHelperCreatedFuture = AuthenticationHelper.getInstance(mContext);
    }
    public static SignInRequest getInstance(){
        if(mSignInRequest == null){
            mSignInRequest = new SignInRequest();
        }
        return mSignInRequest;
    }
    @Override
    public boolean interactivelySignIn(Activity activity, ISignInFinihedListener callback) {
        mAuthHelper = AuthenticationHelperCreatedFuture.join();
        mActivity = activity;
        mSignInFinishedListener = callback;
        doInteractiveSignIn();
        return false;
    }

    @Override
    public void silenceSignIn(Activity activity, ISignInFinihedListener callback) {

    }

    @Override
    public void SignOut(ISignOutFinishedListener callback) {

    }

    @Override
    public void getPhoto(Object object, IPhotoDownloadedListener callback) {

    }

    // Prompt the user to sign in
    private void doInteractiveSignIn() {
        mAuthHelper.acquireTokenInteractively(mActivity)
                .thenAccept(this::handleSignInSuccess)
                .exceptionally(exception -> {
                    handleSignInFailure(exception);
                    return null;
                });
    }
    // Handles the authentication result
    private void handleSignInSuccess(@NonNull IAuthenticationResult authenticationResult) {
        // Log the token for debug purposes
        String accessToken = authenticationResult.getAccessToken();
        Profile profile = new Profile();
        //Log.d("AUTH", String.format("Access token: %s", accessToken));

        // Get Graph client and get user
//        GraphHelper graphHelper = GraphHelper.getInstance();
//        graphHelper.getUser()
//                .thenAccept(user -> {
//                    mUserName = user.displayName;
//                    mUserEmail = user.mail == null ? user.userPrincipalName : user.mail;
//                    mUserTimeZone = (user.mailboxSettings == null || user.mailboxSettings.timeZone == null)
//                            ? "UTC": user.mailboxSettings.timeZone;
//
//                    runOnUiThread(() -> {
//                        hideProgressBar();
//                        setSignedInState(true);
//                        openHomeFragment(mUserName);
//                    });
//                })
//                .exceptionally(exception -> {
//                    Log.e("AUTH", "Error getting /me", exception);
//
//                    runOnUiThread(()-> {
//                        hideProgressBar();
//                        setSignedInState(false);
//                    });
//
//                    return null;
//                });

        /* Successfully got a token, use it to call a protected resource - MSGraph */
        Log.d(TAG, "Successfully authenticated");
        /* Update UI */
        //updateUI(authenticationResult.getAccount());
        Log.d(TAG, "ID: " + authenticationResult.getAccount().getId());
        Log.d(TAG, "User name : " + authenticationResult.getAccount().getUsername());
        Log.d(TAG, "Account : " + authenticationResult.getAccount().toString());
        Log.d(TAG, "Authority : " + authenticationResult.getAccount().getAuthority());
        Log.d(TAG, "Tenant ID : " + authenticationResult.getTenantId());
        Arrays.stream(authenticationResult.getScope()).forEach((s)->{
            Log.d(TAG, "Scope : " + s);
        });
        Log.d(TAG, "AccessToken : " + authenticationResult.getAccessToken());
        // save our auth token for REST API use later
        SharedPrefsUtil.persistAuthToken(authenticationResult);
        //Let's set the profile data using the information got. The missing filed will be set
        //in the getUser
        profile.Name = authenticationResult.getAccount().getUsername();
        profile.Mail = "";
        profile.PhotoUri = null;

        mSignInFinishedListener.onFinished(profile, accessToken);
    }

    private void handleSignInFailure(Throwable exception) {
        if (exception instanceof MsalServiceException) {
            // Exception when communicating with the auth server, likely config issue
            Log.e(TAG, "Service error authenticating", exception);
        } else if (exception instanceof MsalClientException) {
            // Exception inside MSAL, more info inside MsalError.java
            Log.e(TAG, "Client error authenticating", exception);
        } else {
            Log.e(TAG, "Unhandled exception authenticating", exception);
        }
    }
}
