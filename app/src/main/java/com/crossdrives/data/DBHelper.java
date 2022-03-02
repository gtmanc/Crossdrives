package com.crossdrives.data;

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
    private static final String DATABASE_NAME = DBConstants.DATABASE_NAME;
    private static final int DATABASE_VERSION = DBConstants.DATABASE_VERSION;
    private static final String USERPROFILE_TABLE_NAME = DBConstants.TABLE_MASTER_USER_PROFILE;
    private static final String USERPROFILE_TABLE_COL_BRAND = DBConstants.USERPROFILE_TABLE_COL_BRAND;
    private static final String USERPROFILE_TABLE_COL_NAME = DBConstants.USERPROFILE_TABLE_COL_NAME;
    private static final String USERPROFILE_TABLE_COL_MAIL = DBConstants.USERPROFILE_TABLE_COL_MAIL;
    private static final String USERPROFILE_TABLE_COL_PHOTOURL = DBConstants.USERPROFILE_TABLE_COL_PHOTOURL;
    private static final String USERPROFILE_TABLE_COL_STATE = DBConstants.USERPROFILE_TABLE_COL_STATE;

    private static final String TABLE_ALLOCITEM_LIST = DBConstants.TABLE_ALLOCITEM_LIST;
    private static final String ALLOCITEMS_LIST_COL_NAME = DBConstants.ALLOCITEMS_LIST_COL_NAME;
    private static final String ALLOCITEMS_LIST_COL_PATH = DBConstants.ALLOCITEMS_LIST_COL_PATH;
    private static final String ALLOCITEMS_LIST_COL_DRIVENAME = DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME;
    private static final String ALLOCITEMS_LIST_COL_SEQUENCE = DBConstants.ALLOCITEMS_LIST_COL_SEQUENCE;
    private static final String ALLOCITEMS_LIST_COL_TOTALSEG = DBConstants.ALLOCITEMS_LIST_COL_TOTALSEG;
    private static final String ALLOCITEMS_LIST_COL_SIZE = DBConstants.ALLOCITEMS_LIST_COL_SIZE;
    private static final String ALLOCITEMS_LIST_COL_CDFSITEMSIZE = DBConstants.ALLOCITEMS_LIST_COL_CDFSITEMSIZE;
    private static final String ALLOCITEMS_LIST_COL_FOLDER = DBConstants.ALLOCITEMS_LIST_COL_FOLDER;

    SQLiteDatabase mdb = null;

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DBHelper constructed");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DBHelper onCreated");
        String sql_statement1 = "CREATE TABLE " + USERPROFILE_TABLE_NAME
                + " ("
                + "INTEGER AUTO_INCREMENT primary key, "
                + USERPROFILE_TABLE_COL_BRAND + " text, "
                + USERPROFILE_TABLE_COL_NAME + " text, "
                + USERPROFILE_TABLE_COL_MAIL + " text, "
                + USERPROFILE_TABLE_COL_PHOTOURL + " url, "
                + USERPROFILE_TABLE_COL_STATE + " text"
                + "); ";
        String sql_statement2 = "CREATE TABLE " + TABLE_ALLOCITEM_LIST
                + " ("
                + "INTEGER AUTO_INCREMENT primary key, "
                + ALLOCITEMS_LIST_COL_NAME + " text, "
                + ALLOCITEMS_LIST_COL_PATH + " text, "
                + ALLOCITEMS_LIST_COL_DRIVENAME + " text, "
                + ALLOCITEMS_LIST_COL_SEQUENCE + " integer, "
                + ALLOCITEMS_LIST_COL_TOTALSEG + " integer, "
                + ALLOCITEMS_LIST_COL_SIZE + " integer, "
                + ALLOCITEMS_LIST_COL_CDFSITEMSIZE + " integer, "
                + ALLOCITEMS_LIST_COL_FOLDER + " BIT"
                + ");";
        Log.d(TAG, sql_statement1);
        Log.d(TAG, sql_statement2);
        db.execSQL(sql_statement1);
        db.execSQL(sql_statement2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public SQLiteDatabase getDB(){
        return getWritableDatabase();
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
            if(photourl != null) {
                cv.put(USERPROFILE_TABLE_COL_PHOTOURL, photourl.toString());
            }
            cv.put(USERPROFILE_TABLE_COL_STATE, state);
            r_id = mdb.insert(USERPROFILE_TABLE_NAME, null,cv);
            mdb.close();
        }

        return r_id;
    }

    /*
    * If no argument is giving, all columns are read out from database.
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
            //Do not close database after query.
        }

        return cursor;
    }

    /*
    Input:  values. the values to be written to the columns.
            where.  Will be used for the SQlite whereClause.
     */
    public int update(ContentValues values, ContentValues where){
        int rows_affected = 0;

        Log.d(TAG, "update row");

        try{
            mdb = getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "DB update: db open failed!" + e.getMessage());
        }

        if(mdb != null){
            rows_affected = mdb.update(USERPROFILE_TABLE_NAME,
                    values,
                    make_statement(where),
                    null);
            if(rows_affected == 0){
                Log.w(TAG, "db update failed");
            }
            mdb.close();
        }

        return rows_affected;
    }

    private String make_statement(ContentValues cv){
        String statement="";
        String clause;
        Log.d(TAG, "make statement:");

        clause = (String)cv.get(USERPROFILE_TABLE_COL_BRAND);
        if( clause != null){
            Log.d(TAG, "USERPROFILE_TABLE_COL_BRAND:" + clause);
            statement = statement.concat(USERPROFILE_TABLE_COL_BRAND + " = " + "\"" + clause + "\"");
        }
        clause = (String)cv.get(USERPROFILE_TABLE_COL_NAME);
        if( clause != null){
            Log.d(TAG, "USERPROFILE_TABLE_COL_NAME:" + clause);
            statement = statement.concat(" AND " + USERPROFILE_TABLE_COL_NAME + " = " + "\"" + clause + "\"");
        }
        clause = (String)cv.get(USERPROFILE_TABLE_COL_MAIL);
        if( clause != null){
            Log.d(TAG, "USERPROFILE_TABLE_COL_MAIL:" + clause);
            statement = statement.concat(" AND " + USERPROFILE_TABLE_COL_MAIL + " = " + "\"" + clause + "\"");
        }
        clause = (String)cv.get(USERPROFILE_TABLE_COL_PHOTOURL);
        if( clause != null){
            statement = statement.concat(" AND " + USERPROFILE_TABLE_COL_PHOTOURL + " = " + "\"" + clause + "\"");
        }
        clause = (String)cv.get(USERPROFILE_TABLE_COL_STATE);
        if( clause != null){
            statement = statement.concat(" AND " + USERPROFILE_TABLE_COL_STATE + " = " + "\"" + clause + "\"");
        }

        Log.d(TAG, "statement: " + statement);
        return statement;
    }
    public int delete(String ... expression){
        int number_row = 0;
        ContentValues cv = new ContentValues();

        Log.d(TAG, "delete");
        /*
        So far, three conditions and AND operator are supported
         */
        if(expression.length > 6)
        {
            Log.w(TAG, "Too many conditions are required!");
            return number_row;
        }

        try{
            mdb = getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(mdb != null){
            number_row = mdb.delete(USERPROFILE_TABLE_NAME,
                    expression[0] + "=" + expression[1] + "AND" +
                            expression[2] + "=" + expression[3] + "AND" +
                            expression[4] + "=" + expression[5],
                            null);
            if(number_row == 0){
                Log.w(TAG, "db delete failed");
            }
            mdb.close();
        }

        return number_row;
    }
}
