package com.crossdrives.cdfs;

import android.content.Context;
import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.exception.MissingDriveClientException;

import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.util.concurrent.ConcurrentHashMap;

public class BaseCDFS {
    final private String TAG = "CD.BaseCDFS";

    ConcurrentHashMap<String, Drive> mDrives = new ConcurrentHashMap<>();

    public BaseCDFS() {
    }

//    public void setContext(Context context) {
//        Log.d(TAG, "Set context!");
//        mContext = context;
//    }

//    public String getPath(){
//        return mContext.getFilesDir().getPath();
//    }

    //public Context getContext(){return mContext;}

    public void requiresDriveClientNonNull() throws MissingDriveClientException {
        if(mDrives.isEmpty()) {throw new MissingDriveClientException("No available drive client", new Throwable(""));
        }
    }
}
