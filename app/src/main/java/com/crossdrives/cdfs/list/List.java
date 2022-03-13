package com.crossdrives.cdfs.list;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.IAllocManager;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.AllocationFetcher;
import com.crossdrives.cdfs.allocation.ICallBackAllocationFetch;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class List {
    final String TAG = "CD.Allocation.List";
    CDFS mCDFS;

    public List(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public void list(String parent, ICallbackList<FileList> callback) {
        FileList filelist = new FileList();
        java.util.List<com.google.api.services.drive.model.File> Itemlist = new ArrayList<>();
        java.util.List<com.google.api.services.drive.model.File> Dirlist = new ArrayList<>();

        /*
            Fetch the remote allocation files. The fetcher will ensure all of the allocations file
            are downloaded. We will search the local database for the file list as soon as the
            content of the downloaded allocation files are updated to local database.
         */
        AllocationFetcher fetcher = new AllocationFetcher(mCDFS.getDrives());
        fetcher.fetchAll(new ICallBackAllocationFetch<HashMap<String, OutputStream>>() {
            @Override
            public void onCompleted(HashMap<String, OutputStream> allocations)  {
                java.util.List<String> names, dirs;
                AtomicReference<AllocContainer> ac = new AtomicReference<>();
                AllocManager am = new AllocManager(mCDFS);

                /*
                    Update the fetched allocation content to database. We will query local database
                    for the file list requested by caller.
                 */
                mCDFS.getDrives().forEach((key, value)->{
                    ac.set(am.toContainer(allocations.get(key)));
                    if(am.checkCompatibility(ac.get()) == IAllocManager.ERR_COMPATIBILITY_SUCCESS){
                        am.saveNewAllocation(ac.get(), key);
                    }else{
                        Log.w(TAG, "allocation version is not compatible!");
                    }

                    mCDFS.getDrives().get(key).addContainer(ac.get());
                });


                names = getItems(parent);
                dirs = getItemsDir(parent);

                if(names != null) {
                    for (int i = 0; i < names.size(); i++) {
                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
                        f.setName(names.get(i));
                        f.setParents(null);
                        Itemlist.add(f);
                    }
                }

                if(dirs !=null) {
                    java.util.List<String> parentList = new ArrayList<>();
                    parentList.add(parent);
                    for (int i = 0; i < dirs.size(); i++) {
                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
                        f.setName(dirs.get(i));
                        f.setParents(parentList);
                        Itemlist.add(f);
                    }
                }

                filelist.setFiles(Itemlist);
                callback.onCompleted(filelist);
            }

            @Override
            public void onCompletedExceptionally(Throwable throwable) {
                callback.onCompletedExceptionally(throwable);
            }
        });
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
            clause1 = clause1.concat(" =" + "\"" + "Root" + "\"");
        }else{
            clause1 = clause1.concat(" =" + "\"" + parent + "\"");
        }
        /*
            Set filter(clause) attribute folder
         */
        clause2 = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;
        clause2 = clause2.concat(" =" + " 0");

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
            clause1 = clause1.concat(" =" + "\"" + "Root" + "\"");
        }else{
            clause1 = clause1.concat(" =" + "\"" + parent + "\"");
        }
        /*
            Set filter(clause) attribute folder
         */
        clause2 = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;
        clause2 = clause2.concat(" =" + "1");

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
                if (names.get(j).equals(col_name)) {
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
