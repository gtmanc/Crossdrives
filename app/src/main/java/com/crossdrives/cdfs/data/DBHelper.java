package com.crossdrives.cdfs.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.crossdrives.data.DBConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DBHelper{
    private final String TAG = "CD.DBHelper";
    com.crossdrives.data.DBHelper dbh;
    ContentValues mCV = new ContentValues();
    private String mClauseGroup;
    private List<String> mClauseSelect;

    final String  TABLE_ALLOCITEM_LIST = DBConstants.TABLE_ALLOCITEM_LIST;
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

    public DBHelper(Context context) {
        dbh = new com.crossdrives.data.DBHelper(context, null, null, 0);
    }

    public DBHelper setName(String name){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_NAME, name);
        return this;
    }

    public DBHelper setPath(String path){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_PATH, path);
        return this;
    }

    public DBHelper setDrive(String drive){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_DRIVENAME, drive);
        return this;
    }

    public DBHelper setCdfsID(String id){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_CDFSID, id);
        return this;
    }

    public DBHelper setItemID(String id){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_ITEMID, id);
        return this;
    }

    public DBHelper setSequence(int seq){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_SEQUENCE, seq);
        return this;
    }

    public DBHelper setTotalSegment(int total){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_TOTALSEG, total);
        return this;
    }

    public DBHelper setSize(long size){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_SIZE, size);
        return this;
    }

    public DBHelper setCDFSItemSize(long size){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_CDFSITEMSIZE, size);
        return this;
    }

    public DBHelper setAttrFolder(boolean folder){
        ContentValues cv;
        mCV.put(ALLOCITEMS_LIST_COL_FOLDER, folder);
        return this;
    }

    /*
        Sql clauses
    */
    public DBHelper GroupBy(String clause){
        mClauseGroup = clause;
        return this;
    }

    public DBHelper Select(List<String> columns){
        mClauseSelect = columns;
        return this;
    }

    public long insert(){
        SQLiteDatabase db = null;
        long r_id = -1;


        Log.d(TAG, "DB operation insert");
        try{
;            db = dbh.getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed: " + e.getMessage());
        }

        if(db != null){
            r_id = db.insert(TABLE_ALLOCITEM_LIST, null, mCV);
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
            
            //TODO: db.insert(TABLE_ALLOCITEM_LIST, null, cvs.get(0));

            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        }
    }

    /*
        expression: clause for the rows to delete. Note "\" must be added in front of the value if
        the value is in type of string. To delete whole table, set expression to null.
     */
    public int delete(String ... expression){
        SQLiteDatabase db = null;
        int number_row = 0;
        ContentValues cv = new ContentValues();
        String clause = null;

        Log.d(TAG, "DB operation Delete");
        /*
        So far, only one conditions and AND operator are supported
         */
        if(expression != null && expression.length > 2)
        {
            Log.w(TAG, "Too many conditions are required!");
            return number_row;
        }
        /*
            Delete whole table or select
        */
        if(expression.length != 0){
            Log.d(TAG, "expression.length: " +expression.length);
            Log.d(TAG, "Delete specified rows...");
            clause = expression[0] + "=" + expression[1];
        }else{
            Log.d(TAG, "Delete whole table...");
            //db.execSQL("DROP TABLE IF EXISTS "+CONTACTS_TABLE_NAME);
        }

        try{
            db = dbh.getWritableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(db != null){
            Log.d(TAG, "Clause: " + clause);
            number_row = db.delete(TABLE_ALLOCITEM_LIST,
                    clause
//                            + "AND" +
//                            expression[2] + "=" + expression[3] + "AND" +
//                            expression[4] + "=" + expression[5]
                    ,
                    null);
            db.close();
        }

        return number_row;
    }

    /*
     * expression: filter clause for the rows to be read. Note "\" must be added in front of the value if
        the value is in type of string. e.g. parent = "Root".
     * If no expression is giving, all rows are read out from database.
     * The input expression is a string which contains a pair of column name and value. Max is 2 strings.
     * Conditional operator between the two query strings is set to "AND".
     * */
    public Cursor query(String ... expression){
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String statement;
        int i;

        Log.d(TAG, "DB operation Query");
        /*
        So far, two conditions and AND operator are supported
         */
        if(expression.length > 2)
        {
            Log.w(TAG, "Too many conditions are required!");
            return null;
        }

        try {
            db = dbh.getReadableDatabase();
        }
        catch (SQLiteException e)
        {
            Log.w(TAG, "db open failed" + e.getMessage());
        }

        if(expression.length == 0) {
            Log.d(TAG, "Query all");
            statement = "SELECT * FROM " + TABLE_ALLOCITEM_LIST + ";";
        }else{
//            statement = "SELECT * "
//                    + " FROM " + TABLE_ALLOCITEM_LIST
//                    + " WHERE " + expression[0] + " = " + expression[1] + " AND "
//                    + expression[2] + " = " + expression[3]
//                    + ";";
            statement = "SELECT";
            /*
                Select
             */
            if(mClauseSelect == null){
                statement = statement.concat(" * ");
            }else{
                for(i = 0; i < mClauseSelect.size() - 1 ; i++) {
                    statement = statement.concat(" " + mClauseSelect.get(i) + ",");
                }
                statement = statement.concat(" " + mClauseSelect.get(i));
            }


            statement = statement.concat(" FROM " + TABLE_ALLOCITEM_LIST);

            /*
                filter
             */
            statement = statement.concat(" WHERE ");
            for(i = 0; i < expression.length-1 ; i++){
                statement = statement.concat(expression[i] + "AND ");
            }
            statement = statement.concat(expression[i]);
            /*
                Grouping
             */
            if(mClauseGroup != null){
                statement = statement.concat(" GROUP BY " + mClauseGroup);
            }

            statement = statement.concat(";");
            Log.d(TAG, "clause: " + statement);
        }

        if(db != null)    {
            cursor = db.rawQuery(statement, null);
            //Do not close database after query.
        }

        return cursor;
    }
}
