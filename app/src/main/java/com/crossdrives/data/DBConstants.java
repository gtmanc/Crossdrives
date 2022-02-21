package com.crossdrives.data;

public interface DBConstants {
    final String DATABASE_NAME = "com.crossdrives.database";
    final int DATABASE_VERSION = 1;

    /*
    *   Tables
    * */
    final String TABLE_MASTER_USER_PROFILE = "master_profile";
    final String TABLE_CDFSITEM_LIST = "cdfs_itemlist";

    /*
        Columns
     */
    final String USERPROFILE_TABLE_COL_BRAND = "brand";
    final String USERPROFILE_TABLE_COL_NAME = "user_name";
    final String USERPROFILE_TABLE_COL_MAIL = "user_mail";
    final String USERPROFILE_TABLE_COL_PHOTOURL = "user_photourl";
    final String USERPROFILE_TABLE_COL_STATE = "user_state";

    final String CDFSITEMS_LIST_COL_NAME = "name";
    final String CDFSITEMS_LIST_COL_PATH = "name";

    /*
        Index to column
     */
    //Table master account user profile Column Index
    final int COL_INDX_RECORD_ID = 0;
    final int COL_INDX_BRAND = 1;
    final int COL_INDX_NAME = 2;
    final int COL_INDX_MAIL = 3;
    final int COL_INDX_PHOTOURL = 4;
    final int COL_INDX_STATE = 5;
}
