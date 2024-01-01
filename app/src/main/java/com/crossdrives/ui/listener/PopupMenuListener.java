package com.crossdrives.ui.listener;

import android.view.MenuItem;
import android.widget.PopupMenu;

import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.ui.GlobalUiStateVm;
import com.example.crossdrives.R;

public class PopupMenuListener implements PopupMenu.OnMenuItemClickListener {
    GlobalUiStateVm vm;
    CdfsItem startDest[];
    int srcDestId;
    public PopupMenuListener(GlobalUiStateVm vm, CdfsItem[] startDest, int srcDestId) {
        this.vm = vm;
        this.startDest = startDest;
        this.srcDestId = srcDestId;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if( id == R.id.omiMove){
            vm.getMoveItemStateLd().launch(startDest, srcDestId);
        }else if (id == R.id.omiInfo){

        }

        return false;
    }
}
