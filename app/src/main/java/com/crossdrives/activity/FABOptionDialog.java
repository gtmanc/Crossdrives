package com.crossdrives.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.crossdrives.R;
import com.example.crossdrives.SignOutDialog;

public class FABOptionDialog extends ComponentActivity {
    final String TAG = "CD.FABOptionDialog";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fab_option_dialog;

        findViewById(R.id.button_fab_option_dialog_upload).setOnClickListener(clickUpload);
    }

    View.OnClickListener clickUpload = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //intent.putExtra("Brand", SignInManager.BRAND_GOOGLE);
            mStartForResult.launch(createFilePickerIntent());
        }
    };

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //type is required: https://stackoverflow.com/questions/21045091/no-activity-found-to-handle-intent-android-intent-action-open-document
        //Note: if text/plain is set. Any file except text will not be selectable in SAF.
        //intent.setType("text/plain");
        intent.setType("*/*");

        return intent;
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

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
}
