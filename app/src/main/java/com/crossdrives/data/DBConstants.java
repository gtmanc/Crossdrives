package com.crossdrives.data;

public interface DBConstants {
    final String DATABASE_NAME = "com.crossdrives.database";
    final int DATABASE_VERSION = 1;

    /*
    *   Tables
    * */
    final String TABLE_MASTER_USER_PROFILE = "master_profile";
    final String TABLE_ALLOCITEM_LIST = "allocitem_list";

    /*
        Columns
     */
    //Account manager - user profile
    final String USERPROFILE_TABLE_COL_BRAND = "brand";
    final String USERPROFILE_TABLE_COL_NAME = "user_name";
    final String USERPROFILE_TABLE_COL_MAIL = "user_mail";
    final String USERPROFILE_TABLE_COL_PHOTOURL = "user_photourl";
    final String USERPROFILE_TABLE_COL_STATE = "user_state";

    //Allocation item list
    final String ALLOCITEMS_LIST_COL_NAME = "name";
    final String ALLOCITEMS_LIST_COL_PATH = "path";
    final String ALLOCITEMS_LIST_COL_DRIVENAME = "drive_name";
    final String ALLOCITEMS_LIST_COL_SEQUENCE = "sequence";
    final String ALLOCITEMS_LIST_COL_TOTALSEG = "total_segment";
    final String ALLOCITEMS_LIST_COL_SIZE = "item_size";
    final String ALLOCITEMS_LIST_COL_CDFSITEMSIZE = "cdfsitem_size";
    final String ALLOCITEMS_LIST_COL_FOLDER = "folder";

    /*
        Index
     */
    //Table master account user profile Column Index
    final int TABLE_ACCOUNT_COL_INDX_ID = 0;
    final int TABLE_ACCOUNT_COL_INDX_BRAND = 1;
    final int TABLE_ACCOUNT_COL_INDX_NAME = 2;
    final int TABLE_ACCOUNT_COL_INDX_MAIL = 3;
    final int TABLE_ACCOUNT_COL_INDX_PHOTOURL = 4;
    final int TABLE_ACCOUNT_COL_INDX_STATE = 5;

    //Table allocation item column Index
    final int TABLE_ALLOCITEM_COL_INDX_ID = 0;
    final int TABLE_ALLOCITEM_COL_INDX_NAME = 1;
    final int TABLE_ALLOCITEM_COL_INDX_PATH = 2;
}
