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
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.OneDriveClient;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.jetbrains.annotations.NotNull;

public class AddAccountFragment extends BaseFragment{
    private String TAG = "CD.AddAccountFragment";
    SignInManager mSignInManager = null;
    private static final int RC_SIGN_IN = 0;
    View mView;
    Activity mActivity;
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
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
//            Log.d(TAG, "Restore mDrives...");
//            mDrives = (HashMap)savedInstanceState.getSerializable("Drives");
//            Log.d(TAG, "done. mDrives: " + mDrives);
        }
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        //Update activity object each time the fragment is shown. It will be used in callback called by background thread. e.g. onSigninFinished
        mActivity = getActivity();
        return inflater.inflate(R.layout.add_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
            mSignInManager = SignInGoogle.getInstance(getContext());
            AccountManager.AccountInfo ai
                    = getActivatedAccount(GlobalConstants.BRAND_GOOGLE);
            if (ai != null) {
                //OK. Now we are sure that there is a activated google account. Ask user for next step.
                Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), SignOutDialog.class);
                //intent.putExtra("Brand", SignInManager.BRAND_GOOGLE);
                mStartForResult.launch(intent);
            } else {
                mSignInManager.Start(mView, onSigninFinished);
            }

        }
    };

    private View.OnClickListener listener_add_onedrive = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "start sign flow");
            mSignInManager = SignInMS.getInstance(getActivity());
            AccountManager.AccountInfo ai
                    = getActivatedAccount(GlobalConstants.BRAND_MS);
            if (ai != null) {
                //OK. Now we are sure that there is a activated Microsoft account. Ask user for next step.
                Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), SignOutDialog.class);
                //intent.putExtra("Brand", SignInManager.BRAND_MS);
                mStartForResult.launch(intent);
            } else {
                mSignInManager.Start(mView, onSigninFinished);
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
            CDFS cdfs = CDFS.getCDFSService(getActivity().getApplicationContext());
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
                client = cdfs.getClient(brand);
                Log.d(TAG, "Remove CDFS client");
                r = cdfs.removeClient(brand, client);
                if(r != true) {Log.w(TAG, "Remove CDFS client failed!");}
            }

            mSignInManager.Start(mView, onSigninFinished);

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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//        Log.d(TAG, "onSaveInstanceState. mDrives: " + mDrives);
//        outState.putSerializable("Drives", mDrives);
    }

    @Override
    public void onPause() {
        super.onPause();
        callback.remove();
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
                    CDFS.getCDFSService(SnippetApp.getAppContext()).addClient(GlobalConstants.BRAND_GOOGLE, gdc);
                    //mDrives.put(GlobalConstants.BRAND_GOOGLE, i);
                }
                else if(profile.Brand == SignInManager.BRAND_MS)
                {
                    Log.d(TAG, "User sign in OK. Start to create one drive client");
                    OneDriveClient odc =
                            (OneDriveClient) OneDriveClient.builder((String) token).buildClient();
//                    if(getActivity() == null){
//                        Log.e(TAG, "getActivity returns null!");
//                    }
                    CDFS.getCDFSService(SnippetApp.getAppContext()).addClient(GlobalConstants.BRAND_MS, odc);
                    //mDrives.put(GlobalConstants.BRAND_MS, i);
                }
                else{
                    Log.w(TAG, "Unknown brand!");
                }

                createAccount(profile);

//            }
//            else{
//                Toast.makeText(getContext(), "Sign in failed! error:" + result, Toast.LENGTH_LONG).show();
//            }

            //The callback may not be called in main thread. e.g. Microsoft sign in
//            mActivity.runOnUiThread(new Runnable() {
//
//                @Override
//                public void run() {
//                    Fragment f = FragmentManager.findFragment(mView);
//                    //passing name to master account fragment so that a toast is shown to the user that an account is created
//                    AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(profile.Name);
//                    //action.setCreateAccountName(p.Name);
//                    Log.d(TAG, "Fragment: " + f.toString());
//                    //NavHostFragment.findNavController(f).navigate((NavDirections) action);
//                    Navigation.findNavController().navigate((NavDirections) action);
//                }
//            });

        }

        @Override
        public void onFailure(String err) {
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
        Log.d(TAG, "Fragment attached to Activity.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "Fragment de-attached from Activity.");
    }
}