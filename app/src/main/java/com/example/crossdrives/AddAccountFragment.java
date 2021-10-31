package com.example.crossdrives;

import android.app.Activity;
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


import org.jetbrains.annotations.NotNull;

public class AddAccountFragment extends BaseFragment {
    private String TAG = "CD.AddAccountFragment";
    SignInManager mSignInManager = null;
    private static final int RC_SIGN_IN = 0;
    Fragment mFragment;
    View mView;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.add_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.add_account_btn_google).setOnClickListener(listener_add_gdrive);
        view.findViewById(R.id.add_account_btn_ms).setOnClickListener(listener_add_onedrive);

        requireActivity().getOnBackPressedDispatcher().addCallback(callback);

        mFragment = FragmentManager.findFragment(view);

        Toolbar toolbar = view.findViewById(R.id.add_account_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);
    }

    private View.OnClickListener listener_add_gdrive = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "start sign flow");
            mView = v;
            mSignInManager = SignInGoogle.getInstance(getContext());
            AccountManager.AccountInfo ai
                    = getActivatedAccount(GlobalConstants.BRAND_GOOGLE);
            if (ai != null) {
                //OK. Now we are sure that there is a activated google account. Ask user for next step.
                Intent intent = new Intent(mFragment.getActivity(), SignOutDialog.class);
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
                Intent intent = new Intent(mFragment.getActivity(), SignOutDialog.class);
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
            if(result == SignInManager.RESULT_SUCCESS){
                boolean r_am = false;
                String brand_am = GlobalConstants.BRAND_GOOGLE;
                Log.i(TAG, "App has signed out!");

                AccountManager.AccountInfo ai;
                AccountManager am = AccountManager.getInstance();
                if(brand == GlobalConstants.BRAND_MS) {
                    brand_am = GlobalConstants.BRAND_MS;
                }
                ai = am.getAccountActivated(getContext(), brand_am);
                r_am = am.setAccountDeactivated(getContext(), ai.brand, ai.name, ai.mail);
                if(r_am != true){Log.w(TAG, "Set account deactivated not worked");}
                //double check if the account is set deactivated
                //ai = am.getAccountActivated(getContext(), AccountManager.BRAND_GOOGLE);
                //if(ai != null)
                //
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


//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        SignInManager.Profile p;
//        String name = null;
//        AccountManager.AccountInfo ai= new AccountManager.AccountInfo();
//        boolean result;
//
//        Log.d(TAG, "requestCode: " + requestCode);
//        Log.d(TAG, "resultCode: " + resultCode);
//
//        //result code is 0 if user press BACK in the sign in activity. -1 is received if user entered signe in credentials.
//        if(resultCode != 0) {
//            Log.d(TAG, "handle sign flow");
//            p = mSignInManager.HandleSigninResult(data);
//            if (p == null)
//                Log.w(TAG, "handle result error!");
//
//            Log.d(TAG, "User name:" + p.Name);
//            Log.d(TAG, "User mail:" + p.Mail);
//            Log.d(TAG, "User photo url:" + p.PhotoUri);
//
////            DBHelper dbh = new DBHelper(getContext(), null, null, 0);
////            r_id = dbh.insert("Google", p.Name, p.Mail, p.PhotoUri, "Activated");
////            if(r_id == -1){
////                Log.w(TAG, "Create account failed!");
////            }
//            AccountManager am = AccountManager.getInstance();
//            ai.brand = AccountManager.BRAND_GOOGLE;
//            ai.name = p.Name;
//            ai.mail = p.Mail;
//            ai.photouri = p.PhotoUri;
//            result = am.createAccount(getContext(), ai);
//            if (result != true)
//                Log.w(TAG, "Create account failed!");
//            name = p.Name;
//        }
//
//        //passing name to master account fragment so that a toast is shown to the user that an account is created
//        AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(name);
//        //action.setCreateAccountName(p.Name);
//        NavHostFragment.findNavController(mFragment).navigate((NavDirections) action);
//    }

    @Override
    public void onPause() {
        super.onPause();
        callback.remove();
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            // Handle the back button event
            Log.d(TAG, "Back button pressed!");
            //passing null to master account fragment to avoid showing the toast
            AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(null);
            //action.setCreateAccountName(p.Name);
            NavHostFragment.findNavController(mFragment).navigate((NavDirections) action);

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
        NavHostFragment.findNavController(mFragment).navigate((NavDirections) action);

        return super.onOptionsItemSelected(item);
    }

    SignInManager.OnInteractiveSignInfinished onSigninFinished = new SignInManager.OnInteractiveSignInfinished(){
        @Override
        public void onFinished(int result, SignInManager.Profile profile, Object object) {
            boolean err = false;

            if(result == SignInManager.RESULT_SUCCESS) {
                if(profile.Brand == SignInManager.BRAND_GOOGLE){

                    Log.d(TAG, "User sign in OK. Start to create google drive client");
                    GoogleDriveClient google_drive = new GoogleDriveClient(getContext()).create(object);
                }
                else if(profile.Brand == SignInManager.BRAND_MS)
                {
                    Log.d(TAG, "User sign in OK. Start to create one drive client");
                    GraphDriveClient graph_drive = new GraphDriveClient().create(object);
                }
                else{
                    Log.w(TAG, "Unknow brand!");
                }

                createAccount(profile);

            }
            else{
                Toast.makeText(getContext(), "Sign in failed! error:" + result, Toast.LENGTH_LONG).show();
            }

            //passing name to master account fragment so that a toast is shown to the user that an account is created
            AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(profile.Name);
            //action.setCreateAccountName(p.Name);
            NavHostFragment.findNavController(mFragment).navigate((NavDirections) action);
        }
    };

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
        err = am.createAccount(getContext(), ai);
        if (err != true) {
            Log.w(TAG, "Create account failed!");
        }else{
            //TODO: may need a proper handling if something wrong when creating an account.
        }
    }
}
