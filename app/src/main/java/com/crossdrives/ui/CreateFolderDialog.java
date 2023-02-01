package com.crossdrives.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.example.crossdrives.R;

public class CreateFolderDialog extends ComponentActivity {
    final String TAG = "CD.CreateFolderDialog";
    public static String KEY_ACTION = "Action";
    public static String ACTION_FOLDER_NAME = "pick folder name";

    public static String KEY_FOLDER_NAME = "folder name";

    private Intent intent = new Intent();

    Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fab_option_dialog);

        this.setFinishOnTouchOutside(false);
        findViewById(R.id.button_fab_option_dialog_upload).setOnClickListener(NegativeButtonListener);
        findViewById(R.id.button_fab_option_dialog_upload).setOnClickListener(PositiveButtonListener);

        mActivity = this;
    }



    View.OnClickListener NegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mActivity.setResult(RESULT_CANCELED, intent);
            finish();
        }
    };

    View.OnClickListener PositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_FOLDER_NAME, "");
            intent.putExtra(KEY_ACTION, ACTION_FOLDER_NAME);
            intent.putExtras(bundle);
            mActivity.setResult(RESULT_OK, intent);
            finish();
        }
    };


}
