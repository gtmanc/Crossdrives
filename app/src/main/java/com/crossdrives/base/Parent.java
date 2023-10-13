package com.crossdrives.base;

import android.util.Log;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.model.CdfsItem;

import java.util.List;

public class Parent {
    public List<CdfsItem> getParentList() {
        return mParents;
    }
    /*
        holder: set to true if a parent holder is needed.
     */
    static public CdfsItem[] toParentArray(boolean holder){
        int size = 0;

        if( mParents != null){	size = mParents.size();}
        if(holder){size = size + 1;}
        CdfsItem[] itemArray = new CdfsItem[size];
        itemArray = mParents.toArray(itemArray);
        return itemArray;
    }

    @Nullable
    public CdfsItem getParent() {
        return getParent(mParents);
    }

    private CdfsItem getParent(List<CdfsItem> plist){
        CdfsItem item = null;

        if(!plist.isEmpty()){
            item = plist.get(plist.size() - 1);
            Log.d(TAG, "Name of item: " + item.getName());
        }

        return item;
    }
}
