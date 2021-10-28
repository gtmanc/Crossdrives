package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.InputStream;
import java.util.Collections;

import retrofit2.http.Url;

//A concrete object for execution of google sign in flow
public class SignInGoogle extends SignInManager{
    private static String TAG = "CD.SignInGoogle";
    private static SignInGoogle mSignInGoogle = null;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount mGoogleSignInAccount;
    private Context mContext = null;
    //Activity mActivity;
    private static Profile mProfile = new Profile();
    private Fragment mFragment;
    static OnInteractiveSignInfinished mCallback;
    static OnSignOutFinished mSignoutCallback;
    OnPhotoDownloaded mPhotoDownloadCallback;
    private Bitmap mBmp;
    private Object mObject;

    SignInGoogle(Context context)
    {
        mContext = context;
    }

    public static SignInGoogle getInstance(Context context){
        if(mSignInGoogle == null){
            //Log.d(TAG, "Create instance");
            mSignInGoogle = new SignInGoogle(context);
        }
//        Log.d(TAG, "instance is " + mSignInGoogle);
//        Log.d(TAG, "mProfile is " + mProfile);
        return mSignInGoogle;
    }

    //A static class used to exchange data between this class and the interactive sign in fragment
    public static class ReceiveSigninResult {
        public static void onSignedIn(int statuscode, Fragment fragment, GoogleSignInAccount account){
            int code = SignInManager.RESULT_FAILED;

            //Reset profile content first of all
            mProfile.Brand= SignInManager.BRAND_GOOGLE;
            mProfile.Name= "NoName";
            mProfile.Mail = "";
            mProfile.PhotoUri = null;

            if(statuscode == GoogleSignInStatusCodes.SUCCESS) {

                if(mProfile == null){
                    Log.w(TAG, "mProfile is null!");
                }
                mProfile.Name = account.getDisplayName();
                mProfile.Mail = account.getEmail();
                mProfile.PhotoUri = account.getPhotoUrl();
            }

            NavDirections a = GoogleSignInFragmentDirections.backToAddAccountFragment();
            NavHostFragment.findNavController(fragment).navigate(a);

            //Translate google code to sign to auth interface ones.
            if(statuscode == GoogleSignInStatusCodes.SUCCESS) {
                code = SignInManager.RESULT_SUCCESS;
            }

            mCallback.onFinished(code, mProfile, account);
        }
    }

    /*
       A fragment is used to perform the sign in interactive sign in flow. The most of
       the logic is implemented in the fragment.
       A static class ReceiveSigninResult which is used to exchange data between this class and the fragment.
     */
    @Override
    boolean Start(View view, OnInteractiveSignInfinished callback) {
//        Intent signInIntent;
//        GoogleSignInAccount account = null;
//        Drive googleDriveService = null;

        mCallback = callback;

//        // Configure sign-in to request the user's ID, email address, and basic
//        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
//                .build();
//
//        // Build a GoogleSignInClient with the options specified by gso.
//        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
//
//        signInIntent = mGoogleSignInClient.getSignInIntent();

        Log.d(TAG, "navigate to google sign in fragment");
        mFragment = FragmentManager.findFragment(view);

        NavDirections a = AddAccountFragmentDirections.navigteToGoogleSigninFragment();
        NavHostFragment.findNavController(mFragment).navigate(a);
        return true;
    }


//    @Override
//    Profile HandleSigninResult(Intent data) {
//        GoogleSignInAccount account = null;
//
//        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//        try {
//            account = task.getResult(ApiException.class);
//
//        }catch (ApiException e) {
//                // The ApiException status code indicates the detailed failure reason.
//                // Please refer to the GoogleSignInStatusCodes class reference for more information.
//                Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
//                mProfile = null;
//        }
//
//        if(account != null) {
//            GoogleAccountCredential credential =
//                    GoogleAccountCredential.usingOAuth2(
//                            mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
//            credential.setSelectedAccount(account.getAccount());
//            Drive googleDriveService =
//                    new Drive.Builder(
//                            AndroidHttp.newCompatibleTransport(),
//                            new GsonFactory(),
//                            credential)
//                            .setApplicationName("Drive API Migration")
//                            .build();
//
//            if(googleDriveService == null)
//                Log.w(TAG, "googleDriveService is null!");
//            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
//            // Its instantiation is required before handling any onClick actions.
//            // We create DriveServiceHelper here but it will be used later by using getInstance() method
//            //new DriveServiceHelper(googleDriveService);
//            DriveServiceHelper.Create(googleDriveService);
//            mProfile.Name= account.getDisplayName();
//            mProfile.Mail = account.getEmail();
//            mProfile.PhotoUri = account.getPhotoUrl();
//        }
//        return mProfile;
//    }

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

