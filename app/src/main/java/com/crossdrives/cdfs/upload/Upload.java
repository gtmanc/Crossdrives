package com.crossdrives.cdfs.upload;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.Allocator;
import com.crossdrives.cdfs.remote.DriveQuota;
import com.crossdrives.cdfs.data.Drive;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.About;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Upload {
    final String TAG = "CD.Upload";
    CDFS mCDFS;

    public Upload(CDFS cdfs) {
        mCDFS = cdfs;
    }

    public void upload(File file){
        ConcurrentHashMap<String, Drive> drives= mCDFS.getDrives();

        DriveQuota dq = new DriveQuota(drives);
        dq.fetchAllOf().addOnSuccessListener(new OnSuccessListener<HashMap<String, About.StorageQuota>>() {
            @Override
            public void onSuccess(HashMap<String, About.StorageQuota> quotaMap) {
                quotaMap.forEach((k, quota)->{
                    HashMap<String, Long> allocation;
                    Allocator allocator = new Allocator(quotaMap, file.length());
                    Log.d(TAG, "Drive Name: " + k + "Limit: " + quota.getLimit() + " usage: " + quota.getUsage());

                    allocation = allocator.getAllocationResult();



                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

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
