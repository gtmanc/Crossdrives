package com.crossdrives.cdfs.list;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.ICallBackMapFetch;
import com.crossdrives.cdfs.allocation.Result;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
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


    public Task<FileList> execute(@Nullable AllocationItem parent){
        Task<FileList> task;

        task = Tasks.call(mExecutor, new Callable<FileList>()
        {

            @Override
            public FileList call() throws Exception {
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> fetchMapFuture = mapFetcher.pullAll(null);

                return null;
            }
        });

        return task;
    }

    public void list(@Nullable AllocationItem parent, ICallbackList<FileList> callback) {
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
                java.util.List<AllocationItem> children, dirs;
                AtomicReference<AllocContainer> ac = new AtomicReference<>();
                AllocManager am = new AllocManager(mCDFS);
                //Checker checker = new Checker();
                AtomicReference<java.util.List<Result>> results = new AtomicReference<>();
                AtomicBoolean globalResult = new AtomicBoolean(true);


                globalResult.set(am.CheckThenUpdateLocalCopy(path, allocations));

                /*
                    The faulty items have been removed from database if detected in previous step.
                    Rebuild name list for the list result to be sent to callback.
                 */
                children = getItemsByFolder(path, false);
                dirs = getItemsByFolder(path, true);
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
                    for (int i = 0; i < dirs.size(); i++) {
                        com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
                        f.setName(children.get(i).getName());
                        f.setId(children.get(i).getCdfsId());
                        Itemlist.add(f);
                    }
                }

                filelist.setFiles(Itemlist);
                //#42 TODO.Directly tell caller there is no next page for the time being.
                filelist.setNextPageToken(null);

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
