package com.crossdrives.ui.helper;

import android.content.Intent;

public class RenameDialogResultResolver extends BaseDialogResultResolver{
    Intent mIntent;
    public RenameDialogResultResolver(Intent intent) {
        mIntent = intent;
    }

    public String getNewName(){
        return getTexts(mIntent).get(0);
    }
}
