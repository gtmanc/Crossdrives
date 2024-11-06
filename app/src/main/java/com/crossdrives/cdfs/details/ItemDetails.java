package com.crossdrives.cdfs.details;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.base.Parent;
import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.Names;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.exception.ItemNotFoundException;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemDetails {
    final String TAG = "CD.ItemDetails";
    CDFS mCDFS;
    java.util.List<CdfsItem> mParents;
    CdfsItem mItem;

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

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

    public ItemDetails(CDFS mCDFS, List<CdfsItem> mParents, CdfsItem item) {
        this.mCDFS = mCDFS;
        this.mParents = mParents;
        mItem = item;
    }

    public Task<Result> execute(){
        Task<Result> task;


        task = Tasks.call(mExecutor, new Callable<Result>()
        {

            @Override
            public Result call() {
                Result result = new Result();

                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> fetchMapFuture =
                        mapFetcher.pullAll(Parent.getCurrent(mParents));
                final String pathParent;
                java.util.List<AllocationItem> items;

                pathParent = Names.CompletePath(Parent.getCurrent(mParents));
                Log.d(TAG, "pathParent: " + pathParent);

                HashMap<String, OutputStream> allocations = fetchMapFuture.join();

                AllocManager am = new AllocManager();
                am.CheckThenUpdateLocalCopy(pathParent, allocations);
                items = RebuildItemList(pathParent, mItem.getId());

                return buildResult(items);
            }
        });

        return task;
    }

    Result buildResult(List<AllocationItem> items){
        Result r = new Result();

        //build result
        Optional<AllocationItem> optional = items.stream().findAny();
        if(!optional.isPresent()){
            throw new ItemNotFoundException("The specified item is not found in current parent!",new Throwable());
        }

        AllocationItem item = optional.get();
        r.name = item.getName();
        r.size = item.getSize();
        r.created = item.getCreatedTime();
        r.modified = item.getLastModifiedTime();
        HashMap<String, Long> sizeMap = new HashMap<>();
        Iterator iterator = items.iterator();
        while(iterator.hasNext()){
            if(sizeMap.get(item.getDrive()) == null) {
                sizeMap.put(item.getDrive(), item.getSize());
                break;
            }
            sizeMap.compute(item.getDrive(), (k, v) -> v + item.getSize());
        }
        r.allocatedSize = sizeMap;
        return r;
    }
    private @NonNull java.util.List<AllocationItem> RebuildItemList(@Nullable String pathParent, String id) {
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        java.util.List<AllocationItem> items = new ArrayList<>();
        String clause;
        Cursor cursor = null;

        clause = ALLOCITEMS_LIST_COL_CDFSID;
        clause = clause.concat(" = " + "'" + id + "'");

        Log.d(TAG, "clausePath: " + clause);
        cursor = dh.query(clause);
        if(!queryResultCheck(cursor)){return items;}

        /*
            Set filter(clause) attribute folder
         */
        //clause2 = buildClauseFolder(folder);


//        dh.GroupBy(ALLOCITEMS_LIST_COL_CDFSID);
//        cursor = dh.query();    //pass none to read all records
//        if(!queryResultCheck(cursor)){return items;}

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
            item.setItemId(cursor.getString(indexItemId));
            item.setPath(cursor.getString(indexPath));
            item.setSequence(cursor.getInt(indexSeq));
            item.setCDFSItemSize(cursor.getLong(indexCDFSSize));
            item.setDrive(cursor.getString(indexDrive));
            item.setCdfsId(cursor.getString(indexCDFSId));
            item.setTotalSeg(cursor.getInt(indexTotalSeg));
            item.setSize(cursor.getLong(indexSize));
            //Solution for get a boolean from db:
            //https://stackoverflow.com/questions/4088080/get-boolean-from-database-using-android-and-sqlite
            item.setAttrFolder(cursor.getInt(indexAttrFolder) > 0 );
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        return items;
    }
    /*
            A helper function for query
         */
    private boolean queryResultCheck(Cursor cursor) {

        if(cursor == null){
            Log.w(TAG, "Cursor is null!");
            return false;
        }
        if(cursor.getCount() <= 0){
            Log.w(TAG, "Count of cursor is zero!");
            cursor.close();
            return false;
        }
        return true;
    }
}
