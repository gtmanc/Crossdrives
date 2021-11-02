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
import java.util.Map;


public class MainActivity extends AppCompatActivity{
    private String TAG = "CD.MainActivity";
    private ProgressBar mProgressBar = null;
    private final int MAX_BRAND = GlobalConstants.MAX_BRAND_SUPPORT;
    private final String BRAND_GOOGLE = GlobalConstants.BRAND_GOOGLE;
    private final String BRAND_MS = GlobalConstants.BRAND_MS;

    private List<String> mBrands = GlobalConstants.BrandList;
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
        for(String b: mBrands){ mSignInState.put(b, false);}

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

            mSignInState.put(BRAND_GOOGLE, true);

            if(result == SignInManager.RESULT_SUCCESS){
                //Write user profile to database?


                Log.d(TAG, "Google silence sign in OK. Create google drive client...");
                GoogleDriveClient google_drive = new GoogleDriveClient(getApplicationContext()).create(object);


            }
            else{
                //A short term workaround is used here. Show a toast message to prompt user that he has to be signed in.
                Log.w(TAG, "Google silence sign in failed!");
                Toast.makeText(getApplicationContext(), "Not yet signed in. Go to Master Account screen to perform the sign in process", Toast.LENGTH_LONG).show();
            }
            ProceedNextScreen();
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
            ProceedNextScreen();
        }
    };

    private void ProceedNextScreen(){
        boolean state_google, state_ms;
        Intent intent = new Intent();
        boolean AllResultGot = true;

        state_google = mSignInState.get(BRAND_GOOGLE);
        state_ms = mSignInState.get(BRAND_MS);
        for(Map.Entry state : mSignInState.entrySet()){
            Log.d(TAG, "state: " + state.getKey() + " " + (boolean)state.getValue());
            if((boolean)state.getValue() == false){
                AllResultGot = false;
            }
        }

        if(AllResultGot == true){
            Log.d(TAG, "Sign in results all got. Process to QueryResultScreen...");
            mProgressBar.setVisibility(View.GONE);
            //Ready to go to the result list
            intent.setClass(MainActivity.this, QueryResultActivity.class);
//                bundle.putStringArrayList("ResultList", mQueryFileName);
//                intent.putExtras(bundle);
            startActivity(intent);
        }
    }
}



