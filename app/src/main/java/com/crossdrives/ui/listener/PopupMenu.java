package com.crossdrives.ui.listener;

import android.view.MenuItem;

import com.example.crossdrives.R;

public class PopupMenu {

    static public android.widget.PopupMenu.OnMenuItemClickListener create(){
        android.widget.PopupMenu.OnMenuItemClickListener listener = new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if( id == R.id.omiMove){

                }else if (id == R.id.omiInfo){

                }

                return false;
            }
        };
        return listener;
    }
}
