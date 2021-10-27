package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.msgraph.MSGraphRestHelper;
import com.crossdrives.msgraph.SharedPrefsUtil;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

public class SignInMS extends SignInManager{
    private String TAG = "CD.SignInMS";
    private static SignInMS mSignInMS = null;
    private Context mContext;
    private Activity mActivity;
    ISingleAccountPublicClientApplication mSingleAccountApp;
    private final static String[] SCOPES = {"Files.Read"};
    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";
    OnInteractiveSignInfinished mOnInteractiveSignInfinished;
    OnSilenceSignInfinished mOnSilenceSignInfinished;
    Profile mProfile = new Profile();
    private String mToken;

    public SignInMS(Activity activity){mActivity = activity; mContext = mActivity.getApplicationContext();}

    public static SignInMS getInstance(Activity activity){
        if(mSignInMS == null){
            mSignInMS = new SignInMS(activity);
        }
        return mSignInMS;
    }

    @Override
    boolean Start(View view, OnInteractiveSignInfinished callback) {
        mOnInteractiveSignInfinished = callback;
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

        return true;
    }

    @Override
    void silenceSignIn(OnSilenceSignInfinished callback) {
        mOnSilenceSignInfinished = callback;
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
                callback.onFinished(SignInManager.RESULT_SUCCESS);
            }
            @Override
            public void onError(@NonNull MsalException exception){
                Log.w(TAG, "Signout Result: failed! " + exception.toString());
            }
        });
    }

    @Override
    void getPhoto(Object object, OnPhotoDownloaded callback) {
        return;
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
                Log.d(TAG, "ID: " + authenticationResult.getAccount().getId());
                Log.d(TAG, "User name : " + authenticationResult.getAccount().getUsername());
                Log.d(TAG, "Account : " + authenticationResult.getAccount().toString());
                Log.d(TAG, "Authority : " + authenticationResult.getAccount().getAuthority());
                Log.d(TAG, "AccessToken : " + authenticationResult.getAccessToken());
                // save our auth token for REST API use later
                SharedPrefsUtil.persistAuthToken(authenticationResult);
                mProfile.Brand = SignInManager.BRAND_MS;
                mProfile.Name = authenticationResult.getAccount().getUsername();
                mProfile.Mail = "";
                mProfile.PhotoUri = null;

                mToken = authenticationResult.getAccessToken();
                mOnInteractiveSignInfinished.onFinished(SignInManager.RESULT_SUCCESS, mProfile, mToken);
                MSGraphRestHelper msRest = new MSGraphRestHelper();

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
                mOnInteractiveSignInfinished.onFinished(SignInManager.RESULT_FAILED, mProfile, null);
            }
            @Override
            public void onCancel() {
                /* User canceled the authentication */
                Log.w(TAG, "User cancelled login.");
                mOnInteractiveSignInfinished.onFinished(SignInManager.RESULT_FAILED, mProfile, null);
            }
        };
    }

    private SilentAuthenticationCallback getAuthSilentCallback() {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully silence authenticated");

                //callGraphAPI(authenticationResult);
                mToken = authenticationResult.getAccessToken();
                mOnSilenceSignInfinished.onFinished(SignInManager.RESULT_SUCCESS, null, mToken);
            }
            @Override
            public void onError(MsalException exception) {
                Log.w(TAG, "Silence authentication failed: " + exception.toString());
                //displayError(exception);
                mOnSilenceSignInfinished.onFinished(SignInManager.RESULT_FAILED, null, null);
            }
        };
    }

    //REST API reference for profile photo: https://docs.microsoft.com/en-us/graph/api/profilephoto-get?view=graph-rest-1.0
    //Build request: https://docs.microsoft.com/en-us/graph/sdks/create-requests?tabs=java
    private void getMePhoto(IAuthenticationResult authenticationResult) {

        final String accessToken = mToken;

        IGraphServiceClient graphClient =
                GraphServiceClient
                        .builder()
                        .authenticationProvider(new IAuthenticationProvider() {
                            @Override
                            public void authenticateRequest(IHttpRequest request) {
                                Log.d(TAG, "Authenticating request," + request.getRequestUrl());
                                request.addHeader("Authorization", "Bearer " + accessToken);
                            }
                        })
                        .buildClient();
        graphClient
                .me()
                .drive()
                .buildRequest()
                .get(new ICallback<Drive>() {
                    @Override
                    public void success(final Drive drive) {
                        Log.d(TAG, "Found Drive " + drive.id);
                        //displayGraphResult(drive.getRawObject());
                        Log.d(TAG, "Raw Object: " + drive.getRawObject());

                    }

                    @Override
                    public void failure(ClientException ex) {
                        //displayError(ex);
                        Log.w(TAG, "callGraphAPI failed: " + ex.toString());

                    }
                });
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