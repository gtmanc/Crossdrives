package com.example.crossdrives;

public interface DBConstants {
    final String TABLE_MASTER_USER_PROFILE = "master_profile";
    final String DATABASE_NAME = "com.crossdrives.database";
    final int DATABASE_VERSION = 1;
    final String USERPROFILE_TABLE_COL_BRAND = "brand";
    final String USERPROFILE_TABLE_COL_NAME = "user_name";
    final String USERPROFILE_TABLE_COL_MAIL = "user_mail";
    final String USERPROFILE_TABLE_COL_PHOTOURL = "user_photourl";
    final String USERPROFILE_TABLE_COL_STATE = "user_state";


    //Table master account user profile Column Index
    final int COL_INDX_RECORD_ID = 0;
    final int COL_INDX_BRAND = 1;
    final int COL_INDX_NAME = 2;
    final int COL_INDX_MAIL = 3;
    final int COL_INDX_PHOTOURL = 4;
    final int COL_INDX_STATE = 5;

    //Table master account user profile states
    final int PROFILE_STATE_DEACIVATED = 0;
    final int PROFILE_STATE_ACIVATED = 1;
}
