package com.example.crossdrives;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    private String TAG = "CD.DBHelper";
    private static final String DATABASE_NAME = "com.crossdrives.database";
    private static final int DATABASE_VERSION = 1;
    private static final String USERPROFILE_TABLE_NAME = DBConstants.TABLE_MASTER_USER_PROFILE;
    private static final String USERPROFILE_TABLE_COL_BRAND = "brand";
    private static final String USERPROFILE_TABLE_COL_NAME = "user_name";
    private static final String USERPROFILE_TABLE_COL_MAIL = "user_mail";
    private static final String USERPROFILE_TABLE_COL_PHOTOURL = "user_photourl";

    SQLiteDatabase mdb = null;

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DBHelper constructed");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DBHelper onCreated");
        String sql_statement = "CREATE TABLE " + USERPROFILE_TABLE_NAME
                + " ("
                //+ USERPROFILE_TABLE_COL_BRAND + "INTEGER AUTO_INCREMENT primary key, "
                + USERPROFILE_TABLE_COL_BRAND + " text primary key, "
                + USERPROFILE_TABLE_COL_NAME + " text, "
                + USERPROFILE_TABLE_COL_MAIL + " text, "
                + USERPROFILE_TABLE_COL_PHOTOURL + " url"
                + ");";
        Log.d(TAG, sql_statement);
        db.execSQL(sql_statement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long insert(String brand, String name, String mail, Uri photourl){
        long r_id = -1;
        ContentValues cv = new ContentValues();

        try{
            mdb = getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(mdb != null){
            cv.put(USERPROFILE_TABLE_COL_BRAND, brand);
            cv.put(USERPROFILE_TABLE_COL_NAME, name);
            cv.put(USERPROFILE_TABLE_COL_MAIL, mail);
            cv.put(USERPROFILE_TABLE_COL_PHOTOURL, photourl.toString());
            r_id = mdb.insert(USERPROFILE_TABLE_NAME, null,cv);
            mdb.close();
        }

        return r_id;
    }

    public Cursor query(){
        Cursor cursor = null;

        try {
            mdb = getReadableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(mdb != null) {
            Log.d(TAG, "Start query");
            cursor = mdb.rawQuery("SELECT * FROM " + USERPROFILE_TABLE_NAME, null);
        }

        return cursor;
    }
}
