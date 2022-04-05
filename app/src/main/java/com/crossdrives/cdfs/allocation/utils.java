package com.crossdrives.cdfs.allocation;

import android.database.Cursor;

import com.crossdrives.data.DBConstants;

import java.util.ArrayList;
import java.util.HashMap;

public class utils {

    /*
        build up a name list according to the cdfs id.
        cursor: a cursor which must at least cdfs id.
     */
    public java.util.List<String> buildNameList(Cursor cursor){
        java.util.List<String> IDs = new ArrayList<>();
        java.util.List<String> names = new ArrayList<>();
        final int INDEX_ID = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_CDFSID);
        final int INDEX_NAME = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_NAME);

        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++) {
            String col_id = cursor.getString(INDEX_ID);
            if(IDs.stream().noneMatch((id)->{
                return id.equals(col_id);
            }) == true){
                IDs.add(col_id);
                names.add(cursor.getString(INDEX_NAME));
            }
            cursor.moveToNext();
        }

        return names;
    }

    /*
        build up a CDFS ID list.
        cursor: a cursor which must at least contain cdfs id.
     */
    public java.util.List<String> buildCdfsIdList(Cursor cursor){
        java.util.List<String> IDs = new ArrayList<>();
        final int INDEX_ID = cursor.getColumnIndex(DBConstants.ALLOCITEMS_LIST_COL_CDFSID);

        cursor.moveToFirst();
        for(int i = 0 ; i < cursor.getCount(); i++) {
            String col_id = cursor.getString(INDEX_ID);
            if(IDs.stream().noneMatch((id)->{
                return id.equals(col_id);
            }) == true){
                IDs.add(cursor.getString(INDEX_ID));
            }
            cursor.moveToNext();
        }

        return IDs;
    }
}