        //Reset profile content first of all
        mProfile.Brand= SignInManager.BRAND_GOOGLE;
        mProfile.Name= "NoName";
        mProfile.Mail = "";
        mProfile.PhotoUri = null;

        Task<GoogleSignInAccount> task = mGoogleSignInClient.silentSignIn();
        if (task.isSuccessful()) {
            Log.d(TAG, "User's credentials still cached");
            // There's immediate result available.
            mGoogleSignInAccount = task.getResult();
            mProfile.Name= mGoogleSignInAccount.getDisplayName();
            mProfile.Mail = mGoogleSignInAccount.getEmail();
            mProfile.PhotoUri = mGoogleSignInAccount.getPhotoUrl();
            //requestDriveService(mGoogleSignInAccount);
            callback.onFinished(GoogleSignInStatusCodes.SUCCESS, mProfile, mGoogleSignInAccount);
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            Log.w(TAG, "Silence sign in start...");
            task.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                GoogleSignInClient signInClient = null;
                GoogleSignInAccount signInAccount = null;

                @Override
                public void onComplete(Task<GoogleSignInAccount> task) {
                    try {
                        Log.w(TAG, "Silence sign in done!");
                        signInAccount = task.getResult(ApiException.class);
                        mProfile.Name= signInAccount.getDisplayName();
                        mProfile.Mail = signInAccount.getEmail();
                        mProfile.PhotoUri = signInAccount.getPhotoUrl();
                        //requestDriveService(signInAccount);
                        callback.onFinished(GoogleSignInStatusCodes.SUCCESS, mProfile, signInAccount);
                    } catch (ApiException apiException) {
                        Log.w(TAG, "Sign in failed! Error code: " + apiException.getStatusCode());
                        // You can get from apiException.getStatusCode() the detailed error code
                        // e.g. GoogleSignInStatusCodes.SIGN_IN_REQUIRED means user needs to take
                        // explicit action to finish sign-in;
                        // Please refer to GoogleSignInStatusCodes Javadoc for detail
                        callback.onFinished(SignInManager.RESULT_FAILED, null, null);
                    }
                }
            });
        }
    }

//    private void requestDriveService(GoogleSignInAccount account){
//        if(account != null) {
//            GoogleAccountCredential credential =
//                    GoogleAccountCredential.usingOAuth2(
//                            mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
//            credential.setSelectedAccount(account.getAccount());
//            Drive googleDriveService =
//                    new Drive.Builder(
//                            AndroidHttp.newCompatibleTransport(),
//                            new GsonFactory(),
//                            credential)
//                            .setApplicationName("Drive API Migration")
//                            .build();
//
//            if (googleDriveService == null)
//                Log.w(TAG, "googleDriveService is null!");
//            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
//            // Its instantiation is required before handling any onClick actions.
//            // We create DriveServiceHelper here but it will be used later by using getInstance() method
//            //new DriveServiceHelper(googleDriveService);
//            DriveServiceHelper.Create(googleDriveService);
//        }
//    }

    // example code for handling sign-out: https://developers.google.com/identity/sign-in/android/disconnect?hl=zh-TW
    // Google offcial: https://developers.google.com/identity/sign-in/android/disconnect
    @Override
    void SignOut(OnSignOutFinished callback) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        mSignoutCallback = callback;
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso);
        Log.d(TAG, "Sign out");
        Task<Void> pendingResult = mGoogleSignInClient.signOut();
        pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Sign out OK!");
                revokeAccess(mGoogleSignInClient);
            }
        });
    }

    @Override
    void getPhoto(Object object, OnPhotoDownloaded callback) {
        mPhotoDownloadCallback = callback;
        mObject = object;
        new DownloadPhoto()
                .execute(mProfile.PhotoUri.toString());
    }

    private void revokeAccess(GoogleSignInClient signInClient) {
        Task<Void> pendingResult = signInClient.revokeAccess();
        pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "revoke OK!");
                mSignoutCallback.onFinished(SignInManager.RESULT_SUCCESS);
            }
        });
    }



    private class DownloadPhoto extends AsyncTask<String, Void, Bitmap> {
//        ImageView mImageView;
//        public DownloadPhoto(ImageView iv) {
//            mImageView = iv;
//        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap bm = null;
            try{
                InputStream in = new java.net.URL(urls[0]).openStream();
                bm = BitmapFactory.decodeStream(in);
            }catch(Exception e){
                Log.w(TAG, "Open URL failed");
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
//            super.onPostExecute(bitmap);
//            mImageView.setImageBitmap(bitmap);
            mPhotoDownloadCallback.onDownloaded(bitmap, mObject);
        }
    }
}

