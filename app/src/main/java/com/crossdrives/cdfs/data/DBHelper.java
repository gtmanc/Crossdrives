package com.crossdrives.cdfs.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.crossdrives.data.DBConstants;

import java.util.ArrayList;
import java.util.List;

public class DBHelper{
    private final String TAG = "CD.DBHelper";
    com.crossdrives.data.DBHelper dbh;
    List<ContentValues> cvs = new ArrayList<>();

    final String  TABLE_ALLOCITEM_LIST = DBConstants.TABLE_ALLOCITEM_LIST;
    //Columns
    final String ALLOCITEMS_LIST_COL_NAME = DBConstants.ALLOCITEMS_LIST_COL_NAME;
    final String ALLOCITEMS_LIST_COL_PATH = DBConstants.ALLOCITEMS_LIST_COL_PATH;
    final String ALLOCITEMS_LIST_COL_DRIVENAME = DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME;
    final String ALLOCITEMS_LIST_COL_SEQUENCE = DBConstants.ALLOCITEMS_LIST_COL_SEQUENCE;
    final String ALLOCITEMS_LIST_COL_TOTALSEG = DBConstants.ALLOCITEMS_LIST_COL_TOTALSEG;
    final String ALLOCITEMS_LIST_COL_SIZE = DBConstants.ALLOCITEMS_LIST_COL_SIZE;
    final String ALLOCITEMS_LIST_COL_CDFSITEMSIZE = DBConstants.ALLOCITEMS_LIST_COL_CDFSITEMSIZE;

    public DBHelper(Context context) {
        dbh = new com.crossdrives.data.DBHelper(context, null, null, 0);
    }

    public DBHelper setName(String name){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_NAME, name);
        return this;
    }

    public DBHelper setPath(String path){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_PATH, path);
        return this;
    }

    public DBHelper setDrive(String drive){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_DRIVENAME, drive);
        return this;
    }

    public DBHelper setSequence(int seq){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_SEQUENCE, seq);
        return this;
    }

    public DBHelper setTotalSegment(int total){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_TOTALSEG, total);
        return this;
    }

    public DBHelper setSize(long size){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_SIZE, size);
        return this;
    }

    public DBHelper setCDFSItemSize(long size){
        ContentValues cv;
        cv = cvs.get(0);
        cv.put(ALLOCITEMS_LIST_COL_CDFSITEMSIZE, size);
        return this;
    }

    public long insert(){
        SQLiteDatabase db = null;
        long r_id = -1;


        Log.d(TAG, "Insert");
        try{
;            db = dbh.getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed: " + e.getMessage());
        }

        if(db != null){
            r_id = db.insert(TABLE_ALLOCITEM_LIST, null, cvs.get(0));
            db.close();
        }

        return r_id;
    }

    /*
        insert multiple rows:
        https://stackoverflow.com/questions/42619923/how-to-insert-multiple-rows-into-sqlite-android
    */
    public void insertMultiple(){
        SQLiteDatabase db = null;

        try{
            db = dbh.getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed: " + e.getMessage());
        }

        if(db != null){
            db.beginTransaction();
            
            db.insert(TABLE_ALLOCITEM_LIST, null, cvs.get(0));

            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        }
    }
}
