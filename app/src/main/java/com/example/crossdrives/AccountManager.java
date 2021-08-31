package com.example.crossdrives;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class AccountManager {
    private static String TAG = "CD.AccountManager";
    static AccountManager mAM;
    public final static int MAX_BRAND_SUPPORT = 2;
    public final static String BRAND_GOOGLE = "GDrive";
    public final static String BRAND_MS = "OneDrive";

    private final String STATE_ACTIVATED = "Activated";
    private final String STATE_DEACTIVATED = "Deactivated";

    static class AccountInfo{
        String brand;
        String name;
        String mail;
        Uri photouri;
    };

    public AccountManager() {
        Log.d(TAG, "Constructed");
    }

    public static AccountManager getInstance() {
        if(mAM == null){
            Log.d(TAG, "New");
            mAM = new AccountManager();
        }

        return mAM;
    }

    public boolean createAccount(Context context, AccountInfo info){
        long id;
        boolean err = false;
        Log.d(TAG, "createAccount");

        DBHelper dbh = new DBHelper(context, null, null, 0);
        id = dbh.insert(info.brand, info.name, info.mail, info.photouri, "Activated");
        if (id == -1) {
            Log.w(TAG, "create db record failed!");
        }
        else{
            err = true;
        }

        return err;
    }

    public AccountInfo getAccountActivated(Context context, String brand)
    {
        Cursor c = null;
        DBHelper dbh = new DBHelper(context, null,null,0);
        AccountInfo info = null;
        String Col_State = DBConstants.USERPROFILE_TABLE_COL_STATE;
        String Col_Brand = DBConstants.USERPROFILE_TABLE_COL_BRAND;
        int iname = DBConstants.COL_INDX_NAME;
        int imail = DBConstants.COL_INDX_MAIL;
        int iphoto = DBConstants.COL_INDX_PHOTOURL;

        /*
        Read all rows that the state is activated. Normally, there must be only one activated account for each brand.
         */
        if(brand == BRAND_GOOGLE) {
            c = dbh.query(Col_Brand, "\""+brand +"\"", Col_State, "\""+STATE_ACTIVATED+"\"");
            if (c.getCount() > 1) {
                Log.w(TAG, "*** more than one activated google drive account ***");
            }
        }
        else if(brand == BRAND_MS){
            c = dbh.query(Col_Brand, "\""+BRAND_MS+"\"", Col_State, "\""+STATE_ACTIVATED+"\"");
            if(c.getCount() > 1){
                Log.w(TAG, "*** more than one activated onedrive account ***");
            }
        }

        if(c != null && c.getCount() > 0) {
            info = new AccountInfo();
            c.moveToFirst();
            Uri uri = toUri(c.getString(iphoto));
            info.name = c.getString(iname);
            info.mail = c.getString(imail);
            info.photouri = uri;
        }

        return info;
    }


    private Uri toUri(String link){
        Uri uri = null;
        uri = Uri.parse(link);
        return uri;
    }
}

