package com.crossdrives.cdfs;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

public class BaseCDFS {
    final private String TAG = "CD.BaseCDFS";
    Context mContext;


    ConcurrentHashMap<String, Drive> mDrives = new ConcurrentHashMap<>();

    public void setContext(Context context) {
        Log.d(TAG, "Set context!");
        mContext = context;
    }

    public String getPath(){
        return mContext.getFilesDir().getPath();
    }
}
