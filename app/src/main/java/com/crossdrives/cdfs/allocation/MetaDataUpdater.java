package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.model.UpdateFile;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MetaDataUpdater
{
    class MetaData{
        UpdateFile updateFile;

        public MetaData() {}

        public MetaData(MetaData metaData) {
            updateFile = metaData.updateFile;
        }
        public MetaData clone(){return new MetaData(this);}
    }

    HashMap<String, MetaData> metaData = new HashMap<>();

    public MetaDataUpdater parent(CdfsItem parent){
        parent.getMap().entrySet().stream().forEach(set->{
            String driveName = set.getKey();
            //Always take first element as a folder only contains only one drive item.
            String id = set.getValue().get(0);
            List<String> parentList = new ArrayList<>();
            parentList.add(id);
            metaData.get(driveName).updateFile.getMetadata().setParents(parentList);
        });
        return this;
    }

    void run(String id){
        com.google.api.services.drive.model.File metaData = new File();
        metaData.setId(id);

    }

    CompletableFuture<String> run(HashMap<String, List<String>> idList){
        return CompletableFuture.supplyAsync(()->{
            upda
            List<CompletableFuture<String>> works =
            idList.entrySet().stream().map((set)->{

                return;
            });

            return null;
        })

    }

}
