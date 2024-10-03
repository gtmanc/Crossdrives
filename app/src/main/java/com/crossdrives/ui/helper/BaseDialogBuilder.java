package com.crossdrives.ui.helper;

import android.app.Activity;
import android.content.Intent;

import com.crossdrives.ui.BaseActionDialog;

public class BaseDialogBuilder{
    String title;
    String content;
    String textPositiveButton;
    String textNegativeButton;

    int numTextInputBox;

    protected void setTitle(String title){
        this.title = title;
    }

    protected void setContent(String content){
        this.content = content;
    }

    protected void setTextPositiveButton(String text){
        this.textPositiveButton = text;
    }

    protected void setTextNegativeButton(String text){
        this.textNegativeButton = text;
    }

    public void setNumTextInputBox(int number){
        numTextInputBox = number;
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
