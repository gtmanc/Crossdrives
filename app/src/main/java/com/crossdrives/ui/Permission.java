package com.crossdrives.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.R;

public class Permission{
    final String TAG = "CD.Permission";
    Activity mActivity;
    Fragment mFragment;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    String mPermission;
    String mEducationMessage;

    public Permission(Fragment fragment, ActivityResultLauncher<String> requestPermissionLauncher) {
        Log.d(TAG, "fragment: " + fragment);
        this.mFragment = fragment;
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        this.requestPermissionLauncher = requestPermissionLauncher;
    }

    /*
        The method returns immediately the result if the requested permission is granted already.
        Otherwise, a rational UI is shown to the user and the caller has wait the result given by the user.
        The result from user will be sent into the callback function which is assigned in constructor of the class.
     */
    public boolean request(String permisson, String educationMmessage){
        int StatusChecked =
        ContextCompat.checkSelfPermission(SnippetApp.getAppContext(), permisson);
        mPermission = permisson;
        mEducationMessage = educationMmessage;
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
                will always false.
             */
            Log.d(TAG, "Show education UI...");
            EducationDialog dialog = new EducationDialog();
			dialog.show(mFragment.getParentFragmentManager(), "FABOptionAlertDialog");
        }else{
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(permisson);
            Log.d(TAG, "Show request permisson screen...");
        }

        return false;
    }

    class EducationDialog extends DialogFragment{
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(mFragment.getActivity());

            LayoutInflater inflater = requireActivity().getLayoutInflater();

            builder.setMessage(mFragment.getString(R.string.message_permission_education))
                    .setPositiveButton(mFragment.getString(R.string.left_button_permision_education), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // START THE GAME!
                            requestPermissionLauncher.launch(mPermission);
                        }
                    })
                    .setNegativeButton(mFragment.getString(R.string.right_button_permission_education), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            Toast.makeText(mFragment.getContext(), mEducationMessage
                                    , Toast.LENGTH_LONG).show();
                        }
                    })
                    .setView(inflater.inflate(R.layout.fab_option_alertdialog, null));
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

}
