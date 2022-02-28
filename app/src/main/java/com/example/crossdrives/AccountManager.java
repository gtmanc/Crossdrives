package com.example.crossdrives;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.crossdrives.data.DBConstants;
import com.crossdrives.data.DBHelper;
import com.crossdrives.msgraph.MSGraphRestHelper;

import java.io.InputStream;

public class AccountManager {
    private static String TAG = "CD.AccountManager";
    static AccountManager mAM;
    public final static int MAX_BRAND_SUPPORT = 2;
//    public final static String BRAND_GOOGLE = "GDrive";
//    public final static String BRAND_MS = "OneDrive";

    private final String STATE_ACTIVATED = "Activated";
    private final String STATE_DEACTIVATED = "Deactivated";



    static class AccountInfo{
        String brand;
        String name;
        String mail;
        Uri photouri;
        Bitmap mBmp;
        Callback mCallback;

        public void getPhoto(Callback callback){
            mCallback = callback;
            //Google: the user photo can be downloaded using url
//            if(brand.equals(BRAND_GOOGLE)){
//                new DownloadPhoto().execute(photouri.toString());
//            }
//            else if (brand.equals(BRAND_MS)){
//                MSGraphRestHelper ms = new MSGraphRestHelper();
//                ms.getMePhoto(callbackGraph);
//            }
        }
        //Graph helper callback for downloading ms user photo
        MSGraphRestHelper.Callback callbackGraph = new MSGraphRestHelper.Callback(){

            @Override
            public void onPhotoDownloaded(Bitmap bmp) {
                //Call back to UI fragment
                mCallback.onPhotoDownloaded(bmp);
            }
        };

        //Callback for downloading google user photo
        private class DownloadPhoto extends AsyncTask<String, Void, Bitmap> {
            public DownloadPhoto() {
            }

            @Override
            protected Bitmap doInBackground(String... urls) {
                Bitmap bm = null;
                try{
                    InputStream in = new java.net.URL(urls[0]).openStream();
                    bm = BitmapFactory.decodeStream(in);
                }catch(Exception e){
                    Log.w(TAG, "Open URL failed");
                }

                return bm;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                mBmp = bitmap;
                //Call back to UI fragment
                mCallback.onPhotoDownloaded(bitmap);
            }
        }

        public interface Callback{
            void onPhotoDownloaded(Bitmap bmp);
        }
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
        int iname = DBConstants.TABLE_ACCOUNT_COL_INDX_NAME;
        int imail = DBConstants.TABLE_ACCOUNT_COL_INDX_MAIL;
        //int iphoto = DBConstants.COL_INDX_PHOTOURL;

        /*
        Read all rows that the state is activated. Normally, there must be only one activated account for each brand.
         */
        c = dbh.query(Col_Brand, "\""+brand +"\"", Col_State, "\""+STATE_ACTIVATED+"\"");
        if (c.getCount() > 1) {
            Log.w(TAG, "more than one activated account found! Brand: " + brand);
        }

        if(c != null && c.getCount() > 0) {
            info = new AccountInfo();
            c.moveToFirst();
            //Uri uri = toUri(c.getString(iphoto));
            info.brand = brand;
            info.name = c.getString(iname);
            info.mail = c.getString(imail);
            //info.photouri = uri;
        }

        return info;
    }

    /*
        No matter what state the queried row is, set the column state to deactivated.
    */
    public boolean setAccountDeactivated(Context context, String brand, String name, String mail){
        int row_deleted = 0;
        boolean err = false;
        ContentValues values = new ContentValues();
        ContentValues where = new ContentValues();

        Log.w(TAG, "Set account deactivated: " + brand);
        if(brand != null){ where.put(DBConstants.USERPROFILE_TABLE_COL_BRAND, brand); }
        if(name != null){  where.put(DBConstants.USERPROFILE_TABLE_COL_NAME, name); }
        if(mail != null){ where.put(DBConstants.USERPROFILE_TABLE_COL_MAIL, mail);}

        values.put(DBConstants.USERPROFILE_TABLE_COL_STATE, STATE_DEACTIVATED);

        DBHelper dbh = new DBHelper(context, null,null,0);
        row_deleted = dbh.update(values,where);
        if(row_deleted != 0){
            err = true;
        }
        else{
            Log.w(TAG, "Delete account failed");
        }

        return err;
    }
    /*
        Not yet test. Currently there is no scenario which needs this function
    */
    public boolean deleteAccount(Context context, String brand, String name, String mail){
        int row_deleted = 0;
        boolean err = false;
        String Col_Brand = DBConstants.USERPROFILE_TABLE_COL_BRAND;
        String Col_Name = DBConstants.USERPROFILE_TABLE_COL_NAME;
        String Col_Mail = DBConstants.USERPROFILE_TABLE_COL_MAIL;

        DBHelper dbh = new DBHelper(context, null,null,0);
        row_deleted = dbh.delete(Col_Brand, "\""+brand+"\"",
                Col_Name, "\""+name+"\"",
                Col_Mail, "\""+mail+"\"");
        if(row_deleted != 0){
            err = true;
        }
        else{
            Log.w(TAG, "Delete account failed");
        }

        return err;
    }

    private Uri toUri(String link){
        Uri uri = null;
        uri = Uri.parse(link);
        return uri;
    }
}

