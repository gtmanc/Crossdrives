package com.crossdrives.cdfs.list;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocChecker;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.AllocationFetcher;
import com.crossdrives.cdfs.allocation.ICallBackAllocationFetch;
import com.crossdrives.cdfs.allocation.Result;
import com.crossdrives.cdfs.allocation.ResultCode;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;

import org.checkerframework.checker.units.qual.A;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
                AllocChecker checker = new AllocChecker();
                AtomicReference<java.util.List<Result>> results = new AtomicReference<>();
                AtomicBoolean globalResult = new AtomicBoolean(true);

                /*
                    Update the fetched allocation content to database. We will query local database
                    for the file list requested by caller.
                 */
                mCDFS.getDrives().forEach((key, value)->{
                    ac.set(am.toContainer(allocations.get(key)));
//                    if(am.checkCompatibility(ac.get()) == IAllocManager.ERR_COMPATIBILITY_SUCCESS){
//                        am.saveAllocItem(ac.get(), key);
//                    }else{
//                        Log.w(TAG, "allocation version is not compatible!");
//                    }
                    /*
                        Each time new allocation fetched, we have to delete the old rows and then insert the new items
                        so that the duplicated rows can be removed.
                    */
                    am.deleteAllExistingByDrive(key);
                    /*
                        Check allocation item traversely and save to database if the item is valid
                        Here we will lost the cause because it could consume large amount of memory.
                    */
                    for(AllocationItem item : ac.get().getAllocItem()) {
                        results.set(checker.checkAllocItem(item));
                        /*
                            We only want to know whether the item is good or not. Therefore, any check
                            failure we treat the item as faulty item. If any faulty item detected, set
                            the global result to false so that we will call back via onCompleteExceptionally.
                        */
                        Log.d(TAG, "Conclusion allocation check: ");
                        if(getConclusion(results.get())){
                            Log.d(TAG, "Successful");
                            am.saveItem(item, key);
                        }else{
                            Log.d(TAG, "Failed");
                            globalResult.set(false);
                        }
                    }

                    mCDFS.getDrives().get(key).addContainer(ac.get());
                });

                names = getItems(parent);
                if(names.stream().filter((name)->{
                    boolean result = true;
                    java.util.List<AllocationItem> items;
                    items = getItemsByName(name);
                    results.set(checker.checkItems(items));
                    if(getConclusion(results.get())){
                        Log.d(TAG, "Successful");
                    }else{
                        Log.d(TAG, "Failed");
                        am.deleteItemsByName(name);
                        result = false;
                    }
                    return result;
                }).count() < names.size()){
                    globalResult.set(false);
                }

                dirs = getItemsDir(parent);
                //TODO: add the same check

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


                if(globalResult.get()){
                    callback.onSuccess(filelist);
                }else{
                    callback.onCompleteExceptionally(filelist, results.get());
                }

            }

            @Override
            public void onCompletedExceptionally(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    private boolean getConclusion(java.util.List<Result> results){
        boolean conlusion = false;
//        if(results.stream().filter((result)->{
//            return (result.getErr() != ResultCode.SUCCESS);
//        }).count() > 0){
//            conlusion = false;
//        }

        if(results.stream().allMatch((r)->{
           return r.getErr() == ResultCode.SUCCESS;
        })){
            conlusion = true;
        }
        return conlusion;
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

    private java.util.List<AllocationItem> getItemsByName(String name){
        java.util.List<AllocationItem> items= new ArrayList<>();
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String clause;
        Cursor cursor = null;
        java.util.List<String> names = null;
        /*
            Set filter(clause) parent
         */
        clause = DBConstants.ALLOCITEMS_LIST_COL_NAME;
        clause = clause.concat(" =" + "\"" + name + "\"");

        Log.w(TAG, "Get items by name. Clause: " + clause);
        cursor = dh.query(clause);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return items;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            return items;
        }

        final int indexName = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_NAME);
        final int indexPath = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_PATH);
        final int indexDrive = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME);
        final int indexSeq = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_SEQUENCE);
        final int indexTotalSeg = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_TOTALSEG);
        final int indexSize = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_SIZE);
        final int indexCDFSSize = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_CDFSITEMSIZE);
        final int indexAttrFolder = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_FOLDER);
        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++) {
            AllocationItem item = new AllocationItem();
            item.setName(cursor.getString(indexName));
            item.setPath(cursor.getString(indexPath));
            item.setDrive(cursor.getString(indexDrive));
            item.setSequence(cursor.getInt(indexSeq));
            item.setTotalSeg(cursor.getInt(indexTotalSeg));
            item.setSize(cursor.getLong(indexSize));
            item.setCDFSItemSize(cursor.getLong(indexCDFSSize));
            item.setAttrFolder(cursor.getInt(indexAttrFolder)>0);
            items.add(item);
            cursor.moveToNext();
        }
        return items;
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