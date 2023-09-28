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
import java.util.function.Function;

public class MetaDataUpdater
{
    HashMap<String, Drive> mDrives;

    public MetaDataUpdater(HashMap<String, Drive> drives) {
        mDrives = drives;
    }

    private class MetaData{
        HashMap<String, com.google.api.services.drive.model.File> toUpdated = new HashMap<>();
        HashMap<String, com.google.api.services.drive.model.File> original = new HashMap<>();
    }

    MetaData metaData = new MetaData();

    /*
        set parents.
        @param: parentSrc   parents will be removed from the specified item to be moved.
        @param: parentDest  parents will be added to the specified item to be moved.
        @return: the updated MetaDataUpdater object
     */
    public MetaDataUpdater parent(ConcurrentHashMap<String, List<String>> parentSrc, ConcurrentHashMap<String, List<String>> parentDest){
        parentSrc.entrySet().stream().forEach(set->{
            String driveName = set.getKey();
            //Always take first element as a folder only contains only one drive item.
            String id = set.getValue().get(0);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            if(metaData.toUpdated.get(driveName) == null){
                File file = new File();
                file.setParents(parentList);
                metaData.toUpdated.put(driveName, file);
            }else{
                metaData.get(driveName).setParents(parentList);
            }

        });
        return this;
    }

    private <T, R> void setParent(ConcurrentHashMap<String, List<String>> parentSrc,
                             com.google.api.services.drive.model.File metaData,
                                  Function<com.google.api.services.drive.model.File, com.google.api.services.drive.model.File> function ){
        parentSrc.entrySet().stream().forEach(set->{
            String driveName = set.getKey();
            //Always take first element as a folder only contains only one drive item.
            String id = set.getValue().get(0);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            if(metaData.get(driveName) == null){
                File file = new File();
                file = function.apply(file);   //file.setParents(parentList);
                metaData.put(driveName, file);
            }else{
                metaData.get(driveName).setParents(parentList);
            }

        });
    }

    <T, R> copy()


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