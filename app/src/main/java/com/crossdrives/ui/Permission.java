package com.crossdrives.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.R;

public class Permission{
    final String TAG = "CD.Permission";
    Fragment mFragment;
    static private ActivityResultLauncher<String> requestPermissionLauncher;
    static String mRequiredPermission;
    static String mEducationMessage, mImplicationMessage, mImplicationMessageNeverAsk;
    //Class<? extends DialogFragment> educationUI;

    boolean isNeverAskAgainSelected = false;

    public Permission(Fragment fragment, ActivityResultLauncher<String> requestPermissionLauncher, String permission) {
        //Log.d(TAG, "fragment: " + fragment);
        this.mFragment = fragment;
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        this.requestPermissionLauncher = requestPermissionLauncher;
        //this.educationUI = educationUI;
        mRequiredPermission = permission;
    }

    public Permission setImplicationMessage(String implicationMessage){
        mImplicationMessage = implicationMessage;
        return this;
    }

    public Permission setImplicationMessageNeverAsk(String message){
        mImplicationMessageNeverAsk = message;
        return this;
    }

    public Permission setEducationMessage(String educationMessage){
        mEducationMessage = educationMessage;
        return this;
    }

    public boolean neverAskAgainSelected(){return isNeverAskAgainSelected;}

    /*
        The method returns immediately the result if the requested permission is granted already.
        Otherwise, a rational UI is shown to the user and the caller has to wait the result given by the user.
        The result from user will be sent into the callback function which is assigned in constructor of the class.
        Return:
            result for user's response.
            true: requested permission has been granted
            false requested permission has not yet or user response with "never ask again"

        shouldShowRequestPermissionRationale returns if user denies the permission request (not education negative button)
        First time after app is installed:
            false
        Second time:
            true
        Third time
            Android 9: true. true is always returned as long as the "never ask again" option is not checked
            Android 11: false. "Never ask again" seems selected automatically by platform
        First time after user grants the permission in the system setting
            true
     */
    public boolean request(){
        int StatusChecked =
        ContextCompat.checkSelfPermission(SnippetApp.getAppContext(), mRequiredPermission);
        //mEducationMessage = educationMmessage;
        if(StatusChecked == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "Permission already granted!");
            return true;
        }else if(mFragment.getActivity().shouldShowRequestPermissionRationale(mRequiredPermission)){
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            /*
                Once user selected "never ask again" checkbox, shouldShowRequestPermissionRationale
                will always false.
             */
            Log.d(TAG, "Show education UI...");

            EducationDialog educationDialog = new EducationDialog();
            educationDialog.show(mFragment.getParentFragmentManager(), "EducationUIAlertDialog");

        }else{
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request. If user already checked
            //the "never ask again" option, the negative result will get immediately in the registered ActivityResultCallback
            //for the requestPermission screen without user's intervention.
            SharedPreferences sharedPreferences = mFragment.getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor sharedEditor = sharedPreferences.edit();
            if (sharedPreferences.getBoolean("firstTime", true)) {
                sharedEditor.putBoolean("firstTime", false);
                sharedEditor.commit();
                sharedEditor.apply();
            } else {
                isNeverAskAgainSelected = true;
            }

            requestPermissionLauncher.launch(mRequiredPermission);
            //requestPermissionScreen.launch(mRequiredPermission);
            Log.d(TAG, "Show request permission screen...");
        }

        return false;
    }

    public static class EducationDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = requireActivity().getLayoutInflater();

            String educationMessage = "Grant the requested permission to ensure the function works as expectedly.";
            if(mEducationMessage != null){educationMessage = mEducationMessage;}

            builder.setMessage(educationMessage)
                    .setPositiveButton(getString(R.string.right_button_permission_education), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            requestPermissionLauncher.launch(mRequiredPermission);
                            //requestPermissionScreen.launch(mRequiredPermission);
                        }
                    })
                    .setNegativeButton(getString(R.string.left_button_permission_education), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String implicationMessage =
                                    "Without this permission, function of the app may not work as expectedly!";
                            if(mImplicationMessage != null){implicationMessage = mImplicationMessage;}
                            // User cancelled the dialog
                            Toast.makeText(getContext(), implicationMessage, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setView(inflater.inflate(R.layout.fab_option_alertdialog, null));
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    /*
			Once user selected "never ask again" checkbox, nothing is shown even the requestPermissionLauncher.launch
            is called. Besides, the callback is called with negative isGranted (false).
        */
//    public static ActivityResultLauncher<String> requestPermissionScreen =
//            mFragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                if (isGranted) {
//                    // Permission is granted. Continue the action or workflow in your
//                    // app.
//                    Log.d(TAG, "User granted permission.");
//                } else {
//                    // Explain to the user that the feature is unavailable because the
//                    // features requires a permission that the user has denied. At the
//                    // same time, respect the user's decision. Don't link to system
//                    // settings in an effort to convince the user to change their
//                    // decision.
//                    Log.d(TAG, "Show the implication.");
////						Toast.makeText(getContext(), getString(R.string.implication_permission_external_storage_not_granted),
////								Toast.LENGTH_LONG).show();
//                }
//                //requestPermissionFuture.complete(isGranted);
//            });
}
