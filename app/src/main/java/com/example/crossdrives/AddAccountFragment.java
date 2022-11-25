package com.example.crossdrives;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

public class AddAccountFragment extends BaseFragment{
    private String TAG = "CD.AddAccountFragment";
    SignInManager mSignInManager = null;
    View mView;
    Activity mActivity;
    Lifecycle mLifecycle;

    //private Timer myTimer = new Timer();
    /*
        Maintenance of map between drive brand and index for add/remove CDFS client.
        It's observed 'static' must be used. Otherwise, the data in the map is lost each time the
        callback is called. It seems that fragment creates a new concrete map object each time the
        callback gets called.
     */
    //static HashMap<String, Integer> mDrives = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "OnCreated");
        Log.d(TAG, "lifecycle: " + getLifecycle().getCurrentState() + " " + getLifecycle());
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
//            Log.d(TAG, "Restore mDrives...");
//            mDrives = (HashMap)savedInstanceState.getSerializable("Drives");
//            Log.d(TAG, "done. mDrives: " + mDrives);
        }

        mLifecycle = getLifecycle();
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        setHasOptionsMenu(true);
        //Update activity object each time the fragment is shown. It will be used in callback called by background thread. e.g. onSigninFinished
        mActivity = getActivity();
        return inflater.inflate(R.layout.add_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated. View: " + view);
        view.findViewById(R.id.add_account_btn_google).setOnClickListener(listener_add_gdrive);
        view.findViewById(R.id.add_account_btn_ms).setOnClickListener(listener_add_onedrive);
        mView = view;

        requireActivity().getOnBackPressedDispatcher().addCallback(callback);

        Toolbar toolbar = view.findViewById(R.id.add_account_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);
    }

    private View.OnClickListener listener_add_gdrive = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "start sign flow");
            mSignInManager = SignInGoogle.getInstance();

            AccountManager.AccountInfo ai
                    = getActivatedAccount(GlobalConstants.BRAND_GOOGLE);
            if (ai != null) {
                //OK. Now we are sure that there is a activated google account. Ask user for next step.
                Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), SignOutDialog.class);
                //intent.putExtra("Brand", SignInManager.BRAND_GOOGLE);
                mStartForResult.launch(intent);
            } else {
                mSignInManager.Start(getActivity(), onSigninFinished);
            }
        }
    };

    private View.OnClickListener listener_add_onedrive = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "start sign flow");
            mSignInManager = SignInMS.getInstance();
            AccountManager.AccountInfo ai
                    = getActivatedAccount(GlobalConstants.BRAND_MS);
            if (ai != null) {
                //OK. Now we are sure that there is a activated Microsoft account. Ask user for next step.
                Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), SignOutDialog.class);
                //intent.putExtra("Brand", SignInManager.BRAND_MS);
                mStartForResult.launch(intent);
            } else {
                mSignInManager.Start(getActivity(), onSigninFinished);
            }
        }
    };

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "Callback gets called. Result code:" + result.getResultCode());
                    String action, brand;
                    //Result code could be altered: https://medium.com/mobile-app-development-publication/undocumented-startactivityforresult-behavior-for-fragment-b7b04d24a346
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        action = intent.getStringExtra(SignOutDialog.KEY_ACTION);
                        //brand = intent.getStringExtra("Brand");
                        Log.d(TAG, "Action:" + action);
                        if (action.equals(SignOutDialog.ACTION_SIGNOUT)) {
                            Log.i(TAG, "Decided: sign out!");
                            mSignInManager.SignOut(onSignOutFinished);
                        }
                    }
                }
            });

    SignInManager.OnSignOutFinished onSignOutFinished = new SignInManager.OnSignOutFinished(){
        @Override
        public void onFinished(int result, String brand) {
            boolean r;
            IDriveClient client;
            CDFS cdfs = CDFS.getCDFSService();
            if(result == SignInManager.RESULT_SUCCESS){
                boolean r_am = false;
                String brand_am = GlobalConstants.BRAND_GOOGLE;
                Log.d(TAG, "App has signed out!");

                AccountManager.AccountInfo ai;
                AccountManager am = AccountManager.getInstance();
                if(brand == GlobalConstants.BRAND_MS) {
                    brand_am = GlobalConstants.BRAND_MS;
                }
                ai = am.getAccountActivated(getContext(), brand_am);
                r_am = am.setAccountDeactivated(getContext(), ai.brand, ai.name, ai.mail);
                if(r_am != true){Log.w(TAG, "Set account deactivated not worked");}

                Log.d(TAG, "Brand: " + brand);
                //Log.d(TAG, "Drives map: " + mDrives);
                /*
                    Known issue is once app has signed out due to any of reasons (e.g. app data deletion
                    in setting), null is got instead of a index. Tracked in #8, #15, #16.
                 */
                //client = cdfs.getClient(brand);
                r = cdfs.removeClient(brand);
                if(!r) {Log.w(TAG, "Remove CDFS client failed!");}
            }

            mSignInManager.Start(getActivity(), onSigninFinished);

        }
    };
    /*
        A convenience method for get activated account using account manager
    */
    private AccountManager.AccountInfo getActivatedAccount(String brand) {
        AccountManager.AccountInfo ai;
        AccountManager am = AccountManager.getInstance();
        return am.getAccountActivated(getContext(), brand);
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            Fragment f = FragmentManager.findFragment(mView);

            // Handle the back button event
            Log.d(TAG, "Back button pressed!");
            //passing null to master account fragment to avoid showing the toast
            AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(null);
            //action.setCreateAccountName(p.Name);
            NavHostFragment.findNavController(f).navigate((NavDirections) action);

        }
    };

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_option, menu);

        menu.findItem(R.id.search).setVisible(false);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        //Because we only have a action button (close Button) is action bar, so simply go back to previous screen (master account)
        AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(null);
        //action.setCreateAccountName(p.Name);
        Fragment f = FragmentManager.findFragment(mView);
        NavHostFragment.findNavController(f).navigate((NavDirections) action);

        return super.onOptionsItemSelected(item);
    }

    SignInManager.OnSignInfinished onSigninFinished = new SignInManager.OnSignInfinished(){
        @Override
        public void onFinished(SignInManager.Profile profile, String token) {
            boolean err = false;

            //if(result == SignInManager.RESULT_SUCCESS) {
                if(profile.Brand == SignInManager.BRAND_GOOGLE){

                    Log.d(TAG, "User sign in OK. Start to create google drive client. Token: " + token);
                    /*
                        We cant guarantee that getActivity always return valid object. It is still dangerous
                        if mActivity is used.
                        https://stackoverflow.com/questions/6215239/getactivity-returns-null-in-fragment-function
                     */
                    GoogleDriveClient gdc =
                            (GoogleDriveClient) GoogleDriveClient.builder(token).buildClient();
                    Log.d(TAG, "Add CDFS client: " + gdc);
                    CDFS.getCDFSService().addClient(GlobalConstants.BRAND_GOOGLE, gdc);
                    //mDrives.put(GlobalConstants.BRAND_GOOGLE, i);
                }
                else if(profile.Brand == SignInManager.BRAND_MS)
                {
                    Log.d(TAG, "User sign in OK. Start to create one drive client");
                    OneDriveClient odc =
                            (OneDriveClient) OneDriveClient.builder((String) token).buildClient();

                    Log.d(TAG, "Add CDFS client: " + odc);
                    CDFS.getCDFSService().addClient(GlobalConstants.BRAND_MS, odc);
                    //mDrives.put(GlobalConstants.BRAND_MS, i);
                }
                else{
                    Log.w(TAG, "Unknown brand!");
                }

                createAccount(profile);

            /*
                The callback seems to be attached in the old fragment instance. Therefore, an error
                message says "view androidx.constraintlayout.widget.ConstraintLayout... does not have
                a NavController set "when we attempt to transit to previous fragment in the
                callback (onSigninFinished). This is because when we return from Google signin fragment
                (Now it is changed to an activity), this fragment (AddAccountFragment) is recreated since
                the view is destroyed. However, the callback is still attached to the old AddAccountFragment.

                When add One drive account button is pressed, a dialog is shown in front of this fragment,
                the call sequence is onPause->onSaveInstance->Stop
                When returned from sing-in flow, the call sequence is onStart -> onResume

                Back to previous screen: onPause->onStop->onDestroyView->onDestroy
                Observation:
                Encountered only once for Microsoft sign-in. Not yet clear how it happen:
                java.lang.IllegalArgumentException: Navigation action/destination com.example.crossdrives:id/navigate_back_to_master_account cannot be found from the current destination Destination(com.example.crossdrives:id/drawer_menu_item_master_account) class=com.example.crossdrives.MasterAccountFragment
            */
            //passing name to master account fragment so that a toast is shown to the user that an account is created
            AddAccountFragmentDirections.NavigateBackToMasterAccount action =
                            AddAccountFragmentDirections.navigateBackToMasterAccount(profile.Name);
            Log.d(TAG, "mView: " + mView);
            Navigation.findNavController(mView).navigate((NavDirections) action);

        }

        @Override
        public void onFailure(String brand, String err) {
            Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
        }
    };

    private void addGoogleDriveClient(GoogleSignInAccount account){

    }

    private void createAccount(SignInManager.Profile profile){
        AccountManager.AccountInfo ai= new AccountManager.AccountInfo();
        boolean err = false;

        AccountManager am = AccountManager.getInstance();
        if(profile.Brand == SignInManager.BRAND_GOOGLE){
            ai.brand = GlobalConstants.BRAND_GOOGLE;
        }
        else if(profile.Brand == SignInManager.BRAND_MS){
            ai.brand = GlobalConstants.BRAND_MS;
        }else{
            Log.w(TAG, "Unknown brand!");
        }
        ai.name = profile.Name;
        ai.mail = profile.Mail;
        ai.photouri = profile.PhotoUri;
        err = am.createAccount(mActivity.getApplicationContext(), ai);
        if (err != true) {
            Log.w(TAG, "Create account failed!");
        }else{
            //TODO: may need a proper handling if something wrong when creating an account.
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //Log.d(TAG, "Fragment attached to Activity.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Log.d(TAG, "Fragment de-attached from Activity.");
    }
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop!");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState!");
//        outState.putSerializable("Drives", mDrives);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Log.d(TAG, "onViewStateRestored!");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause!");
        callback.remove();
    }

    /*
        We want the screen is switched to previous one(Master account fragment) without user's intervention
        once a signin result is got
     */
