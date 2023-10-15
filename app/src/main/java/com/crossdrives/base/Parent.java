package com.crossdrives.base;

import android.icu.text.Edits;
import android.util.Log;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.model.CdfsItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Parent {
    final static String TAG = "CD.Parent";
    /*
        holder: set to true if a parent holder is needed.
     */
    static public CdfsItem[] toArray(List<CdfsItem> parents, boolean holder){
        int size = 0;

        if( parents != null){	size = parents.size();}
        if(holder){size = size + 1;}
        CdfsItem[] itemArray = new CdfsItem[size];
        itemArray = parents.toArray(itemArray);
        return itemArray;
    }

    static public CdfsItem getCurrent(List<CdfsItem> plist){
        CdfsItem item = null;

        if(!plist.isEmpty()){
            item = plist.get(plist.size() - 1);
            Log.d(TAG, "Current parent: " + item.getName() );

        }

        return item;
    }

    public static @Nullable List<String> toIdList(@Nullable List<CdfsItem> parents){
        CdfsItem item = getCurrent(parents);
        List<String> list = null;
        if(item != null){
           list = item.getParents();
        }

        return list;
    }
}
