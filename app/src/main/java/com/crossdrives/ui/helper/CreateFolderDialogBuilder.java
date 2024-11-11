package com.crossdrives.ui.helper;

import android.app.Activity;
import android.content.Intent;

import com.crossdrives.ui.BaseActionDialog;
import com.fasterxml.jackson.databind.ser.Serializers;

import java.util.ArrayList;

public class CreateFolderDialogBuilder {
    String title;
    String content;
    String textPositiveButton;
    String textNegativeButton;

    int numTextInputBox;

    public CreateFolderDialogBuilder setTitle(String title){
        this.title = title;
        return this;
    }

    public CreateFolderDialogBuilder setContent(String content){
        this.content = content;
        return this;
    }

    public CreateFolderDialogBuilder setTextPositiveButton(String text){
        this.textPositiveButton = text;
        return this;
    }

    public CreateFolderDialogBuilder setTextNegativeButton(String text){
        this.textNegativeButton = text;
        return this;
    }

    public CreateFolderDialogBuilder setNumTextInputBox(int number){
        numTextInputBox = number;
        return this;
    }

    public Intent build(Activity activity){
        Intent intent = new Intent(activity, BaseActionDialog.class);

        intent.putExtra(BaseActionDialog.KEY_TITLE, title);
        intent.putExtra(BaseActionDialog.KEY_CONTENT, content);
        intent.putExtra(BaseActionDialog.KEY_NEGATIVE_BUTTON_TEXT, textNegativeButton);
        intent.putExtra(BaseActionDialog.KEY_POSITIVE_BUTTON_TEXT, textPositiveButton);
        intent.putExtra(BaseActionDialog.KEY_NUM_TEXTEDIT_BOX_REQUIRED, numTextInputBox);


        return intent;
    };
}
