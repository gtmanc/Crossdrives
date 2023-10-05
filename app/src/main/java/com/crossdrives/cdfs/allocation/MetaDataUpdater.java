package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.remote.updater;
import com.crossdrives.driveclient.update.MetaData;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class MetaDataUpdater
{
    final String TAG = "CD.MetaDataUpdater";
    HashMap<String, Drive> mDrives;

    public MetaDataUpdater(HashMap<String, Drive> drives) {
        mDrives = drives;
    }

    HashMap<String, MetaData> metaData = new HashMap<>();

    /*
        set parents.
        @param: parentSrc   parents will be removed from the specified item to be moved.
        @param: parentDest  parents will be added to the specified item to be moved.
        @return: the updated MetaDataUpdater object
     */
//    public MetaDataUpdater parent(ConcurrentHashMap<String, List<String>> parentSrc, ConcurrentHashMap<String, List<String>> parentDest){
//        parentSrc.entrySet().stream().forEach(set->{
//            String driveName = set.getKey();
//            //Always take first element as a folder only contains only one drive item.
//            String id = set.getValue().get(0);
//            List<String> parentList = new ArrayList<>();
//            parentList.add(id);
//            if(metaData.toUpdated.get(driveName) == null){
//                File file = new File();
//                file.setParents(parentList);
//                metaData.toUpdated.put(driveName, file);
//            }else{
//                metaData.toUpdated.get(driveName).setParents(parentList);
//            }
//
//        });
//        return this;
//    }

    public MetaDataUpdater parent(ConcurrentHashMap<String, List<String>> oldParents, ConcurrentHashMap<String, List<String>> newParents){
        forEachValue(newParents, metaData, (list, metadata)->{
            String id = list.get(0);
            Log.d(TAG, "new parent: " + id);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            metadata.setParentsToAdded(parentList);
            return metadata;
        });

        forEachValue(oldParents, metaData, (list, metadata)->{
            String id = list.get(0);
            Log.d(TAG, "old parent: " + id);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            metadata.setParentsToRemoved(parentList);
            return metadata;
        });
        return this;
    }

    /*

    */
    private <T> void forEachValue(ConcurrentHashMap<String, T> from,
                                  HashMap<String, com.crossdrives.driveclient.update.MetaData> to,
                                  BiFunction<T, com.crossdrives.driveclient.update.MetaData, com.crossdrives.driveclient.update.MetaData> function)
    {
        from.entrySet().stream().forEach(set->{
            String driveName = set.getKey();
            //Always take first element as a folder only contains only one drive item.
            T v = set.getValue();
            //List<String> parentList = new ArrayList<>();
            //parentList.add(id);
            if(to.get(driveName) == null){
                com.crossdrives.driveclient.update.MetaData metaData = new com.crossdrives.driveclient.update.MetaData();
                metaData = function.apply(v, metaData);   //file.setParents(parentList);
                to.put(driveName, metaData);
            }else{
                to.put(driveName,function.apply(v, to.get(driveName)));//If the map previously contained a mapping for the key, the old value is replaced.
            }
        });
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

            //Log.d(TAG, "new parent: " + metaData.newData.get(driveName).getParents().get(0));
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