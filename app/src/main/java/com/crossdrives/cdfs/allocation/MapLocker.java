package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.cdfs.remote.Locker;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapLocker {

    ConcurrentHashMap<String, Drive> mDrives;
    HashMap<String, CompletableFuture<FileList>> fileListFutures = new HashMap<>();

    public MapLocker(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    /*
        So far, we simply call locker without any additional business logic.
     */
    public CompletableFuture<HashMap<String, File>> lockAll(HashMap<String, File> files){
//        CompletableFuture<HashMap, File> resultFuture = CompletableFuture.supplyAsync(()->{
            Locker locker = new Locker(this.mDrives);
            return locker.lockAll(files);
//        });
    }
}
