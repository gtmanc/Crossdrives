package com.example.crossdrives;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity{
    GoogleSignInClient mGoogleSignInClient;
    //Request code used for OnActivityResult
    private static final int RC_SIGN_IN = 0;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 1; /* unique request id */
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private static final int RC_QUERY = 3;
    private static final int RC_DELETE_FILE = 4;
    private DriveServiceHelper mDriveServiceHelper;
    private String TAG = "CD.MainActivity";
    private ProgressBar mProgressBar = null;

    private ArrayList<String> mQueryFileId, mQueryFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


        silenceSignin();
    }

    private void silenceSignin() {
        SignInManager.Profile p = null;

        mProgressBar = findViewById(R.id.main_activity_progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        SignInGoogle google = SignInGoogle.getInstance(getApplicationContext());
        Log.d(TAG, "Google silence signin callback: " + onSigninFinishedGdrive);
        google.silenceSignIn(onSigninFinishedGdrive);

        SignInMS onedrive = SignInMS.getInstance(this);
        onedrive.silenceSignIn(onSigninFinishedOnedrive);

    }

    SignInManager.OnSilenceSignInfinished onSigninFinishedGdrive = new SignInManager.OnSilenceSignInfinished(){

        @Override
        public void onFinished(int result, SignInManager.Profile profile, Object object) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();

            mProgressBar.setVisibility(View.GONE);

            if(result == SignInManager.RESULT_SUCCESS){
                //Write user profile to database

                Log.d(TAG, "Google slience sign in OK. Ccreate google drive client");
                GoogleDriveClient google_drive = new GoogleDriveClient(getApplicationContext()).create(object);

                //Ready to go to the result list
                intent.setClass(MainActivity.this, QueryResultActivity.class);
                bundle.putStringArrayList("ResultList", mQueryFileName);
                intent.putExtras(bundle);
                startActivity(intent);
            }
            else{
                //A short term workaround is used here. Show a toast message to prompt user that he has to be signed in.
                Toast.makeText(getApplicationContext(), "Not yet signed in. Go to Master Account screen to perform the sign in process", Toast.LENGTH_LONG).show();
                intent.setClass(MainActivity.this, QueryResultActivity.class);
                bundle.putStringArrayList("ResultList", mQueryFileName);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        }
    };
    SignInManager.OnSilenceSignInfinished onSigninFinishedOnedrive = new SignInManager.OnSilenceSignInfinished(){
        @Override
        public void onFinished(int result, SignInManager.Profile profile, Object client) {
            //Ready to go to the result list
            if(result == GoogleSignInStatusCodes.SUCCESS){
                //Write user profile to database

                GraphDriveClient onedrive = new GraphDriveClient();
                Log.d(TAG, "Onedrive silence sign in works");
            }
            else{
                Log.w(TAG, "Onedrive silence sign in failed");
            }
        }
    };
}



