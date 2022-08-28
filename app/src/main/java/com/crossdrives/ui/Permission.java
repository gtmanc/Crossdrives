package com.crossdrives.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.crossdrives.cdfs.exception.CompletionException;
import com.crossdrives.msgraph.SnippetApp;

public class Permission{
    final String TAG = "CD.Permission";
    Activity mActivity;
    Fragment fragment;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public Permission(Activity mActivity, ActivityResultLauncher<String> requestPermissionLauncher) {
        this.mActivity = mActivity;
        Log.d(TAG, "fragment: " + fragment);
        this.fragment = fragment;
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        this.requestPermissionLauncher = requestPermissionLauncher;
    }

    public boolean request(String permisson){
        int StatusChecked =
        ContextCompat.checkSelfPermission(SnippetApp.getAppContext(), permisson);
        if(StatusChecked == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Permission already granted!");
            return true;
        }else if(mActivity.shouldShowRequestPermissionRationale(permisson)){
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            /*
                Once user selected "never ask again" checkbox, shouldShowRequestPermissionRationale
                will return false.
             */
            Log.d(TAG, "Show education UI...");
            requestPermissionLauncher.launch(permisson);
        }else{
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(permisson);
            Log.d(TAG, "Show request permisson screen...");
        }

        return false;
    }


}
