package com.example.crossdrives;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;

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
        Bitmap mBmp;
        Callback mCallback;

        public void getPhoto(Callback callback){
            mCallback = callback;
            //Google: the user photo can be downloaded using url
            if(brand.equals(BRAND_GOOGLE)){
                new DownloadPhoto().execute(photouri.toString());
            }
        }

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

