package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.msgraph.SharedPrefsUtil;
import com.crossdrives.msgraph.SnippetApp;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SignInMS extends SignInManager{
    private String TAG = "CD.SignInMS";
    private static SignInMS mSignInMS = null;
    private Context mContext;
    private Activity mActivity;
    ISingleAccountPublicClientApplication mSingleAccountApp;
    //https://docs.microsoft.com/en-us/graph/permissions-reference?context=graph%2Fapi%2F1.0&view=graph-rest-1.0
    private final static String[] SCOPES = {"Files.ReadWrite.All", "Files.ReadWrite.AppFolder"};
    //private final static List<String> scopes = new ArrayList<>();
    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";
    OnSignInfinished mOnSignInfinished;
    OnPhotoDownloaded mPhotoDownloadCallback;
    Profile mProfile = new Profile();
    private String mToken;
    private Object mObject;

    public SignInMS(){; mContext = SnippetApp.getAppContext(); //scopes.add("Files.Read");
    }

    public static SignInMS getInstance(){
        if(mSignInMS == null){
            mSignInMS = new SignInMS();
        }
        return mSignInMS;
    }

    @Override
    boolean Start(Activity activity, OnSignInfinished callback) {
        mOnSignInfinished = callback;
        mActivity = activity;
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
                        callback.onFailure(SignInManager.BRAND_MS, exception.toString());
                    }
                });

        return true;
    }

    @Override
    void silenceSignIn(Activity activity, OnSignInfinished callback) {
        mOnSignInfinished = callback;
        mActivity = activity;
        PublicClientApplication.createSingleAccountPublicClientApplication(mContext,
                R.raw.auth_config_single_account, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        Log.d(TAG, "OnCreated");
                        mSingleAccountApp = application;
                        mSingleAccountApp.acquireTokenSilentAsync(SCOPES, AUTHORITY, getAuthSilentCallback());
                    }
                    @Override
                    public void onError(MsalException exception) {
                        Log.w(TAG, "signInResult:failed! " + exception.toString());
                        mOnSignInfinished.onFailure(SignInManager.BRAND_MS, exception.toString());
                    }
                });

    }

    @Override
    void SignOut(OnSignOutFinished callback) {
        if (mSingleAccountApp == null){
            return;
        }
        mSingleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                callback.onFinished(SignInManager.RESULT_SUCCESS, SignInManager.BRAND_MS);
            }
            @Override
            public void onError(@NonNull MsalException exception){
                Log.w(TAG, "Signout Result: failed! " + exception.toString());
            }
        });
    }


    @Override
    void getPhoto(Object object, OnPhotoDownloaded callback) {
        mPhotoDownloadCallback = callback;
        mObject = object;

        //REST API reference for profile photo: https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
        //Build request: https://docs.microsoft.com/en-us/graph/sdks/create-requests?tabs=java
        GraphServiceClient graphClient =
                GraphServiceClient
                        .builder()
                        .authenticationProvider(new IAuthenticationProvider() {
                            /*
                                TODO: We cant guarantee the token is valid each time the client is created.
                                e.g. App is pushed to the background for longer than 5 minutes and
                                pulled to foreground afterwards.
                            */
                            @NonNull
                            @Override
                            public CompletableFuture<String> getAuthorizationTokenAsync(@NonNull URL requestUrl) {
                                CompletableFuture<String> future = null;
                                future = new CompletableFuture<>();
                                future.complete(mToken);
                                return future;
                            }

//                            @Override
//                            public void authenticateRequest(IHttpRequest request) {
//                                Log.d(TAG, "Authenticating request," + request.getRequestUrl());
//                                request.addHeader("Authorization", "Bearer " + accessToken);
//                            }
                        })
                        .buildClient();
        graphClient
                .me()
                .photo()
                .content()
                .buildRequest()
                //.get(new ICallback<InputStream>() {
                .getAsync()
                .thenAccept(inputStream -> {
                    mPhotoDownloadCallback.onDownloaded(BitmapFactory.decodeStream(inputStream), mObject);
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "IO exception: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {Log.d(TAG, "get photo failed: " + ex.toString()); return null;});
//                    @Override
//                    public void success(InputStream inputStream) {
//                        mPhotoDownloadCallback.onDownloaded(BitmapFactory.decodeStream(inputStream), mObject);
//                    }
//
//                    @Override
//                    public void failure(ClientException ex) {
//                        Log.d(TAG, "get photo failed, " + ex.toString());
//                    }
//                });
    }

    private void loadAccount(){
        if (mSingleAccountApp == null) {
            Log.w(TAG, "mSingleAccountApp is null!");
            mOnSignInfinished.onFailure(SignInManager.BRAND_MS, "Unknown failure!");
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
                    mOnSignInfinished.onFailure(SignInManager.BRAND_MS, "Account has changed. Not yet implemented.");
                }
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                //displayError(exception);
                Log.w(TAG, "signInResult:failed! Code=" + exception.toString());
                Log.w(TAG, " + exception.getMessage())");
                mOnSignInfinished.onFailure(SignInManager.BRAND_MS, exception.getMessage());
            }
        });
    }
    private AuthenticationCallback getAuthInteractiveCallback() {
        mProfile.Brand = SignInManager.BRAND_MS;
        mProfile.Name = "NoName";
        mProfile.Mail = "";
        mProfile.PhotoUri = null;
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
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
                //Log.d(TAG, "AccessToken : " + authenticationResult.getAccessToken());
                // save our auth token for REST API use later
                SharedPrefsUtil.persistAuthToken(authenticationResult);
                //Let's set the profile data using the information got. The missing filed will be set
                //in the getUser
                mProfile.Name = authenticationResult.getAccount().getUsername();
                mProfile.Mail = "";
                mProfile.PhotoUri = null;

                mToken = authenticationResult.getAccessToken();
                mOnSignInfinished.onFinished(mProfile, mToken);
                //MSGraphRestHelper msRest = new MSGraphRestHelper();


                /* call graph */
//                callGraphAPI(authenticationResult);
                //Give up on use of onedrive sdk since it seems obsolete. (the last update in github is about 6 year ago)
                //createOneDriveClient(mActivity, null);
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */
                Log.w(TAG, "Authentication failed: " + exception.toString());
                //displayError(exception);
                mOnSignInfinished.onFailure(SignInManager.BRAND_MS, exception.toString());
            }
            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.w(TAG, "User cancelled login.");
                mOnSignInfinished.onFailure(SignInManager.BRAND_MS, "User cancelled the sign in");
            }
        };
    }

    private SilentAuthenticationCallback getAuthSilentCallback() {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully silence authenticated");
                SignInManager.Profile profile = new Profile();
                profile.Name = authenticationResult.getAccount().getUsername();
                profile.Mail = "";
                profile.PhotoUri = null;
                profile.Brand = SignInManager.BRAND_MS;

                //callGraphAPI(authenticationResult);
                mToken = authenticationResult.getAccessToken();
                mOnSignInfinished.onFinished(profile, mToken);
            }
            @Override
            public void onError(MsalException exception) {
                Log.w(TAG, "Silence authentication failed: " + exception.toString());
                //displayError(exception);
                mOnSignInfinished.onFailure(SignInManager.BRAND_MS, "Silence authentication failed: " + exception.toString());
            }
        };
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
//    /**
//     * Used to setup the Services
//     * @param activity the current activity
//     * @param serviceCreated the callback
//     */
//    synchronized void createOneDriveClient(final Activity activity, final ICallback<Void> serviceCreated) {
//        final DefaultCallback<IOneDriveClient> callback = new DefaultCallback<IOneDriveClient>(activity) {
//            @Override
//            public void success(final IOneDriveClient result) {
//                if(result != null) {
//                    mClient.set(result);
//                    Log.w(TAG, "Create one drive client OK");
//                }else{
//                    Log.w(TAG, "Create one drive client failed");
//                }
//                //serviceCreated.success(null);
//            }
////            @Override
////            public void failure(final ClientException error) {
////                //serviceCreated.failure(error);
////                Log.d(TAG, "failure");
////            }
//        };
//        new OneDriveClient
//                .Builder()
//                .fromConfig(createConfig())
//                .loginAndBuildClient(activity, callback);
//    }

