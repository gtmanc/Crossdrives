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

import java.net.URL;

public class DBHelper extends SQLiteOpenHelper {
    private String TAG = "CD.DBHelper";
    private static final String DATABASE_NAME = DBConstants.DATABASE_NAME;
    private static final int DATABASE_VERSION = DBConstants.DATABASE_VERSION;
    private static final String USERPROFILE_TABLE_NAME = DBConstants.TABLE_MASTER_USER_PROFILE;
    private static final String USERPROFILE_TABLE_COL_BRAND = DBConstants.USERPROFILE_TABLE_COL_BRAND;
    private static final String USERPROFILE_TABLE_COL_NAME = DBConstants.USERPROFILE_TABLE_COL_NAME;
    private static final String USERPROFILE_TABLE_COL_MAIL = DBConstants.USERPROFILE_TABLE_COL_MAIL;
    private static final String USERPROFILE_TABLE_COL_PHOTOURL = DBConstants.USERPROFILE_TABLE_COL_PHOTOURL;
    private static final String USERPROFILE_TABLE_COL_STATE = DBConstants.USERPROFILE_TABLE_COL_STATE;

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
                + "INTEGER AUTO_INCREMENT primary key, "
                + USERPROFILE_TABLE_COL_BRAND + " text, "
                + USERPROFILE_TABLE_COL_NAME + " text, "
                + USERPROFILE_TABLE_COL_MAIL + " text, "
                + USERPROFILE_TABLE_COL_PHOTOURL + " url, "
                + USERPROFILE_TABLE_COL_STATE + " text"
                + ");";
        Log.d(TAG, sql_statement);
        db.execSQL(sql_statement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long insert(String brand, String name, String mail, Uri photourl, String state){
        long r_id = -1;
        ContentValues cv = new ContentValues();

        Log.d(TAG, "Insert");
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
            cv.put(USERPROFILE_TABLE_COL_STATE, state);
            r_id = mdb.insert(USERPROFILE_TABLE_NAME, null,cv);
            mdb.close();
        }

        return r_id;
    }

    /*
    * If no argument is giving, all columns are read
    * The input arguments are the column name and condition which will be used to filter the queried result further
    * Note: so far, two conditions and AND operator are supported!
    * */
    public Cursor query(String ... expression){
        Cursor cursor = null;
        String statement;

        /*
        So far, two conditions and AND operator are supported
         */
        if(expression.length > 4)
        {
            Log.w(TAG, "Too many conditions are required!");
            return null;
        }

        try {
            mdb = getReadableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(expression.length == 0) {
            Log.d(TAG, "Query all");
            statement = "SELECT * FROM " + USERPROFILE_TABLE_NAME + ";";
        }else{
            statement = "SELECT * "
                       + " FROM " + USERPROFILE_TABLE_NAME
                       + " WHERE " + expression[0] + " = " + expression[1] + " AND "
                       + expression[2] + " = " + expression[3]
                       + ";";
            Log.d(TAG, "Query required: " + statement);
        }

        if(mdb != null)    {
            cursor = mdb.rawQuery(statement, null);
        }

        return cursor;
    }
}
