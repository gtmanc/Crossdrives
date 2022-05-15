package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.allocation.ISplitProgressCallback;
import com.crossdrives.cdfs.allocation.Splitter;
import com.crossdrives.cdfs.remote.DriveQuota;
import com.crossdrives.cdfs.data.Drive;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.About;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Upload {
    final String TAG = "CD.Upload";
    CDFS mCDFS;
    IUploadCallbck mCallback = null;
    InputStream inputStream;

    public Upload(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public Upload(CDFS cdfs, InputStream ins) {
        mCDFS = cdfs;
        inputStream = ins;
    }

    public void upload(File file, IUploadCallbck callback){
        mCallback = callback;
        ConcurrentHashMap<String, Drive> drives= mCDFS.getDrives();

        DriveQuota dq = new DriveQuota(drives);
        dq.fetchAllOf().addOnSuccessListener(new OnSuccessListener<HashMap<String, About.StorageQuota>>() {
            @Override
            public void onSuccess(HashMap<String, About.StorageQuota> quotaMap) {
                quotaMap.forEach((k, quota)->{
                    HashMap<String, Long> allocation;
                    long size = 0;
                    try {
                        size = (long)inputStream.available();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Size to upload: " + size);
                    //Allocator allocator = new Allocator(quotaMap, file.length());
                    Allocator allocator = new Allocator(quotaMap, size);
                    Log.d(TAG, "Drive Name: " + k + "Limit: " + quota.getLimit() + " usage: " + quota.getUsage());

                    allocation = allocator.getAllocationResult();
                    Splitter splitter = new Splitter(inputStream, allocation);
                    splitter.split(new ISplitProgressCallback() {
                        @Override
                        public void start(String name, long total) {
                            Log.d(TAG, "Split start. drive name: " + name +
                                    " allocated length: " + total);
                        }

                        @Override
                        public void progress(File slice) {
                            //upload slice to remote
                            Log.d(TAG, "Split progress: " + slice.getPath());
                        }

                        @Override
                        public void finish(String name, long remaining) {
                            Log.d(TAG, "split finished. drive name: " + name +
                                    " Remaining data: " + remaining);
                        }

                        @Override
                        public void onFailure(String ex) {
                            Log.d(TAG, "Split file failed! " +ex);
                            mCallback.onFailure(new Throwable(ex));
                        }
                    });
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mCallback.onFailure(new Throwable(e.getMessage()));
            }
        });

//        drives.forEach((driveName, drive)->{
//            Log.d(TAG, "" + driveName);
//            drive.getClient().about().buildRequest().run(new IAboutCallBack() {
//                @Override
//                public void success(About about) {
//
//                    About.StorageQuota quota = about.getStorageQuota();
//                    Log.d(TAG, "Limit: " + quota.getLimit() + " usage: " + quota.getUsage());
//                }
//
//                @Override
//                public void failure(String ex) {
//                    Log.w(TAG, ex);
//                }
//            });
//        });
    }
}
