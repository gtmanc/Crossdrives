package com.example.crossdrives;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.DriveScopes;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BranchAccountFragment extends Fragment{
    private String TAG = "CD.SettingAccountFragment";
    private ArrayList<AccountListModel> mItems;
    private RecyclerView mRecyclerView = null;
    private RecyclerView.LayoutManager mLayoutManager;
    private AccountAdapter mAdapter;

    private static final int RC_SIGN_IN = 0;
    GoogleSignInClient mGoogleSignInClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(R.layout.branch_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");

        NavController navController = Navigation.findNavController(view);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(navController.getGraph()).build();
        Toolbar toolbar = view.findViewById(R.id.saf_toolbar);

        NavigationUI.setupWithNavController(
                toolbar, navController, appBarConfiguration);


        mRecyclerView = view.findViewById(R.id.rv_setting_account);
        mLayoutManager = new LinearLayoutManager(getContext());
        //It seems to be ok we create a new layout manager ans set to the recyclarview.
        //It is observed each time null is got if view.getLayoutManager is called
        mRecyclerView.setLayoutManager(mLayoutManager);

        mItems = get_signed_in_accounts();

        mAdapter = new AccountAdapter(mItems);

        mRecyclerView.setAdapter(mAdapter);


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        view.findViewById(R.id.tv_account_add).setOnClickListener(listener_account_add);


//        AccountManager am = AccountManager.get(getContext());
//        Account[] al = am.getAccountsByType("com.google");
//        Log.d(TAG, "account list length:" + al.length);
//        Log.d(TAG, "Name:" + al[0].name);

    }

    private View.OnClickListener listener_account_add = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Start sign-in screen");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleAccount) {
                    }
                })
                .addOnFailureListener(new OnFailureListener(){
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {

                    }
                });
    }

    //TODO a module "account manager" needs to be developed for handling the signed in accounts.
    private ArrayList<AccountListModel> get_signed_in_accounts(){
        ArrayList<AccountListModel> account_list = new ArrayList<>();

        //For test
        AccountListModel account = new AccountListModel("My name");

        account_list.add(account);

        return account_list;
    }
}
