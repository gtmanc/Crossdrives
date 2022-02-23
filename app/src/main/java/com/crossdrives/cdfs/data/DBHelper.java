package com.crossdrives.cdfs.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.crossdrives.data.DBConstants;

public class DBHelper{
    private final String TAG = "CD.DBHelper";
    com.crossdrives.data.DBHelper dbh;
    ContentValues cv = new ContentValues();

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
        cv.put(ALLOCITEMS_LIST_COL_NAME, name);
        return this;
    }

    /*
        insert multiple rows:
        https://stackoverflow.com/questions/42619923/how-to-insert-multiple-rows-into-sqlite-android
     */
    public long insert(String brand, String name, String mail, Uri photourl, String state){
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
            cv.put(USERPROFILE_TABLE_COL_BRAND, brand);
            cv.put(USERPROFILE_TABLE_COL_NAME, name);
            cv.put(USERPROFILE_TABLE_COL_MAIL, mail);
            if(photourl != null) {
                cv.put(USERPROFILE_TABLE_COL_PHOTOURL, photourl.toString());
            }
            cv.put(USERPROFILE_TABLE_COL_STATE, state);
            r_id = db.insert(USERPROFILE_TABLE_NAME, null,cv);
            db.close();
        }

        return r_id;
    }
}
