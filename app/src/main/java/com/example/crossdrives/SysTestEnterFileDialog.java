package com.example.crossdrives;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

public class SysTestEnterFileDialog extends Activity {
    private String TAG = "CD.SysTestEnterFileContentDialog";
    TextInputLayout  mName = null;
    TextInputLayout  mText = null;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate is called");
        setContentView(R.layout.systemtest_enter_content_dialog);
        mName = (TextInputLayout)findViewById(R.id.sys_test_dialog_input_box1);
        mText = (TextInputLayout)findViewById(R.id.sys_test_dialog_input_box2);
        findViewById(R.id.system_test_dialog_button_create).setOnClickListener(OnCreateButton);
        findViewById(R.id.system_test_dialog_button_cancel).setOnClickListener(OnCancelButton);
    }

    View.OnClickListener OnCreateButton = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            Editable name = mName.getEditText().getText();
            Editable text = mText.getEditText().getText();

            intent.putExtra("Name", name.toString());
            intent.putExtra("Text", text.toString());
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    View.OnClickListener OnCancelButton = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            finish();
        }
    };
}
