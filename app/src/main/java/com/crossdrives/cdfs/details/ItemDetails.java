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
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
                java.util.List<CdfsItem> items;

                pathParent = Names.CompletePath(Parent.getCurrent(mParents));
                Log.d(TAG, "pathParent: " + pathParent);

                HashMap<String, OutputStream> allocations = fetchMapFuture.join();

                AllocManager am = new AllocManager();
                am.CheckThenUpdateLocalCopy(pathParent, allocations);
                items = buildCdfsItemList(pathParent);

                //build result
                Optional<CdfsItem> fileredItem = items.stream().filter((item)-> item.getId().equals(mItem.getId())).findAny();
                if(!fileredItem.isPresent()){
                    throw new ItemNotFoundException("The specified item is not found in current parent!",new Throwable());
                }

                return buildResult(fileredItem.get());
            }
        });

        return task;
    }

    Result buildResult(CdfsItem item){
        Result r = new Result();

        r.name = item.getName();
        r.size = item.getSize();
        r.created = item.getTimeCreated();
        r.modified = item.getTimeModified();

        return r;
    }
    private @NonNull java.util.List<CdfsItem> buildCdfsItemList(@Nullable String pathParent) {
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        java.util.List<CdfsItem> items = new ArrayList<>();
        final String clause1;
        Cursor cursor = null;

        /*
            Set filter(clause) parent. This actually is not needed anymore because the CDFS is changed to
            multiply parents structure. The local database only contains items which have the same parent(path).
         */
        //clause1 = buildClausePath(pathParent);
        //cursor = dh.query(clause1);

        /*
            Set filter(clause) attribute folder
         */
        //clause2 = buildClauseFolder(folder);

        /*
            First build up a CDFS list with an empty drive item map. The map will be filled in next step.
            Use database group statement to make sure only one entry is got from all entries which
            have the same CDFS ID.
        */
        dh.GroupBy(ALLOCITEMS_LIST_COL_CDFSID);
        cursor = dh.query();    //pass none to read all records
        if(!queryResultCheck(cursor)){return items;}

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
        Log.d(TAG, "start to create cdfs list. cursor count: " + cursor.getCount());
        for(int i = 0 ; i < cursor.getCount(); i++){
            CdfsItem item = new CdfsItem();
            ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
            item.setMap(map);   //Just set a placeholder. We will fill the content in later step.
            item.setName(cursor.getString(indexName));
            item.setId(cursor.getString(indexCDFSId));
            item.setPath(cursor.getString(indexPath));
            //if we are in root, the list is null or empty. In this case, we have to create a list.
            java.util.List<String> list = Parent.toIdList(mParents);
            if(list == null){list= new ArrayList<>();}
            list.add(Parent.getCurrent(mParents).getId());
            item.setParents(list);
            //Solution for get a boolean from db:
            //https://stackoverflow.com/questions/4088080/get-boolean-from-database-using-android-and-sqlite
            item.setFolder(cursor.getInt(indexAttrFolder) > 0 );
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        /*
            Fill the map we created in previous step. Read out the entries which has the same CDFS ID and then put the
            drive item ID to the map according to the drive name.
         */
        Log.d(TAG, "Start to fill map. Number of cdfs items: " + items.size());
        dh.GroupBy(null);   //remove the group clause we setup in previous step
        items.stream().forEach((item)->{    //each cdfs item
            String clause2 = ALLOCITEMS_LIST_COL_CDFSID;
            clause2 = clause2.concat(" = " + "'" + item.getId() + "'");
            Cursor cursor2 = null;
            cursor2 = dh.query(clause2);
            if(!queryResultCheck(cursor2)){return;}
            ConcurrentHashMap<String, java.util.List<String>> map = item.getMap();
            cursor2.moveToFirst();
            for(int i = 0 ; i < cursor2.getCount(); i++){
                String driveName = cursor2.getString(indexDrive);
                // Add ID first time?
                if(map.get(driveName) == null){
                    Log.d(TAG, "Create list. item: " + item.getName() + "drive name: " + driveName);
                    java.util.List<String> list = new ArrayList<>();
                    map.put(driveName, list);
                }
                map.get(driveName).add(cursor2.getString(indexItemId));
                cursor2.moveToNext();
            }
        });

        //dump for debugging
//        {
//            Log.d(TAG, "Dump built cdfs items:");
//            items.stream().forEach((item) -> {
//                Log.d(TAG, "item@" + item.toString());
//                Log.d(TAG, "name: " + item.getName());
//                Log.d(TAG, "map@" + item.getMap().toString());
//                item.getMap().entrySet().stream().forEach((set) -> {
//                    Log.d(TAG, "drive: " + set.getKey() + " id: " + set.getValue());
//                });
//            });
//        }
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