//    final MSAAuthenticator msaAuthenticator = new MSAAuthenticator() {
//        @Override
//        public String getClientId() {
//            return "afd432e7-01a1-47ab-8c37-5b487970f05c";
//        }
//
//        @Override
//        public String[] getScopes() {
//            return new String[] { "onedrive.appfolder" };
//        }
//    };
//
//    final ADALAuthenticator adalAuthenticator = new ADALAuthenticator() {
//        @Override
//        public String getClientId() {
//            return "afd432e7-01a1-47ab-8c37-5b487970f05c";
//        }
//
//        @Override
//        protected String getRedirectUrl() {
//            return "msauth://com.example.crossdrives/yuA%2BnLjqHb%2Blo8n78AI7ZAgEens%3D";
//        }
//    };

//    /**
//     * Create the client configuration
//     * @return the newly created configuration
//     */
//    private IClientConfig createConfig() {
//        final MSAAuthenticator msaAuthenticator = new MSAAuthenticator() {
//            @Override
//            public String getClientId() {
//                return "afd432e7-01a1-47ab-8c37-5b487970f05c";
//            }
//
//            @Override
//            public String[] getScopes() {
//                return new String[] {"onedrive.readwrite", "onedrive.appfolder", "wl.offline_access"};
//            }
//        };
//
//        final IClientConfig config = DefaultClientConfig.createWithAuthenticator(msaAuthenticator);
//        config.getLogger().setLoggingLevel(LoggerLevel.Debug);
//        return config;
//    }
}