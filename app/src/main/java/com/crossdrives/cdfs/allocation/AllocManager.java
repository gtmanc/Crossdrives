package com.crossdrives.cdfs.allocation;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.IAllocManager;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.gson.Gson;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AllocManager implements IAllocManager {
    static private final String TAG = "CD.AllocManager";
    static private final int mVersion = 1;
    private String mDriveName;
    static CDFS mCDFS;

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

    public AllocManager(CDFS cdfs) { mCDFS = cdfs;}

    public AllocContainer toContainer(OutputStream stream){
        AllocContainer container;
        Gson gson = new Gson();
        Drive drive;

        container = gson.fromJson(stream.toString(), AllocContainer.class);
        return container;
    }

    public int checkCompatibility(AllocContainer container){
        int result = ERR_COMPATIBILITY_SUCCESS;

        /*
            Expect more checks need to be added in the future.
         */
        if(container.getVersion() != mVersion){
            result = ERR_COMPATIBILITY_VER_NOT_COMPATIBLE;
            Log.w(TAG, "Expected version: " + mVersion + " version in container: " + container.getVersion());
        }

        return result;
    }

    /*
        Create a new allocation file
     */
    public String newAllocation(){
        AllocContainer container = new AllocContainer();
        Gson gson = new Gson();
        String json;
        container.setVersion(mVersion);
        /*
            Add test content
         */
        if(mDriveName.contains("Google")) {
            Log.d(TAG, "Add test allocation item: Google, seq = 1");
            addTestContentGoogle(container);
        }
        if(mDriveName.contains("Microsoft")) {
            Log.d(TAG, "Add test allocation item: Microsoft, seq = 2");
            addTestContentMicrosoft(container);
        }

        json = gson.toJson(container);
        return json;
    }

    public boolean CheckThenUpdateLocalCopy(String parent, HashMap<String, OutputStream> allocations){
        AtomicReference<AllocContainer> ac = new AtomicReference<>();
        Checker checker = new Checker();
        AtomicBoolean globalResult = new AtomicBoolean(true);
        java.util.List<String> whole;
        AtomicReference<java.util.List<Result>> results = new AtomicReference<>();

        /*
            Update the fetched allocation content to database. We will query local database
            for the file list requested by caller.
        */
        mCDFS.getDrives().forEach((key, value)->{
            ac.set(toContainer(allocations.get(key)));
            /*
                Each time new allocation fetched, we have to delete the old rows and then insert the new items
                so that the duplicated rows can be removed.
                TODO: this may not be the best way.
            */
            deleteAllExistingByDrive(key);
            /*
                Check allocation item traversely and save to database if the item is valid
                Here we will lost the cause because it could consume large amount of memory.
            */
            for(AllocationItem item : ac.get().getAllocItem()) {
                results.set(checker.checkAllocItem(item));
                /*
                    We only want to know whether the item is good or not. Therefore, any check
                    failure we treat the item as faulty item. If any faulty item detected, set
                    the global result to false so that we can call back to via onCompleteExceptionally.
                    instead onSuccess.
                */
                if(getConclusion(results.get())){
                    saveItem(item, key);
                }else{
                    Log.w(TAG, "Single item check: faulty item detected ");
                    globalResult.set(false);
                }
            }

            mCDFS.getDrives().get(key).addContainer(ac.get());
        });

        /*
            To do the item cross check, the database functionality is utilized.
            Build name list contains the unique names. We will use the list to query local
            database for cross item check.
        */
//        whole = getNameList(parent);
//        if(whole != null) {
//            if (whole.stream().filter((name) -> {
//                boolean result = true;
//                java.util.List<AllocationItem> items;
//                items = getItemsByName(name);
//                results.set(checker.checkItemsCrossly(items));
//                if (getConclusion(results.get())) {
//                } else {
//                    Log.w(TAG, "Corss item check: faulty items detected ");
//                    deleteItemsByName(name);
//                    result = false;
//                }
//                return result;
//            }).count() < whole.size()) {
//                globalResult.set(false);
//            }
//        }

        java.util.List<String> IDs;
        IDs = getCdfsIdList(parent);
        if(IDs != null) {
            Log.d(TAG, "Cross item check...");
            if (IDs.stream().filter((id) -> {
                boolean result = true;
                java.util.List<AllocationItem> items;
                items = getItemsByID(id);
                //Log.d(TAG, "Size of items: " + items.size());
                results.set(checker.checkItemsCrossly(items));
                if (getConclusion(results.get())) {
                } else {
                    Log.w(TAG, "faulty items detected ");
                    deleteItemsByID(id);
                    result = false;
                }
                return result;
            }).count() < IDs.size()) {
                globalResult.set(false);
            }
        }

        return globalResult.get();
    }

    private boolean getConclusion(java.util.List<Result> results){
        boolean conlusion = false;

        if(results.stream().allMatch((r)->{
            return r.getErr() == ResultCode.SUCCESS;
        })){
            conlusion = true;
        }
        return conlusion;
    }

    private List<Result> grtErrorList(){
        List<Result> errors= new ArrayList<>();

        return errors;
    }

    private java.util.List<String> getNameList(String parent){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String filter;
        Cursor cursor = null;
        java.util.List<String> names = new ArrayList<>();
        java.util.List<String> selects= new ArrayList<>();

        /*
            Set filter for parent
         */
        filter = DBConstants.ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            filter = filter.concat(" =" + "\"" + "Root" + "\"");
        }else{
            filter = filter.concat(" =" + "\"" + parent + "\"");
        }

        Log.d(TAG, "Get name list. Filter clause: " + filter);

        dh.GroupBy(ALLOCITEMS_LIST_COL_NAME);
        selects.add(ALLOCITEMS_LIST_COL_NAME);
        dh.Select(selects);
        cursor = dh.query(filter);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return names;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            cursor.close();
            return names;
        }

        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++){
            Log.d(TAG, "Name: " + cursor.getString(cursor.getColumnIndex(ALLOCITEMS_LIST_COL_NAME)));
            names.add(cursor.getString(cursor.getColumnIndex(ALLOCITEMS_LIST_COL_NAME)));
            cursor.moveToNext();
        }

        //names = new utils().buildNameList(cursor);
        cursor.close();
        return names;
    }

    private java.util.List<String> getCdfsIdList(String parent){
        java.util.List<String> IDs= new ArrayList<>();
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String filter;
        java.util.List<String> selects= new ArrayList<>();
        Cursor cursor = null;

        /*
            Set filter(clause) parent
         */
        filter = DBConstants.ALLOCITEMS_LIST_COL_PATH;
        if(parent == null) {
            filter = filter.concat(" =" + "\"" + "Root" + "\"");
        }else{
            filter = filter.concat(" =" + "\"" + parent + "\"");
        }

        Log.d(TAG, "Get CDFS ID list. Filter clause: " + filter);

        selects.add(DBConstants.ALLOCITEMS_LIST_COL_CDFSID);
        dh.Select(selects);
        dh.GroupBy(ALLOCITEMS_LIST_COL_CDFSID);

        cursor = dh.query(filter);

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return IDs;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            cursor.close();
            return IDs;
        }

        //IDs = new utils().buildCdfsIdList(cursor);
        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++){
            Log.d(TAG, "CDFS ID: " + cursor.getString(cursor.getColumnIndex(ALLOCITEMS_LIST_COL_CDFSID)));
            IDs.add(cursor.getString(cursor.getColumnIndex(ALLOCITEMS_LIST_COL_CDFSID)));
            cursor.moveToNext();
        }
        cursor.close();
        return IDs;
    }

    private java.util.List<AllocationItem> getItemsByName(String name){
        java.util.List<AllocationItem> items= new ArrayList<>();
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String filter;
        Cursor cursor = null;
        java.util.List<String> names = null;

        /*
            Set filter(clause) parent
         */
        filter = DBConstants.ALLOCITEMS_LIST_COL_NAME;
        filter = filter.concat(" =" + "\"" + name + "\"");

        Log.w(TAG, "Get items by name. Clause: " + filter);
        cursor = dh.query(filter);

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
        for(int i = 0 ; i < cursor.getCount(); i++) {
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
        return items;
    }

    private java.util.List<AllocationItem> getItemsByID(String id){
        java.util.List<AllocationItem> items= new ArrayList<>();
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        String filter;
        Cursor cursor = null;
        java.util.List<String> names = null;

        /*
            Set filter(clause) parent
         */
        filter = DBConstants.ALLOCITEMS_LIST_COL_CDFSID;
        filter = filter.concat(" =" + "\"" + id + "\"");

        Log.d(TAG, "Get items by ID. Clause: " + filter);
        cursor = dh.query(filter);

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
        for(int i = 0 ; i < cursor.getCount(); i++) {
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
        return items;
    }

    public void setDriveNameForTest(String name){mDriveName = name;}
    private void addTestContentGoogle(AllocContainer container){
        AllocationItem item = new AllocationItem();
        AllocationItem item2 = new AllocationItem();
        item.setDrive("Google");
        item.setName("Test1.txt");
        item.setCdfsId("CDFSID_GoogleMicrosoft_Text1");
        item.setItemId("google_text1");
        item.setPath("Root");
        item.setSequence(1);
        item.setTotalSeg(2);
        item.setSize(32);
        item.setCDFSItemSize(64);
        item.setAttrFolder(false);
        container.setAllocItem(item);

        item2.setDrive("Google");
        item2.setName("Test1.txt");
        item2.setCdfsId("CDFSID_GoogleMicrosoft_Text1-1");
        item2.setItemId("google_text1-1");
        item2.setPath("Root");
        item2.setSequence(1);
        item2.setTotalSeg(2);
        item2.setSize(32);
        item2.setCDFSItemSize(64);
        item2.setAttrFolder(false);
        container.setAllocItem(item2);
    }
    private void addTestContentMicrosoft(AllocContainer container){
        AllocationItem item = new AllocationItem();
        AllocationItem item2 = new AllocationItem();
        item.setDrive("Microsoft");
        item.setName("Test1.txt");
        item.setCdfsId("CDFSID_GoogleMicrosoft_Text1");
        item.setItemId("Microsoft_text1");
        item.setPath("Root");
        item.setSequence(2);
        item.setTotalSeg(2);
        item.setSize(32);
        item.setCDFSItemSize(64);
        item.setAttrFolder(false);
        container.setAllocItem(item);

        item2.setDrive("Microsoft");
        item2.setName("Test1.txt");
        item2.setCdfsId("CDFSID_GoogleMicrosoft_Text1-1");
        item2.setItemId("Microsoft_text1-1");
        item2.setPath("Root");
        item2.setSequence(2);
        item2.setTotalSeg(2);
        item2.setSize(32);
        item2.setCDFSItemSize(64);
        item2.setAttrFolder(false);
        container.setAllocItem(item2);
    }

    public void saveItem(AllocationItem item, String drive)
    {
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());


        Log.d(TAG, "Save new allocation. Drive: " + drive);

        dh.setName(item.getName());
        dh.setDrive(item.getDrive());
        dh.setPath(item.getPath());
        dh.setCdfsID(item.getCdfsId());
        dh.setItemID(item.getItemId());
        dh.setSequence(item.getSequence());
        dh.setTotalSegment(item.getTotalSeg());
        dh.setSize(item.getSize());
        dh.setCDFSItemSize(item.getCDFSItemSize());
        dh.setAttrFolder(item.getAttrFolder());
        dh.insert();
    }

    public int deleteAllExistingByDrive(String drive){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        int deleted;

        deleted = dh.delete(DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME, "\"" + drive + "\"");
        Log.d(TAG, "Drive " + drive + " items have been deleted: " + deleted);
        return deleted;
    }

    public int deleteItemsByName(String name){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        int deleted;

        deleted = dh.delete(DBConstants.ALLOCITEMS_LIST_COL_NAME, "\"" + name + "\"");
        Log.d(TAG, "Name " + name + " items have been deleted: " + deleted);
        return deleted;
    }

    public int deleteItemsByID(String ID){
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        int deleted;

        deleted = dh.delete(DBConstants.ALLOCITEMS_LIST_COL_CDFSID, "\"" + ID + "\"");
        Log.d(TAG, "ID " + ID + " items have been deleted: " + deleted);
        return deleted;
    }

    public OutputStream upload(File file){
        OutputStream stream = null;

        return stream;
    }

}
