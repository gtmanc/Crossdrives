package com.example.crossdrives;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class AddAccountFragment extends Fragment {
    private String TAG = "CD.AddAccountFragment";
    SignInManager mSignInManager = null;
    private static final int RC_SIGN_IN = 0;
    Fragment mFragment;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.add_account_btn_google).setOnClickListener(listener_account_add);

        requireActivity().getOnBackPressedDispatcher().addCallback(callback);

        mFragment = FragmentManager.findFragment(view);
    }

    private View.OnClickListener listener_account_add = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "start sign flow");
            mSignInManager = new SignInGoogle(getContext());
            Intent signInIntent = mSignInManager.Prepare();

            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        SignInManager.Profile p;
        SQLiteDatabase db = null;
        long r_id = -1;
        Cursor c;
        String name = null;
        Log.d(TAG, "requestCode: " + requestCode);
        Log.d(TAG, "resultCode: " + resultCode);

        //result code is 0 if user press BACK in the sign in activity. -1 is received if user entered signe in credentials.
        if(resultCode != 0) {
            Log.d(TAG, "handle sign flow");
            p = mSignInManager.HandleSigninResult(data);
            if (p == null)
                Log.w(TAG, "handle result error!");

            Log.d(TAG, "User name:" + p.Name);
            Log.d(TAG, "User mail:" + p.Mail);
            Log.d(TAG, "User photo url:" + p.PhotoUri);

            DBHelper dbh = new DBHelper(getContext(), null, null, 0);
            r_id = dbh.insert("Google", p.Name, p.Mail, p.PhotoUri);
            if (r_id == -1)
                Log.w(TAG, "Insert record fails!");
            name = p.Name;
        }

        AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(name);
        //action.setCreateAccountName(p.Name);
        NavHostFragment.findNavController(mFragment).navigate(action);
    }

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
            AddAccountFragmentDirections.NavigateBackToMasterAccount action = AddAccountFragmentDirections.navigateBackToMasterAccount(null);
            //action.setCreateAccountName(p.Name);
            NavHostFragment.findNavController(mFragment).navigate(action);

        }
    };
}
