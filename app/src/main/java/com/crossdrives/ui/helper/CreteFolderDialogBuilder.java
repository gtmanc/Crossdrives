package com.crossdrives.ui.helper;

import android.content.Intent;

public class CreateFolderDialogBuilder {
    String title;
    String content;
    String textPositiveButton;
    String textNegativeButton;

    void setTitle(String title){this.title = title;}

    void setConten(String content){this.content = content;}

    void setTextPositiveButton(String text){this.textPositiveButton = text;}

    void setTextNegativeButton(String text){this.textNegativeButton = text;}

    Intent build(){
        Intent intent = new Intent();
    };

    String getName(Intent intent){}
}
