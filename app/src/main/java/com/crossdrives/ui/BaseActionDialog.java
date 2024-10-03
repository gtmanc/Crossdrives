package com.crossdrives.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.crossdrives.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class BaseActionDialog extends ComponentActivity
{
    final String TAG = "CD.BaseActionDialog";

    public static final String KEY_TITLE = "title";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_NEGATIVE_BUTTON_TEXT = "negative button text";
    public static final String KEY_POSITIVE_BUTTON_TEXT = "positive button text";
    public static final String KEY_NUM_TEXTEDIT_BOX_REQUIRED = "number text edit required";

    public static final String KEY_RESULT_EDIT_TEXTS = "results edit text";

    private Intent intent = new Intent();

    Activity mActivity;

    ArrayList <TextInputLayout> viewTextInputLayout;
    ArrayList <TextInputEditText> viewEditTexts;
    int numberTextEditBox;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.base_action_dialog_dialog);
        mActivity = this;
        this.setFinishOnTouchOutside(false);

        TextView viewTitle = findViewById(R.id.title_base_action_dialog);
        TextView viewContent = findViewById(R.id.content_base_action_dialog);
        Button negativeButton = findViewById(R.id.button_negative_base_action_dialog);
        Button positiveButton = findViewById(R.id.button_positive_base_action_dialog);
        viewTextInputLayout = new ArrayList<>();
        viewEditTexts = new ArrayList<>();
        viewTextInputLayout.add(findViewById(R.id.textInputLayout1_base_action_dialog));
        viewTextInputLayout.add(findViewById(R.id.textInputLayout2_base_action_dialog));
        viewEditTexts.add(findViewById(R.id.edit_box1_base_action_dialog));
        viewEditTexts.add(findViewById(R.id.edit_box2_base_action_dialog));

        //
        Intent intent = this.getIntent();
        if(intent == null){
            return;
        }

        String title = intent.getStringExtra(KEY_TITLE);
        String content = intent.getStringExtra(KEY_CONTENT);
        String textNegativeButton = intent.getStringExtra(KEY_NEGATIVE_BUTTON_TEXT);
        String textPositiveButton = intent.getStringExtra(KEY_POSITIVE_BUTTON_TEXT);
        numberTextEditBox = intent.getIntExtra(KEY_NUM_TEXTEDIT_BOX_REQUIRED, 1);

        if(title != null){
            viewTitle.setText(title);
        }
        if(content != null){
            viewContent.setText(content);
            viewContent.setVisibility(View.VISIBLE);
            //Log.d(TAG, "content: " + viewContent.getText().toString());
        }
        if(textNegativeButton != null){
            negativeButton.setText(textNegativeButton);
        }
        if(textPositiveButton != null){
            positiveButton.setText(textPositiveButton);
        }

        if(numberTextEditBox > 2) {numberTextEditBox=2;}
        for(int i = 0; i < numberTextEditBox; i++){
            viewTextInputLayout.get(i).setVisibility(View.VISIBLE);
        }

        //Determine the widget that the buttons are aligned to
        int id = 0;
        if(numberTextEditBox==0){
            id = R.id.content_base_action_dialog;
        }else{
            id = viewTextInputLayout.get(numberTextEditBox-1).getId();
        }

        ConstraintLayout constraintLayout = findViewById(R.id.layout_base_action_dialog);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.button_negative_base_action_dialog,ConstraintSet.TOP,
                id, ConstraintSet.BOTTOM,0);
        constraintSet.connect(R.id.button_positive_base_action_dialog,ConstraintSet.TOP,
                id,ConstraintSet.BOTTOM,0);
        constraintSet.applyTo(constraintLayout);
        negativeButton.setOnClickListener(NegativeButtonListener);
        positiveButton.setOnClickListener(PositiveButtonListener);

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
            ArrayList<String> results = new ArrayList<>();
            Intent intent = new Intent();
            for(int i = 0; i < numberTextEditBox; i++){
                //intent.putExtra(KEY_RESULT_EDIT_TEXTS, viewEditTexts.get(i).getText().toString());
                results.add(i,  viewEditTexts.get(i).getText().toString());
                Log.d(TAG, "Entered name: " + viewEditTexts.get(i).getText().toString());
            }

            intent.putExtra(KEY_RESULT_EDIT_TEXTS, results);
            mActivity.setResult(RESULT_OK, intent);
            finish();
        }
    };
}
