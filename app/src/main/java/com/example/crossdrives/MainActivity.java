package com.example.crossdrives;

import static com.example.crossdrives.GlobalConstants.supporttedDriveClient;
import static com.example.crossdrives.GlobalConstants.supporttedSignin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.msgraph.SnippetApp;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class MainActivity extends AppCompatActivity{
    private String TAG = "CD.MainActivity";
    private ProgressBar mProgressBar = null;
    private final int MAX_BRAND = GlobalConstants.MAX_BRAND_SUPPORT;
    private final String BRAND_GOOGLE = GlobalConstants.BRAND_GOOGLE;
    private final String BRAND_MS = GlobalConstants.BRAND_MS;
    private Activity mActivity;

    private List<String> mBrands = GlobalConstants.BrandList;
    private HashMap<String, Boolean> mSignInState = new HashMap<>(); //0: Google, 1: Microsoft

    CompletableFuture<String> GoogleFuture = new CompletableFuture<>();
    CompletableFuture<String> MicrosoftFuture = new CompletableFuture<>();
    HashMap<String, CompletableFuture<String>> Futures = new HashMap<>();
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
        mActivity = this;

        silenceSignin();
    }

    /*
        TODO: Ticket #14
     */
    private void silenceSignin() {
        //Clean the sign in state
        //for(String b: mBrands){ mSignInState.put(b, false);}

        mProgressBar = findViewById(R.id.main_activity_progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        supporttedSignin.entrySet().stream().forEach((entry)->{
            Futures.put(entry.getKey(), CompletableFuture.supplyAsync(()->{
                entry.getValue().silenceSignIn(this, onSigninFinished);
                return null;
            }));

        });
//        SignInGoogle google = SignInGoogle.getInstance();
//        google.silenceSignIn(this, onSigninFinishedGdrive);
//        Futures.put(BRAND_GOOGLE, GoogleFuture);
//
//        SignInMS onedrive = SignInMS.getInstance();
//        onedrive.silenceSignIn(this, onSigninFinishedOnedrive);
//        Futures.put(BRAND_MS, MicrosoftFuture);
        Log.d(TAG, "Wait for silence results...");
        HashMap<String, String> tokenMap = Mapper.reValue(Futures, (f)->{
            return f.join();
        });
        Log.d(TAG, "Result got!");

        Collection<String> failed;
        failed = mBrands.stream().filter((brand)->{
            String token;
            token = tokenMap.get(brand);
            return token == null ? true : false;
        }).collect(Collectors.toCollection(ArrayList::new));

        Toast.makeText(getApplicationContext(),
                "Sign in failed! Drive " + String.join(" ,",failed), Toast.LENGTH_LONG).show();

        mProgressBar.setVisibility(View.GONE);
        //Ready to go to the result list
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, QueryResultActivity.class);
//                bundle.putStringArrayList("ResultList", mQueryFileName);
//                intent.putExtras(bundle);
        startActivity(intent);

        GoogleFuture.exceptionally((ex)->{
            Log.w(TAG, "Google silence sign in failed!");
            return null;
        });
        MicrosoftFuture.exceptionally(ex->{
            Log.w(TAG, "Microsoft silence sign in failed!");
            return null;
        });
    }

    SignInManager.OnSignInfinished onSigninFinishedGdrive = new SignInManager.OnSignInfinished(){

        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            //mSignInState.put(BRAND_GOOGLE, true);

                //Write user profile to database?


                Log.d(TAG, "Google silence sign in OK. Create google drive client...");
                //GoogleDriveClient google_drive = GoogleDriveClient.create(getApplicationContext(), object);
                addGoogleDriveClient(token);
                Futures.get(profile.Brand).complete(token);
                //GoogleFuture.complete(token);

            //ProceedNextScreen();
        }

        @Override
        public void onFailure(String brand, String err) {
            //mSignInState.put(BRAND_GOOGLE, true);
            //A short term workaround is used here. Show a toast message to prompt user that he has to be signed in.
            //Log.w(TAG, "Google silence sign in failed!");
            //Toast.makeText(getApplicationContext(), "Not yet signed in. Go to Master Account screen to perform the sign in process", Toast.LENGTH_LONG).show();
            //GoogleFuture.completeExceptionally(new Throwable(err));
            Futures.get(brand).completeExceptionally(new Throwable(err));
            //ProceedNextScreen();
        }
    };

    SignInManager.OnSignInfinished onSigninFinished = new SignInManager.OnSignInfinished(){

        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            String brand = profile.Brand;
            Log.d(TAG, "Signin successfully: " + brand);
            IDriveClient dc = (IDriveClient)supporttedDriveClient.get(brand);
            dc = dc.build(token);
            Futures.get(profile.Brand).complete(token);
        }

        @Override
        public void onFailure(String brand, String err) {
            Futures.get(brand).completeExceptionally(new Throwable(err));
        }
    };

    SignInManager.OnSignInfinished onSigninFinishedOnedrive = new SignInManager.OnSignInfinished(){
        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            String brand = profile.Brand;
            //mSignInState.put(BRAND_MS, true);
                //Write user profile to database
            IDriveClient dc = (IDriveClient)supporttedDriveClient.get(brand);
            dc = dc.build(token);
            CDFS.getCDFSService(getApplicationContext()).addClient(brand, dc);
//            GoogleDriveClient gdc
//                    (GoogleDriveClient) GoogleDriveClient.builder(token).buildClient();
            CDFS.getCDFSService(getApplicationContext()).addClient(brand, dc);
                //GraphDriveClient onedrive = new GraphDriveClient();
                //addOneDriveClient(token);
                //Log.d(TAG, "Onedrive silence sign in works");
                Futures.get(profile.Brand).complete(token);
                //MicrosoftFuture.complete(token);
            //ProceedNextScreen();
        }

        @Override
        public void onFailure(String brand, String err) {
            //mSignInState.put(BRAND_MS, true);
            //Log.w(TAG, "Onedrive silence sign in failed");
        //Toast.makeText(getApplicationContext(), "Not yet signed in. Go to Master Account screen to perform the sign in process", Toast.LENGTH_LONG).show();
            Futures.get(brand).completeExceptionally(new Throwable(err));
            //MicrosoftFuture.completeExceptionally(new Throwable(err));
            //ProceedNextScreen();
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
    private void addGoogleDriveClient(String token){
        GoogleDriveClient gdc =
                (GoogleDriveClient) GoogleDriveClient.builder(token).buildClient();
        CDFS.getCDFSService(getApplicationContext()).addClient(GlobalConstants.BRAND_GOOGLE, gdc);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    void addOneDriveClient(String token){
        OneDriveClient odc =
                (OneDriveClient) OneDriveClient.builder(token).buildClient();
        CDFS.getCDFSService(getApplicationContext()).addClient(GlobalConstants.BRAND_MS, odc);
//        oneDriveClient.
//                list().
//                buildRequest().
//                //select().
//                run(new ICallBack<FileList, Object>() {
//            @Override
//            public void success(FileList fileList, Object page) {
//
//            }
//
//            @Override
//            public void failure(String ex) {
//
//            }
//        });
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }
}



