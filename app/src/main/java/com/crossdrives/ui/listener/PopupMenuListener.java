package com.crossdrives.ui.listener;

import android.view.MenuItem;
import android.widget.PopupMenu;

import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.ui.GlobalUiStateVm;
import com.example.crossdrives.R;

public class PopupMenuListener implements PopupMenu.OnMenuItemClickListener {
    GlobalUiStateVm vm;
    CdfsItem startDest[];
    public PopupMenuListener(GlobalUiStateVm vm, CdfsItem[] startDest) {
        this.vm = vm;
        this.startDest = startDest;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if( id == R.id.omiMove){
            vm.getMoveItemStateLd().launch(startDest);
        }else if (id == R.id.omiInfo){

        }

        return false;
    }
}