//    private void startTimer(){
//
//        myTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                TimerMethod();
//            }
//
//        }, 0, 2000);
//    }
//
//    private void TimerMethod()
//    {
//        //This method is called directly by the timer
//        //and runs in the same thread as the timer.
//
//        //We call the method that will work with the UI
//        //through the runOnUiThread method.
//        //Log.d(TAG, "Timer: wait sign in result...");
//        //Log.d(TAG, "Lifecycle: " + getLifecycle().getCurrentState() + " " + getLifecycle());
//        //Log.d(TAG, "mLifecycle: " + mLifecycle.getCurrentState() + " " + mLifecycle);
//        if(getSigninState().equals(STATE_COMPLETED)) {
//            //Log.d(TAG, "Timer: sign in result got!");
//            if(mLifecycle.getCurrentState().isAtLeast(Lifecycle.State.RESUMED)){
//                //Log.d(TAG, "Back to previous screen");
//                mActivity.runOnUiThread(Timer_Tick);
//                myTimer.cancel();
//            }
//        }
//    }
//
//    private Runnable Timer_Tick = new Runnable() {
//        public void run() {
//
//                //This method runs in the same thread as the UI.
//
//                //Do something to the UI thread here
//                Fragment f = FragmentManager.findFragment(mView);
//                AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(signinAccountName);
//                NavHostFragment.findNavController(f).navigate((NavDirections) action);
//
//
//        }
//    };
//
}