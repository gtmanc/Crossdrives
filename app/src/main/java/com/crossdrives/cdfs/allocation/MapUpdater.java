package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.data.FileLocal;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.UpdateContent;
import com.crossdrives.cdfs.model.updateFile;
import com.crossdrives.cdfs.remote.updater;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapUpdater {
    final String TAG = "CD.MapFetcher";
    ConcurrentHashMap<String, Drive> mDrives;

    public MapUpdater(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, File>> updateAll(HashMap<String, UpdateContent> contents){

        updater updater = new updater(mDrives);

        //map UpdateContent to UpdateFile
        HashMap<String, updateFile> files = Mapper.reValue(contents,(in)->{
            updateFile file = new updateFile();
            FileContent fileContent = new FileContent("application/octet-stream", in.getMediaContent());
            file.setID(in.getID());
            file.setMediaContent(fileContent);
            return file;
        });


        return updater.updateAll(files);
    }

    public CompletableFuture<HashMap<String, File>> updateAll(HashMap<String, AllocContainer> containers
        , String parent){
        CompletableFuture<HashMap<String, File>> resultFuture = new CompletableFuture<>();
        CompletableFuture.supplyAsync(()->{
            MapFetcher mapFetcher = new MapFetcher(mDrives);
            CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> mapIDFuture = mapFetcher.listAll(parent);
            HashMap<String, com.google.api.services.drive.model.File> mapIDs = mapIDFuture.join();
            if(mapIDs.values().stream().anyMatch((v)->v==null)){
                Log.w(TAG, "map is missing!");
                resultFuture.completeExceptionally(new Throwable("map is missing!"));
                //TODO: clean up orphan files
                return null;
            }

            HashMap<String, UpdateContent> localMaps = Mapper.reValue(containers, (driveName, container)->{
                Gson gson = new Gson();
                FileLocal creator = new FileLocal(SnippetApp.getAppContext());
                UpdateContent content = new UpdateContent();
                content.setID(mapIDFuture.join().get(driveName).getId());
                String localMapName = driveName + "_map.txt";
                content.setMediaContent(creator.create(localMapName, gson.toJson(container)));
                return content;
            });

            Log.d(TAG, "update local map files...");
            MapUpdater updater = new MapUpdater(mDrives);

            //TODO: what will happen if two source update the same file in remote?
            CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                    = updater.updateAll(localMaps);

            resultFuture.complete(updateFuture.join());
            return null;
        });

        return resultFuture;
    }

//    public CompletableFuture<HashMap<String, File>> updateAll(HashMap<String, OutputStream> containers
//                , HashMap<String, String> parents){
//
//    return null;
//
//    }
}
