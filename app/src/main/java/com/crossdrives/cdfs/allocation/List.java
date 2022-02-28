package com.crossdrives.cdfs.allocation;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;

public class List {
    final String TAG = "CD.Allocation.List";
    CDFS mCDFS;

    public List(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public FileList list(String parent){
        FileList filelist = null;
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        Cursor cursor = null;
        java.util.List<String> names = new ArrayList<>();
        String column, value;

        column = DBConstants.ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            value = "\"" + "Root" + "\"";
        }else{
            value = "";
        }

        cursor = dh.query(column, value);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return filelist;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            return filelist;
        }

        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++) {
            boolean presnted = false;
            for (int j = 0; j < names.size(); j++){
                if (names.get(i).equals(cursor.getString(DBConstants.TABLE_ALLOCITEM_COL_INDX_NAME))) {
                    presnted = true;
                }
            }
        }
        name = cursor.getString(DBConstants.TABLE_ALLOCITEM_COL_INDX_NAME);
        Log.d(TAG, "Name: " + name);
        return filelist;
    }


//    public void list(){
//        Set<String> set = map.keySet();
//        Iterator<String> it = set.iterator();
//        while (it.hasNext()) {
//            String key = it.next();
//            System.out.printf("key:%s,value:%s\n", key, map.get(key));
//        }
//
//        //forEach
//        for (Map.Entry<String, Double> entry : map.entrySet()) {
//            System.out.println("key:" + entry.getKey() + ",value:" + entry.getValue());
//        }
//        mCDFS.getDrives().
    }
}
