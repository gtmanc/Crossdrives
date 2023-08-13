package com.example.crossdrives;

import static com.example.crossdrives.GlobalConstants.supporttedDriveClient;
import static com.example.crossdrives.GlobalConstants.supporttedSignin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.Infrastructure;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.OneDriveClient;

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

//    CompletableFuture<String> GoogleFuture = new CompletableFuture<>();
//    CompletableFuture<String> MicrosoftFuture = new CompletableFuture<>();
    HashMap<String, CompletableFuture<String>> Futures = new HashMap<>();
    final String IVALID_TOKEN = "";
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

        silenceSigninAsync();
    }

    /*
        TODO: Ticket #14
     */
    private CompletableFuture<HashMap<String, String>> silenceSigninAsync() {

        //Clean the sign in state
        //for(String b: mBrands){ mSignInState.put(b, false);}

        mProgressBar = findViewById(R.id.main_activity_progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        supporttedSignin.entrySet().stream().forEach((entry)->{
            entry.getValue().silenceSignIn(this, onSigninFinished);
            CompletableFuture<String> future = new CompletableFuture<>();
            Futures.put(entry.getKey(), future);
        });
//        SignInGoogle google = SignInGoogle.getInstance();
//        google.silenceSignIn(this, onSigninFinishedGdrive);
//        Futures.put(BRAND_GOOGLE, GoogleFuture);
//
//        SignInMS onedrive = SignInMS.getInstance();
//        onedrive.silenceSignIn(this, onSigninFinishedOnedrive);
//        Futures.put(BRAND_MS, MicrosoftFuture);
        CompletableFuture<HashMap<String, String>> joinFuture = CompletableFuture.supplyAsync(()->{
            Log.d(TAG, "Wait for silence signin results...");
            HashMap<String, String> tokenMap = Mapper.reValue(Futures, (f)->{
                String r = f.join();
                return r;});

            //Now we should get the result of silence sign in. Start to build infrastructure.
            //First of all, made input accepted by infrastructure builder
            Map<String, String> tokenMapReduced =
            tokenMap.entrySet().stream().filter(set->{
                return !set.getValue().equals(IVALID_TOKEN);
            }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

            Log.d(TAG, "Reduced tokenMap: " + tokenMapReduced);
            HashMap<String, IDriveClient> clients = Mapper.reValue(new HashMap<>(tokenMapReduced), (k,v)->{
                return supporttedDriveClient.get(k).build(v);
            });

            //We wait here until the infra builder finishes
            Infrastructure infrastructure = Infrastructure.getInstance();
            infrastructure.buildAsync(clients).join();

            //Show warning message for the not yet signed in brand
            Collection<String> failedBrands =
            getFailedSigninCollecton(mBrands, tokenMap);

            //https://stackoverflow.com/questions/3875184/cant-create-handler-inside-thread-that-has-not-called-looper-prepare
            this.runOnUiThread(()->{
                if(!failedBrands.isEmpty()) {
                    Log.d(TAG, "Sign in completed. Failed: " + String.join(", ",failedBrands));
                    Toast.makeText(getApplicationContext(),
                            "Sign in failed! Drive " + String.join(" ,", failedBrands), Toast.LENGTH_LONG).show();
                }
                mProgressBar.setVisibility(View.GONE);
                //Ready to go to the result list
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, QueryResultActivity.class);
                //          bundle.putStringArrayList("ResultList", mQueryFileName);
//          intent.putExtras(bundle);
                startActivity(intent);
            });

            return tokenMap;
        });

        joinFuture.exceptionally((e)->{
            Log.d(TAG, "Exception in join sign in result thread: " + e.getMessage());
            return null;
        });


//        GoogleFuture.exceptionally((ex)->{
//            Log.w(TAG, "Google silence sign in failed!");
//            return null;
//        });
//        MicrosoftFuture.exceptionally(ex->{
//            Log.w(TAG, "Microsoft silence sign in failed!");
//            return null;
//        });
        return joinFuture;
    }

    private Collection<String> getFailedSigninCollecton(List<String> brands, HashMap<String, String> tokenMap){
        //Log.d(TAG, "result: " + tokenMap);
        return brands.stream().filter((brand)->{
            String token;
            token = tokenMap.get(brand);
            return token.equals(IVALID_TOKEN) ? true : false;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    SignInManager.OnSignInfinished onSigninFinished = new SignInManager.OnSignInfinished(){

        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            String brand = profile.Brand;
            Log.d(TAG, "Sign in successfully: " + brand);

            IDriveClient dc = supporttedDriveClient.get(brand).build(token);

            CDFS.getCDFSService().addClient(brand, dc);
            CompletableFuture<String> future = Futures.get(brand);
            future.complete(token);
        }

        @Override
        public void onFailure(String brand, String err) {
            Log.w(TAG, "Sign in failed. Drive: " + brand + ". " + err);
            CompletableFuture<String> future = Futures.get(brand);
            future.complete(IVALID_TOKEN);
         }
    };

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



    SignInManager.OnSignInfinished onSigninFinishedOnedrive = new SignInManager.OnSignInfinished(){
        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            String brand = profile.Brand;

//            IDriveClient dc = (IDriveClient)supporttedDriveClient.get(brand);
//            dc = dc.build(token);
//            CDFS.getCDFSService().addClient(brand, dc);
            Futures.get(profile.Brand).complete(token);

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
        CDFS.getCDFSService().addClient(GlobalConstants.BRAND_GOOGLE, gdc);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    void addOneDriveClient(String token){
        OneDriveClient odc =
                (OneDriveClient) OneDriveClient.builder(token).buildClient();
        CDFS.getCDFSService().addClient(GlobalConstants.BRAND_MS, odc);
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



