package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

//A concrete object for execution of google sign in flow
public class SignInGoogle extends SignInManager{
    private static String TAG = "CD.SignInGoogle";
    private static SignInGoogle mSignInGoogle = null;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount mGoogleSignInAccount;
    private static Context mContext = null;
    private static Profile mProfile = new Profile();
    private static Activity mActivity;
    static OnSignInfinished mCallback;
    static OnSignOutFinished mSignoutCallback;
    OnPhotoDownloaded mPhotoDownloadCallback;
    private Bitmap mBmp;
    private Object mObject;
    private static final String CLIENT_SECRET_FILE = "raw/client_secret_web_backend.json";
    private static String mAccessToken;
    private final int RC_SIGN_IN = 0;
    private int mSigninResult = GoogleSignInStatusCodes.SUCCESS;

    SignInGoogle(Context context)
    {
        mContext = context;
    }

    public static SignInGoogle getInstance(){
        if(mSignInGoogle == null){
            //Log.d(TAG, "Create instance");
            mSignInGoogle = new SignInGoogle(SnippetApp.getAppContext());
        }
//        Log.d(TAG, "instance is " + mSignInGoogle);
//        Log.d(TAG, "mProfile is " + mProfile);
        return mSignInGoogle;
    }

    //A static class used to exchange data between this class and the interactive sign in activity
    public static class ReceiveSigninResult {
        public static void onSignedIn(int statuscode/*, Fragment fragment*/, GoogleSignInAccount account){
            int code = SignInManager.RESULT_FAILED;
            String accessToken;
            String Authcode;
            Intent intent = new Intent();

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

                Authcode = account.getServerAuthCode();
                if(Authcode != null) {
                    /*
                    android.os.NetworkOnMainThreadException will be thrown if an application attempts
                    to perform a networking operation on its main thread.
                    https://stackoverflow.com/questions/6343166/how-can-i-fix-android-os-networkonmainthreadexception
                    */
                    ExchangeToken exchangeToken = new ExchangeToken();
                    exchangeToken.execute(Authcode);
                }
            }else{
                mCallback.onFailure(SignInManager.BRAND_GOOGLE, "Encountered problem in sign in!");
            }

//            NavDirections a = GoogleSignInFragmentDirections.backToAddAccountFragment();
//            NavHostFragment.findNavController(fragment).navigate(a);

