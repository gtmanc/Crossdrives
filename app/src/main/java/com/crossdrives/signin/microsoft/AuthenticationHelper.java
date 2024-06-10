package com.crossdrives.signin.microsoft;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AuthenticationHelper extends BaseAuthenticationProvider {

    private static AuthenticationHelper INSTANCE = null;
    private final static String[] mScopes = {"Files.ReadWrite.All", "Files.ReadWrite.AppFolder"};
    private ISingleAccountPublicClientApplication mPCA = null;
    private AuthenticationHelper(Context ctx, final IAuthenticationHelperCreatedListener listener) {
        PublicClientApplication.createSingleAccountPublicClientApplication(ctx, R.raw.msal_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mPCA = application;
                        listener.onCreated(INSTANCE);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e("AUTH_HELPER", "Error creating MSAL application", exception);
                        listener.onError(exception);
                    }
                });
    }
    public static synchronized CompletableFuture<AuthenticationHelper> getInstance(Context ctx) {

        if (INSTANCE == null) {
            CompletableFuture<AuthenticationHelper> future = new CompletableFuture<>();
            INSTANCE = new AuthenticationHelper(ctx, new IAuthenticationHelperCreatedListener() {
                @Override
                public void onCreated(AuthenticationHelper authHelper) {
                    future.complete(authHelper);
                }

                @Override
                public void onError(MsalException exception) {
                    future.completeExceptionally(exception);
                }
            });

            return future;
        } else {
            return CompletableFuture.completedFuture(INSTANCE);
        }
    }

    // Version called from fragments. Does not create an
    // instance if one doesn't exist
    public static synchronized AuthenticationHelper getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "AuthenticationHelper has not been initialized from MainActivity");
        }

        return INSTANCE;
    }

    public CompletableFuture<IAuthenticationResult> acquireTokenInteractively(Activity activity) {
        CompletableFuture<IAuthenticationResult> future = new CompletableFuture<>();
        SignInParameters parameters = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getAuthenticationCallback(future))
                .build();
        mPCA.signIn(parameters);

        return future;
    }
    @NonNull
    @Override
    public CompletableFuture<String> getAuthorizationTokenAsync(@NonNull URL requestUrl) {
        return null;
    }

    private AuthenticationCallback getAuthenticationCallback(
            CompletableFuture<IAuthenticationResult> future) {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                future.cancel(true);
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                future.complete(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                future.completeExceptionally(exception);
            }
        };
    }
}
