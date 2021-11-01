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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity{
    private String TAG = "CD.MainActivity";
    private ProgressBar mProgressBar = null;
    private final int MAX_BRAND = GlobalConstants.MAX_BRAND_SUPPORT;
    private final String BRAND_GOOGLE = GlobalConstants.BRAND_GOOGLE;
    private final String BRAND_MS = GlobalConstants.BRAND_MS;

    //private boolean[] mSignInState = new boolean[MAX_BRAND];
    private HashMap<String, Boolean> mSignInState = new HashMap<>(); //0: Google, 1: Microsoft

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

        //Clean the sign in state
        mSignInState.put(BRAND_GOOGLE, false);
        mSignInState.put(BRAND_MS, false);

        mProgressBar = findViewById(R.id.main_activity_progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        SignInGoogle google = SignInGoogle.getInstance(getApplicationContext());
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
            mSignInState.put(BRAND_GOOGLE, true);

            if(result == SignInManager.RESULT_SUCCESS){
                //Write user profile to database?


                Log.d(TAG, "Google silence sign in OK. Create google drive client...");
                GoogleDriveClient google_drive = new GoogleDriveClient(getApplicationContext()).create(object);

                //Ready to go to the result list
                intent.setClass(MainActivity.this, QueryResultActivity.class);
//                bundle.putStringArrayList("ResultList", mQueryFileName);
//                intent.putExtras(bundle);
                startActivity(intent);
            }
            else{
                //A short term workaround is used here. Show a toast message to prompt user that he has to be signed in.
                Log.w(TAG, "Google silence sign in failed!");
                Toast.makeText(getApplicationContext(), "Not yet signed in. Go to Master Account screen to perform the sign in process", Toast.LENGTH_LONG).show();
                intent.setClass(MainActivity.this, QueryResultActivity.class);
//                bundle.putStringArrayList("ResultList", mQueryFileName);
//                intent.putExtras(bundle);
                startActivity(intent);
            }
        }
    };
    SignInManager.OnSilenceSignInfinished onSigninFinishedOnedrive = new SignInManager.OnSilenceSignInfinished(){
        @Override
        public void onFinished(int result, SignInManager.Profile profile, Object client) {
            mSignInState.put(BRAND_MS, true);
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

    private void ProceedNextScreen(){
        boolean state_google, state_ms;
        state_google = mSignInState.get(BRAND_GOOGLE);
        state_ms = mSignInState.get(BRAND_GOOGLE);
        if(state_google == true && state_ms == true){
            Log.d(TAG, "Sign in results all got. Process to QueryResultScreen...");
        }
    }
}



