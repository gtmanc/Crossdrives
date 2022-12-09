package com.crossdrives.cdfs.list;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.ICallBackMapFetch;
import com.crossdrives.cdfs.allocation.Result;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class List {
    final String TAG = "CD.List";
    CDFS mCDFS;

    //Columns
    final String ALLOCITEMS_LIST_COL_NAME = DBConstants.ALLOCITEMS_LIST_COL_NAME;
    final String ALLOCITEMS_LIST_COL_PATH = DBConstants.ALLOCITEMS_LIST_COL_PATH;
    final String ALLOCITEMS_LIST_COL_DRIVENAME = DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME;
    final String ALLOCITEMS_LIST_COL_CDFSID = DBConstants.ALLOCITEMS_LIST_COL_CDFSID;
    final String ALLOCITEMS_LIST_COL_ITEMID = DBConstants.ALLOCITEMS_LIST_COL_ITEMID;
    final String ALLOCITEMS_LIST_COL_SEQUENCE = DBConstants.ALLOCITEMS_LIST_COL_SEQUENCE;
    final String ALLOCITEMS_LIST_COL_TOTALSEG = DBConstants.ALLOCITEMS_LIST_COL_TOTALSEG;
    final String ALLOCITEMS_LIST_COL_SIZE = DBConstants.ALLOCITEMS_LIST_COL_SIZE;
    final String ALLOCITEMS_LIST_COL_CDFSITEMSIZE = DBConstants.ALLOCITEMS_LIST_COL_CDFSITEMSIZE;
    final String ALLOCITEMS_LIST_COL_FOLDER = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;

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
        MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
        mapFetcher.fetchAll(parent, new ICallBackMapFetch<HashMap<String, OutputStream>>() {
            @Override
            public void onCompleted(HashMap<String, OutputStream> allocations)  {
                java.util.List<AllocationItem> children, dirs;
                AtomicReference<AllocContainer> ac = new AtomicReference<>();
                AllocManager am = new AllocManager(mCDFS);
                //Checker checker = new Checker();
                AtomicReference<java.util.List<Result>> results = new AtomicReference<>();
                AtomicBoolean globalResult = new AtomicBoolean(true);


                globalResult.set(am.CheckThenUpdateLocalCopy(parent, allocations));
//                /*
//                    Update the fetched allocation content to database. We will query local database
//                    for the file list requested by caller.
//                 */
//                mCDFS.getDrives().forEach((key, value)->{
//                    ac.set(am.toContainer(allocations.get(key)));
////                    if(am.checkCompatibility(ac.get()) == IAllocManager.ERR_COMPATIBILITY_SUCCESS){
////                        am.saveAllocItem(ac.get(), key);
////                    }else{
////                        Log.w(TAG, "allocation version is not compatible!");
////                    }
//                    /*
//                        Each time new allocation fetched, we have to delete the old rows and then insert the new items
//                        so that the duplicated rows can be removed.
//                    */
//                    am.deleteAllExistingByDrive(key);
//                    /*
//                        Check allocation item traversely and save to database if the item is valid
//                        Here we will lost the cause because it could consume large amount of memory.
//                    */
//                    for(AllocationItem item : ac.get().getAllocItem()) {
//                        results.set(checker.checkAllocItem(item));
//                        /*
//                            We only want to know whether the item is good or not. Therefore, any check
//                            failure we treat the item as faulty item. If any faulty item detected, set
//                            the global result to false so that we can call back to via onCompleteExceptionally.
//                            instead onSuccess.
//                        */
//                        if(getConclusion(results.get())){
//                            am.saveItem(item, key);
//                        }else{
//                            Log.w(TAG, "Single item check: faulty item detected ");
//                            globalResult.set(false);
//                        }
//                    }
//
//                    mCDFS.getDrives().get(key).addContainer(ac.get());
//                });
//
//                /*
//                    To do the item cross check, the database functionality is utilized.
//                    Build name list contains the unique names. We will use the list to query local
//                    database for cross item check.
//                 */
//                complete = new ArrayList<>();
//                names = getNonDirItems(parent);
//                if(names != null){complete.addAll(names);}
//                dirs = getItemsDir(parent);
//                if(dirs != null){complete.addAll(dirs);}
//
//                if(complete != null) {
//                    if (complete.stream().filter((name) -> {
//                        boolean result = true;
//                        java.util.List<AllocationItem> items;
//                        items = getItemsByName(name);
//                        results.set(checker.checkItemsCrossly(items));
//                        if (getConclusion(results.get())) {
//                        } else {
//                            Log.w(TAG, "Corss item check: faulty items detected ");
//                            am.deleteItemsByName(name);
//                            result = false;
//                        }
//                        return result;
//                    }).count() < complete.size()) {
//                        globalResult.set(false);
//                    }
//                }

                /*
                    The faulty items have been removed from database if detected in previous step.
                    Rebuild name list for the list result to be sent to callback.
                 */
                children = getItemsByFolder(parent, false);
                dirs = getItemsByFolder(parent, true);
                if(children != null) {
                    for (int i = 0; i < children.size(); i++) {
                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
                        f.setName(children.get(i).getName());
                        f.setId(children.get(i).getCdfsId());
                        f.setParents(null);
                        Itemlist.add(f);
                    }
                }

                if(dirs !=null) {
                    java.util.List<String> parentList = new ArrayList<>();
                    parentList.add(parent);
                    for (int i = 0; i < dirs.size(); i++) {
                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
                        f.setName(children.get(i).getName());
                        f.setId(children.get(i).getCdfsId());
                        f.setParents(parentList);
                        Itemlist.add(f);
                    }
                }

                filelist.setFiles(Itemlist);

                //Page size for various drives
                //Google: integer
                // The maximum number of files to return per page. Partial or empty result pages are possible
                // even before the end of the files list has been reached. Acceptable values are 1 to 1000, inclusive. (Default: 100)
                //Microsoft
                //
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

    private java.util.List<AllocationItem> getItemsByFolder(String parent, boolean folder){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        java.util.List<AllocationItem> items= new ArrayList<>();
        String clause1, clause2;
        Cursor cursor = null;

        /*
            Set filter(clause) parent
         */
        clause1 = ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            clause1 = clause1.concat(" =" + "\"" + "Root" + "\"");
        }else{
            clause1 = clause1.concat(" =" + "\"" + parent + "\"");
        }
        /*
            Set filter(clause) attribute folder
         */
        clause2 = ALLOCITEMS_LIST_COL_FOLDER;
        if(folder) {
            clause2 = clause2.concat(" =" + " 1");
        }else{
            clause2 = clause2.concat(" =" + " 0");
        }
        Log.w(TAG, "Get items not a dir. Clause: " + clause1 + " and " + clause2);

        dh.GroupBy(ALLOCITEMS_LIST_COL_CDFSID);
        cursor = dh.query(clause1, clause2);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return items;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            cursor.close();
            return items;
        }
        final int indexName = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_NAME);
        final int indexPath = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_PATH);
        final int indexDrive = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME);
        final int indexCDFSId = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_CDFSID);
        final int indexItemId = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_ITEMID);
        final int indexSeq = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_SEQUENCE);
        final int indexTotalSeg = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_TOTALSEG);
        final int indexSize = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_SIZE);
        final int indexCDFSSize = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_CDFSITEMSIZE);
        final int indexAttrFolder = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_FOLDER);
        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++){
            AllocationItem item = new AllocationItem();
            item.setName(cursor.getString(indexName));
            item.setPath(cursor.getString(indexPath));
            item.setDrive(cursor.getString(indexDrive));
            item.setCdfsId(cursor.getString(indexCDFSId));
            item.setItemId(cursor.getString(indexItemId));
            item.setSequence(cursor.getInt(indexSeq));
            item.setTotalSeg(cursor.getInt(indexTotalSeg));
            item.setSize(cursor.getLong(indexSize));
            item.setCDFSItemSize(cursor.getLong(indexCDFSSize));
            item.setAttrFolder(cursor.getInt(indexAttrFolder)>0);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();
        //names = new utils().buildNameList(cursor);
        return items;
    }
}
