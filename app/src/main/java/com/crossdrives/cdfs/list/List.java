package com.crossdrives.cdfs.list;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.ICallBackMapFetch;
import com.crossdrives.cdfs.allocation.Names;
import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class List {
    final String TAG = "CD.List";
    CDFS mCDFS;
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

    public List(CDFS cdfs) {
        mCDFS = cdfs;
    }


    public Task<ListResult> execute(@Nullable java.util.List<CdfsItem> parents){
        Task<ListResult> task;


        task = Tasks.call(mExecutor, new Callable<ListResult>()
        {

            @Override
            public ListResult call() {
                ListResult result = new ListResult();
                //ConcurrentHashMap<String, Drive> drives = ApplicableDriveListBuilder.build(mCDFS.getDrives(), parent);
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> fetchMapFuture = mapFetcher.pullAll(parent);
                final String pathParent;
                java.util.List<CdfsItem> items;

                //Prepare the path string that we will use to query database items
//                if(parent != null) {pathParent = parent.getPath() + parent.getName();}
//                else{pathParent = IConstant.CDFS_PATH_BASE;}
                pathParent = Names.CompletePath(parent);
                Log.d(TAG, "pathParent: " + pathParent);

                HashMap<String, OutputStream> allocations = fetchMapFuture.join();
                AllocManager am = new AllocManager();

                am.CheckThenUpdateLocalCopy(pathParent, allocations);

                items = buildCdfsItemList(pathParent);
                result.setItems(items);
                return result;
            }
        });

        return task;
    }

    void list(@Nullable AllocationItem parent, ICallbackList<java.util.List<CdfsItem>> callback) {
        FileList filelist = new FileList();
        java.util.List<com.google.api.services.drive.model.File> Itemlist = new ArrayList<>();
        java.util.List<com.google.api.services.drive.model.File> Dirlist = new ArrayList<>();
        final String path;

        if(parent != null) {path = parent.getPath();}
        else{path = null;}

        /*
            Fetch the remote allocation files. The fetcher will ensure all of the allocations file
            are downloaded. We will search the local database for the file list as soon as the
            content of the downloaded allocation files are updated to local database.
         */
        MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
        mapFetcher.fetchAll(path, new ICallBackMapFetch<HashMap<String, OutputStream>>() {
            @Override
            public void onCompleted(HashMap<String, OutputStream> allocations)  {
                java.util.List<CdfsItem> children, dirs;
                AtomicReference<AllocContainer> ac = new AtomicReference<>();
                AllocManager am = new AllocManager();
                //Checker checker = new Checker();
                AtomicReference<java.util.List<ListResult>> results = new AtomicReference<>();
                AtomicBoolean globalResult = new AtomicBoolean(true);


                globalResult.set(am.CheckThenUpdateLocalCopy(path, allocations));

                /*
                    The faulty items have been removed from database if detected in previous step.
                    Rebuild name list for the list result to be sent to callback.
                 */
//                children = getItemsByFolder(path, false);
//                dirs = getItemsByFolder(path, true);
                children = buildCdfsItemList(path);
//                for (int i = 0; i < children.size(); i++) {
//                    com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
//                    f.setName(children.get(i).getName());
//                    f.setId(children.get(i).getCdfsId());
//                    f.setParents(null);
//                    Itemlist.add(f);
//                }
//
//
//                if(dirs !=null) {
//                    for (int i = 0; i < dirs.size(); i++) {
//                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
//                        f.setName(children.get(i).getName());
//                        f.setId(children.get(i).getCdfsId());
//                        Itemlist.add(f);
//                    }
//                }
//
//                filelist.setFiles(Itemlist);
//                //#42 TODO.Directly tell caller there is no next page for the time being.
//                filelist.setNextPageToken(null);

                //Page size for various drives
                //Google: integer
                // The maximum number of files to return per page. Partial or empty result pages are possible
                // even before the end of the files list has been reached. Acceptable values are 1 to 1000, inclusive. (Default: 100)
                //Microsoft
                //
                if(globalResult.get()){
                    callback.onSuccess(children);
                }else{
                    callback.onCompleteExceptionally(children, results.get());
                }

            }

            @Override
            public void onCompletedExceptionally(Throwable throwable) {
                callback.onFailure(throwable);
            }
        });
    }

    /*
        Build list of CDFS items from local database
        Input:
            pathParent: the cdfs display path string
     */
    private @NonNull java.util.List<CdfsItem> buildCdfsItemList(@Nullable String pathParent) {
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        java.util.List<CdfsItem> items = new ArrayList<>();
        final String clause1;
        Cursor cursor = null;

        /*
            Set filter(clause) parent. This actually is not needed anymore because the CDFS is changed to
            multiply parents structure. The local database only contains items which have the same parent(path).
         */
        clause1 = buildClausePath(pathParent);

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
        cursor = dh.query(clause1);
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
        for(int i = 0 ; i < cursor.getCount(); i++){
            CdfsItem item = new CdfsItem();
            ConcurrentHashMap<String, java.util.List<String>> map = new ConcurrentHashMap<>();
            item.setMap(map);   //Just set a placeholder. We will fill the content in later step.
            item.setName(cursor.getString(indexName));
            item.setId(cursor.getString(indexCDFSId));
            item.setPath(cursor.getString(indexPath));
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
        dh.GroupBy(null);   //remove the group clause we setup in previous step
        items.stream().forEach((item)->{    //each cdfs item
            String clause2 = ALLOCITEMS_LIST_COL_CDFSID;
            clause2 = clause2.concat(" = " + "'" + item.getId() + "'");
            Cursor cursor2 = null;
            cursor2 = dh.query(clause1, clause2);
            if(!queryResultCheck(cursor2)){return;}
            ConcurrentHashMap<String, java.util.List<String>> map = item.getMap();
            cursor2.moveToFirst();
            for(int i = 0 ; i < cursor2.getCount(); i++){
                String driveName = cursor2.getString(indexDrive);
                // Add ID first time?
                if(map.get(driveName) == null){
                    java.util.List<String> list = new ArrayList<>();
                    map.put(driveName, list);
                }
                map.get(driveName).add(cursor2.getString(indexItemId));
                cursor2.moveToNext();
            }
        });

        return items;
    }

    String buildClausePath(@Nullable String parent){
        String clause = ALLOCITEMS_LIST_COL_PATH;
        if (parent == null) {
            clause = clause.concat(" = " + "'" + IConstant.CDFS_PATH_BASE + "' ");
            //clause = clause.concat(" = " + "'\\' ");
        } else {
            clause = clause.concat(" = " + "'" + parent + "'");
        }
        Log.d(TAG, "buildClausePath: " + clause);
        return clause;
    }

    String buildClauseFolder(boolean folder){
        String clause = ALLOCITEMS_LIST_COL_FOLDER;
        if (folder) {
            clause = clause.concat(" =" + " 1");
        } else {
            clause = clause.concat(" =" + " 0");
        }
        return clause;
    }

    /*
        A helper function for query
     */
    boolean queryResultCheck(Cursor cursor) {

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

    void closeCursor(Cursor cursor){
        if(cursor != null){ cursor.close();}
    }
    /*
        Get grouped item(s) from local database. Only an item is got even if multiple entries in local
        database have the same CDFS ID
     */
    private java.util.List<AllocationItem> getItemsByFolder(@Nullable String parent, boolean folder){
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

        /*
            Use database group statement to make sure only one entry is got from all entries which
            have the sme CDFS ID
        */
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
        //Names = new utils().buildNameList(cursor);
        return items;
    }
}
