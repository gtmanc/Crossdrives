package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.updateContent;
import com.crossdrives.cdfs.model.updateFile;
import com.crossdrives.cdfs.remote.updater;
import com.crossdrives.cdfs.util.Mapper;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapUpdater {

    ConcurrentHashMap<String, Drive> mDrives;

    public MapUpdater(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, File>> updateAll(HashMap<String, updateContent> contents){

        updater updater = new updater(mDrives);

        HashMap<String, updateFile> files = Mapper.reValue(contents,(in)->{
            updateFile file = new updateFile();
            FileContent fileContent = new FileContent("application/octet-stream", in.getMediaContent());
            file.setID(in.getID());
            file.setMediaContent(fileContent);
            return file;
        });


        return updater.updateAll(files);
    }
}
