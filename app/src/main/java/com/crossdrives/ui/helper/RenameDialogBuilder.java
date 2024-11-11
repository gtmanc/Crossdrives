package com.crossdrives.ui.helper;

public class RenameDialogBuilder extends BaseDialogBuilder{
    public RenameDialogBuilder title(String title){
        setTitle(title);
        return this;
    }

    public RenameDialogBuilder content(String content){
        setContent(content);
        return this;
    }

    public RenameDialogBuilder numTextInputBox(int number){
        setNumTextInputBox(number);
        return this;
    }
}