            //Translate google code to sign to auth interface ones.
//            if(statuscode == GoogleSignInStatusCodes.SUCCESS) {
//                code = SignInManager.RESULT_SUCCESS;
//            }
        }
    }

    static private String exchangeAccessToken(String authCode){
        if(authCode == null){
            Log.w(TAG, "authCode is null!");
            return null;
        }
        // Set path to the Web application client_secret_*.json file you downloaded from the
        // Google API Console: https://console.developers.google.com/apis/credentials
        // You can also find your Web application client ID and client secret from the
        // console and specify them directly when you create the GoogleAuthorizationCodeTokenRequest
        // object.
        GoogleTokenResponse tokenResponse = null;

        // Exchange auth code for access token
        GoogleClientSecrets clientSecrets = null;
        AssetFileDescriptor descriptor = null;
        BufferedReader reader = null;
        File secret = createSecret();
        String uri = "";
        if(secret != null) {

            //https://stackoverflow.com/questions/15912825/how-to-read-file-from-res-raw-by-name
            //https://stackoverflow.com/questions/4789325/android-path-to-asset-txt-file
            //https://www.geeksforgeeks.org/resource-raw-folder-in-android-studio/
//        try {
//            descriptor = mFragment.getActivity().getAssets().openFd(CLIENT_SECRET_FILE);
//            descriptor = mFragment.getActivity().getAssets().openFd("test.txt");
//            reader = new BufferedReader(new InputStreamReader(
//                    mFragment.getActivity().getAssets().open(CLIENT_SECRET_FILE)));
//        } catch (IOException e) {
//            Log.w(TAG, "Failed to open secret file!" + e.getMessage());
//        }


//        FileDescriptor fd = descriptor.getFileDescriptor();
//        FileReader fr = new FileReader();
//        char[] array = new char[1024];
//        int l;
//        try {
//             l = fr.read(array);
//            Log.d(TAG, "Read length: " + l + "Content: " + array.toString());
//        } catch (IOException e) {
//            Log.w(TAG, "File reader doenst work" + e.getMessage());
//        }

//        String mLine;
//        while (true) {
//            try {
//                if ((mLine = reader.readLine()) != null){
//                    Log.d(TAG, "Line read: " + mLine.toString());
//                }else{
//                    break;
//                }
//            } catch (IOException e) {
//                Log.w(TAG, "File reader doenst work" + e.getMessage());
//            }
//        }

            try {
                clientSecrets = GoogleClientSecrets.load(
                        GsonFactory.getDefaultInstance(), new FileReader(secret));
                //GsonFactory.getDefaultInstance(), new FileReader(descriptor.getFileDescriptor()));
            } catch (IOException e) {
                Log.w(TAG, "Failed to load client secret!" + e.getMessage());
            }

            try {
                Log.d(TAG, "ClientId: " +clientSecrets.getDetails().getClientId());
                Log.d(TAG, "ClientSecret: " +clientSecrets.getDetails().getClientSecret());
                tokenResponse =
                        new GoogleAuthorizationCodeTokenRequest(
                                new NetHttpTransport(),
                                GsonFactory.getDefaultInstance(),
                                "https://oauth2.googleapis.com/token",
                                clientSecrets.getDetails().getClientId(),
                                clientSecrets.getDetails().getClientSecret(),
                                authCode,
                                uri)   // Specify the same redirect URI that you use with your web
                                // app. If you don't have a web version of your app, you can
                                // specify an empty string.
                                .execute();
            } catch (IOException e) {
                Log.w(TAG, "Failed to get GoogleAuthorizationCodeToken!");
            }
        }
        else{
            Log.w(TAG, "Secret file is not available");
        }
        return tokenResponse.getAccessToken();
    }

    /*
    Since GoogleClientSecrets.load request FileReader, we create a file in app directory so that
    a File object can be used for FileReader.
     */
    static private File createSecret(){
        InputStream ins;
        InputStreamReader inr;
        BufferedReader br;
        //FileOutputStream fos;
        File f = null;
        FileOutputStream fOut = null;

        //To get a resoirce id:
        // getResources().getIdentifier("FILENAME_WITHOUT_EXTENSION", "raw", getPackageName());
        ins = mContext.getResources().openRawResource(R.raw.client_secret_web_backend);

        //f = new File(mContext.getFilesDir().toString() + "/Google_secret");
        f = new File(mContext.getFilesDir().toString(), "/Google_secret");

        Log.d(TAG, "Secret path: " +  f.toString());
        try {
            inr = new InputStreamReader(ins, "UTF-8");
            br = new BufferedReader(inr);
            Writer wr = new StringWriter();
            //fos = new FileOutputStream(f);
            fOut = mActivity.openFileOutput("Google_secret", Activity.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            char[] buf = new char[1024];
            int read;
            while ((read = br.read(buf)) != -1) {
                wr.write(buf, 0, read);
                Log.d(TAG, "Secret read: " + wr.toString());
                // Write the string to the file
                osw.write(wr.toString());
                /* ensure that everything is
                 * really written out and close */

                //fos.write(buf);
            }
            wr.close();
            osw.flush();
            osw.close();
            inr.close();
            br.close();
//            fos.flush();
//            fos.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to write secret! " + e.getMessage());
        }

        try {
            ins.close();
            fOut.close();
        } catch (IOException e) {
            Log.w(TAG, "Failed to close input stream! " + e.getMessage());
        }

        return f;
    }
    /*
       A activity is used to perform the sign in interactive sign in flow. The most of
       the logic is implemented in the activity: GoogleSigninActivity
       A static class ReceiveSigninResult which is used to exchange data between this class and the fragment.
     */
    @Override
    boolean Start(Activity activity, OnSignInfinished callback) {
        Intent signInIntent;

        mCallback = callback;
        mActivity = activity;

        Intent intent = new Intent();
        intent.setClass(SnippetApp.getAppContext(), GoogleSigninActivity.class);
        mActivity.startActivity(intent);

        Log.d(TAG, "navigate to google sign in fragment");

//        NavDirections a = AddAccountFragmentDirections.navigteToGoogleSigninFragment();
//        NavHostFragment.findNavController(mFragment).navigate(a);
        return true;
    }

    //Silence sign in. The account information and result will be provided via callback even if the user is already signed in.
    @Override
    void silenceSignIn(Activity activity, OnSignInfinished callback) {
        String Authcode = null;
        String serverClientId = mContext.getString(R.string.server_client_id);
        mCallback = callback;
        mActivity = activity;
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(serverClientId)
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
            Authcode = mGoogleSignInAccount.getServerAuthCode();
            if(Authcode != null) {
                    /*
                    android.os.NetworkOnMainThreadException will be thrown if an application attempts
                    to perform a networking operation on its main thread.
                    https://stackoverflow.com/questions/6343166/how-can-i-fix-android-os-networkonmainthreadexception
                    */
                ExchangeToken exchangeToken = new ExchangeToken();
                //call back will get called in the background thread
                exchangeToken.execute(Authcode);
            }
            //callback.onFinished(mProfile, mAccessToken);
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            Log.w(TAG, "Silence sign in start...");
            task.addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                GoogleSignInClient signInClient = null;
                GoogleSignInAccount signInAccount = null;

                @Override
                public void onComplete(Task<GoogleSignInAccount> task) {
                    String Authcode;
                    try {
                        Log.d(TAG, "Silence sign in done!");
                        signInAccount = task.getResult(ApiException.class);
                        Log.d(TAG, "getDisplayName: " + signInAccount.getDisplayName());
                        mProfile.Name= signInAccount.getDisplayName();
                        mProfile.Mail = signInAccount.getEmail();
                        mProfile.PhotoUri = signInAccount.getPhotoUrl();
                        Authcode = signInAccount.getServerAuthCode();
                        if(Authcode != null) {
                            /*
                            android.os.NetworkOnMainThreadException will be thrown if an application attempts
                            to perform a networking operation on its main thread.
                            https://stackoverflow.com/questions/6343166/how-can-i-fix-android-os-networkonmainthreadexception
                            */
                            ExchangeToken exchangeToken = new ExchangeToken();
                            //call back will get called in the background thread
                            exchangeToken.execute(Authcode);
                        }
                        //callback.onFinished(mProfile, signInAccount);
                    } catch (ApiException apiException) {
                        Log.w(TAG, "Sign in failed! Error code: " + apiException.getStatusCode());
                        // You can get from apiException.getStatusCode() the detailed error code
                        // e.g. GoogleSignInStatusCodes.SIGN_IN_REQUIRED means user needs to take
                        // explicit action to finish sign-in;
                        // Please refer to GoogleSignInStatusCodes Javadoc for detail
                        mCallback.onFailure(SignInManager.BRAND_GOOGLE, "Sign in failed! Error code: " + apiException.getStatusCode() +
                                apiException.getMessage());
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
        //#45 The line of code leads to crash
        Log.d(TAG, "Download photo with url: " + mProfile.PhotoUri.toString());
        new DownloadPhoto()
                .execute(mProfile.PhotoUri.toString());
    }

    private void revokeAccess(GoogleSignInClient signInClient) {
        Task<Void> pendingResult = signInClient.revokeAccess();
        pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "revoke OK!");
                mSignoutCallback.onFinished(SignInManager.RESULT_SUCCESS, SignInManager.BRAND_GOOGLE);
            }
        });
    }


    /*
    Async Task is deprecated. The reason could be found in https://www.techyourchance.com/asynctask-deprecated/
    */
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

    //1st String: authcode
    //2nd
    //3rd: String: accessToken
    static private class ExchangeToken extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... authcode) {
            String token;
            token = exchangeAccessToken(authcode[0]);
            Log.d(TAG, "Access Token: " + token);
            return token;
        }

        @Override
        protected void onPostExecute(String token) {
            Log.d(TAG, "call back with Access Token: " + token);
            mCallback.onFinished(mProfile, token);
        }
    }
}