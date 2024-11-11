package com.crossdrives.ui.helper;

import android.content.Intent;

import com.crossdrives.ui.BaseActionDialog;

import java.util.ArrayList;

public class BaseDialogResultResolver {
    public ArrayList<String> getTexts(Intent intent){
        return intent.getStringArrayListExtra(BaseActionDialog.KEY_RESULT_EDIT_TEXTS);
    }
}
