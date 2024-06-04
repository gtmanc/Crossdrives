package com.crossdrives.signin.microsoft;

import android.content.Context;

import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.SignInMS;

/*
    The code is implemented based on the example Microsoft Graph sample Android app:
    https://github.com/microsoftgraph/msgraph-sample-android
 */
public class SignIn {
    Context mContext;
    static SignIn mSignIn;
    private AuthenticationHelper mAuthHelper = null;

    private SignIn(){;
        mContext = SnippetApp.getAppContext();

        AuthenticationHelper.getInstance(getApplicationContext())
                .thenAccept(authHelper -> {
                    mAuthHelper = authHelper;
                    if (!mIsSignedIn) {
                        doSilentSignIn(false);
                    } else {
                        hideProgressBar();
                    }
                })
                .exceptionally(exception -> {
                    Log.e("AUTH", "Error creating auth helper", exception);
                    return null;
                });
    }

    public static SignIn getInstance(){
        if(mSignIn == null){
            mSignIn = new SignIn();
        }
        return mSignIn;
    }

    // Prompt the user to sign in
    private void doInteractiveSignIn() {
        mAuthHelper.acquireTokenInteractively(this)
                .thenAccept(this::handleSignInSuccess)
                .exceptionally(exception -> {
                    handleSignInFailure(exception);
                    hideProgressBar();
                    return null;
                });
    }
}
