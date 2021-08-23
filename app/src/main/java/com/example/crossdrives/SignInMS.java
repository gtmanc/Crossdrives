package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

public class SignInMS extends SignInManager{
    private String TAG = "CD.SignInMS";
    Context mContext;
    Activity mActivity;
    ISingleAccountPublicClientApplication mSingleAccountApp;
    private final static String[] SCOPES = {"Files.Read"};
    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";

    public SignInMS(Activity activity){mActivity = activity; mContext = mActivity.getApplicationContext();}

    @Override
    Intent Prepare() {
        PublicClientApplication.createSingleAccountPublicClientApplication(mContext,
                R.raw.auth_config_single_account, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        Log.d(TAG, "OnCreated");
                        mSingleAccountApp = application;
                        loadAccount();
                    }
                    @Override
                    public void onError(MsalException exception) {
                        Log.w(TAG, "signInResult:failed! " + exception.toString());
                    }
                });

        return null;
    }

    @Override
    Profile HandleSigninResult(Intent data) {
        return null;
    }

    @Override
    void silenceSignIn(OnSilenceSignInfinished callback) {

    }


    private void loadAccount(){
        if (mSingleAccountApp == null) {
            Log.w(TAG, "mSingleAccountApp is null!");
            return;
        }

        mSingleAccountApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                // You can use the account data to update your UI or your app database.
                //updateUI(activeAccount);
                Log.d(TAG, "onAccountLoaded");
                //Log.d(TAG, "ID: " + activeAccount.getId());
                mSingleAccountApp.signIn(mActivity, null, SCOPES, getAuthInteractiveCallback());
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
                    //performOperationOnSignOut();
                    Log.d(TAG, "onAccountChanged");
                }
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                //displayError(exception);
                Log.w(TAG, "signInResult:failed! Code=" + exception.toString());
                Log.w(TAG, " + exception.getMessage())");
            }
        });
    }
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated");
                /* Update UI */
                //updateUI(authenticationResult.getAccount());
                /* call graph */
                //callGraphAPI(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.w(TAG, "Authentication failed: " + exception.toString());
                //displayError(exception);
            }
            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.w(TAG, "User cancelled login.");
            }
        };
    }
}