package com.example.crossdrives;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class GoogleSignInFragment extends Fragment {
    private String TAG = "CD.GoogleSignInFragment";
    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInAccount mGoogleSignInAccount;
    private static final int RC_SIGN_IN = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.google_signin_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent signInIntent;
        Log.d(TAG, "onViewCreated");

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        signInIntent = mGoogleSignInClient.getSignInIntent();

        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SignInManager.Profile p;
        String name = null;
        AccountManager.AccountInfo ai= new AccountManager.AccountInfo();
        boolean result;

        Log.d(TAG, "requestCode: " + requestCode);
        Log.d(TAG, "resultCode: " + resultCode);

        //result code is 0 if user press BACK in the sign in activity. -1 is received if user entered signe in credentials.
        if(resultCode != 0) {
            Log.d(TAG, "handle sign flow");
            p = HandleSigninResult(data);
            if (p == null)
                Log.w(TAG, "handle result error!");

            Log.d(TAG, "User name:" + p.Name);
            Log.d(TAG, "User mail:" + p.Mail);
            Log.d(TAG, "User photo url:" + p.PhotoUri);

//            DBHelper dbh = new DBHelper(getContext(), null, null, 0);
//            r_id = dbh.insert("Google", p.Name, p.Mail, p.PhotoUri, "Activated");
//            if(r_id == -1){
//                Log.w(TAG, "Create account failed!");
//            }
            AccountManager am = AccountManager.getInstance();
            ai.brand = AccountManager.BRAND_GOOGLE;
            ai.name = p.Name;
            ai.mail = p.Mail;
            ai.photouri = p.PhotoUri;
            result = am.createAccount(getContext(), ai);
            if (result != true)
                Log.w(TAG, "Create account failed!");
            name = p.Name;
        }

        //passing name to master account fragment so that a toast is shown to the user that an account is created
        AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(name);
        //action.setCreateAccountName(p.Name);
        NavHostFragment.findNavController(mFragment).navigate((NavDirections) action);
    }

    SignInManager.Profile HandleSigninResult(Intent data) {
        GoogleSignInAccount account = null;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            account = task.getResult(ApiException.class);

        }catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            mProfile = null;
        }

        if(account != null) {
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            getContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build();

            if(googleDriveService == null)
                Log.w(TAG, "googleDriveService is null!");
            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any onClick actions.
            // We create DriveServiceHelper here but it will be used later by using getInstance() method
            //new DriveServiceHelper(googleDriveService);
            DriveServiceHelper.Create(googleDriveService);
            mProfile.Name= account.getDisplayName();
            mProfile.Mail = account.getEmail();
            mProfile.PhotoUri = account.getPhotoUrl();
        }
        return mProfile;
    }
}