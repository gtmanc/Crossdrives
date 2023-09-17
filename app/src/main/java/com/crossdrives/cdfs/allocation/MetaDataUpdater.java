package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.model.UpdateFile;
import com.crossdrives.cdfs.remote.updater;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MetaDataUpdater
{
    HashMap<String, Drive> mDrives;
//    class MetaData{
//        UpdateFile updateFile;
//
//        public MetaData() {}
//
//        public MetaData(MetaData metaData) {
//            updateFile = metaData.updateFile;
//        }
//        public MetaData clone(){return new MetaData(this);}
//    }

    public MetaDataUpdater(HashMap<String, Drive> drives) {
        mDrives = drives;
    }

    HashMap<String, com.google.api.services.drive.model.File> metaData = new HashMap<>();

    public MetaDataUpdater parent(ConcurrentHashMap<String, List<String>> parent){
        parent.entrySet().stream().forEach(set->{
            String driveName = set.getKey();
            //Always take first element as a folder only contains only one drive item.
            String id = set.getValue().get(0);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            //metaData.get(driveName).updateFile.getMetadata().setParents(parentList);
            metaData.get(driveName).setParents(parentList);
        });
        return this;
    }

    public CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> run(String id){
        //Not yet implemented
        return null;
    }

    public CompletableFuture<HashMap<String, List<com.google.api.services.drive.model.File>>> run(ConcurrentHashMap<String, List<String>> idList){
        HashMap<String, CompletableFuture<List<com.google.api.services.drive.model.File>>> futures = new HashMap<>();
        idList.entrySet().stream().forEach((set)->{
            CompletableFuture<List<com.google.api.services.drive.model.File>> future;
            String driveName = set.getKey();
            List<String> list = set.getValue();
            updater updater = new updater(mDrives);
            //future = updater.updateAll(driveName, list, metaData.get(driveName).updateFile.getMetadata(), null);
            future = updater.updateAll(driveName, list, metaData.get(driveName), null);
            futures.put(driveName, future);
        });

        return CompletableFuture.supplyAsync(()->{
            HashMap<String, List<com.google.api.services.drive.model.File>> result = new HashMap<>();
            futures.entrySet().stream().forEach((set)->{
                result.put(set.getKey(), set.getValue().join());
            });
            return result;
        });
    }

}