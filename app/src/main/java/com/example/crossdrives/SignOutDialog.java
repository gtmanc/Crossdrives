package com.example.crossdrives;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

public class SignOutDialog extends Activity {
    private Activity mActivity;
    public static String KEY_ACTION = "Action";
    public static String ACTION_SIGNOUT = "Signout";
    public static String ACTION_CANCEL = "Cancel";

    //String mBrand;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;

        setContentView(R.layout.signout_dialog);

        findViewById(R.id.signout_dialog_button_signout).setOnClickListener(mOnClickerSignOut);
        findViewById(R.id.signout_dialog_button_cancel).setOnClickListener(mOnClickerCancel);

//        Intent intent = getIntent();
//        mBrand = intent.getStringExtra("Brand");
    }

    View.OnClickListener mOnClickerSignOut = new View.OnClickListener(){

        @Override
        public void onClick(View v) {

            Intent intent = new Intent();
            intent.putExtra(KEY_ACTION, ACTION_SIGNOUT);
            //intent.putExtra("Brand", mBrand);
            mActivity.setResult(RESULT_OK, intent);
            mActivity.finish();
        }
    };
    View.OnClickListener mOnClickerCancel = new View.OnClickListener(){

        @Override
        public void onClick(View v) {

            Intent intent = new Intent();
            intent.putExtra(KEY_ACTION, ACTION_CANCEL);
            //intent.putExtra("Brand", mBrand);
            mActivity.setResult(RESULT_OK, intent);
            mActivity.finish();
        }
    };
}
