package com.crossdrives.cdfs;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class BaseCDFS {
    final private String TAG = "CD.BaseCDFS";
    private Context mContext;


    ConcurrentHashMap<String, Drive> mDrives = new ConcurrentHashMap<>();

    public BaseCDFS(Context context) {
        Log.d(TAG, "Set context: " + context);
        this.mContext = context;
    }

//    public void setContext(Context context) {
//        Log.d(TAG, "Set context!");
//        mContext = context;
//    }

    public String getPath(){
        return mContext.getFilesDir().getPath();
    }

    public Context getContext(){return mContext;}
}
