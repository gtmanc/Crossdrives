package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.cdfs.remote.Locker;
import com.google.api.services.drive.model.ContentRestriction;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MapLocker {

    ConcurrentHashMap<String, Drive> mDrives;
    HashMap<String, CompletableFuture<FileList>> fileListFutures = new HashMap<>();

    public MapLocker(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, File>> lockAll(HashMap<String, File> files){
//        CompletableFuture<HashMap, File> resultFuture = CompletableFuture.supplyAsync(()->{

       Locker locker = new Locker(this.mDrives);
       return locker.lockAll(getID(files));
//        });
    }


    public CompletableFuture<HashMap<String, File>> unlockAll(HashMap<String, File> files){
        Locker locker = new Locker(this.mDrives);
        return locker.unlockAll(getID(files));
    };

    public CompletableFuture<HashMap<String, ContentRestriction>> getStatus(HashMap<String, File> files){
        Locker locker = new Locker(this.mDrives);
        return locker.getStatusAll(getID(files));
    }


    HashMap<String, String> getID(HashMap<String, File> files) {
        Map<String, String> IDs = files.entrySet().stream().map((set) -> {
            Map.Entry<String, String> entry = new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return set.getKey();
                }

                @Override
                public String getValue() {
                    return set.getValue().getId();
                }

                @Override
                public String setValue(String s) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        return new HashMap<>(IDs);
    }
}
