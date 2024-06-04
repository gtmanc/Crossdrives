package com.crossdrives.signin.microsoft;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AuthenticationHelper extends BaseAuthenticationProvider {
    private final static String[] mScopes = {"Files.ReadWrite.All", "Files.ReadWrite.AppFolder"};
    private ISingleAccountPublicClientApplication mPCA = null;

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
