package com.crossdrives.cdfs.allocation;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.util.ArrayList;

public class List {
    final String TAG = "CD.Allocation.List";
    CDFS mCDFS;

    public List(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public FileList list(String parent){
        FileList filelist = new FileList();
        java.util.List<com.google.api.services.drive.model.File> Itemlist = null;
        java.util.List<com.google.api.services.drive.model.File> Dirlist = null;


        java.util.List<String> names, dirs;
        names = getItems(parent);
        dirs = getItemsDir(parent);
        for (int i = 0 ; i < names.size(); i++){
            com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
            f.setName(names.get(i));
            f.setParents(null);
            Itemlist.add(f);
        }
        filelist.setFiles(Itemlist);

        java.util.List<String> parentList = new ArrayList<>();
        parentList.add(parent);
        for (int i=0 ; i < dirs.size(); i++){
            com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
            f.setName(dirs.get(i));
            f.setParents(parentList);
            Dirlist.add(f);
        }
        filelist.setFiles(Dirlist);

        return filelist;
    }

    private java.util.List<String> getItems(String parent){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String clause1, clause2;
        Cursor cursor = null;
        java.util.List<String> names = null;
        /*
            Set filter(clause) parent
         */
        clause1 = DBConstants.ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            clause1.concat(" =" + "\"" + "Root" + "\"");
        }else{
            clause1.concat(" =" + "\"" + parent + "\"");
        }
        /*
            Set filter(clause) attribute folder
         */
        clause2 = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;
        clause2.concat("=" + "0");

        Log.w(TAG, "Get items not a dir. Clause: " + clause1 + " and " + clause2);
        cursor = dh.query(clause1, clause2);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return names;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            return names;
        }

        names = buildNameList(cursor);
        return names;
    }

    private java.util.List<String> getItemsDir(String parent){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String clause1, clause2;
        Cursor cursor = null;
        java.util.List<String> names = null;
        /*
            Set filter(clause) parent
         */
        clause1 = DBConstants.ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            clause1.concat(" =" + "\"" + "Root" + "\"");
        }else{
            clause1.concat(" =" + "\"" + parent + "\"");
        }
        /*
            Set filter(clause) attribute folder
         */
        clause2 = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;
        clause2.concat("=" + "1");

        Log.w(TAG, "Get dir items. Clause: " + clause1 + " and " + clause2);
        cursor = dh.query(clause1, clause2);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return names;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            return names;
        }

        names = buildNameList(cursor);
        return names;
    }

    private java.util.List<String> buildNameList(Cursor cursor){
        java.util.List<String> names = new ArrayList<>();

        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++) {
            boolean matched = false;
            String col_name = cursor.getString(DBConstants.TABLE_ALLOCITEM_COL_INDX_NAME);
            for (int j = 0; j < names.size(); j++){
                if (names.get(i).equals(col_name)) {
                    matched = true;
                }
            }
            if(matched == false){
                names.add(col_name);
            }
            cursor.moveToNext();
        }

        return names;
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
//    }
}
