package com.crossdrives.cdfs.remote;

import android.util.Log;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.UpdateFile;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.update.IUpdateCallBack;
import com.crossdrives.driveclient.update.IUpdateRequest;
import com.crossdrives.driveclient.update.MetaData;
import com.google.api.client.http.FileContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
    update meta data or content of file for user's drives
*/
public class updater {

    final String TAG = "CD.updater";
    HashMap<String, Drive> mDrives;

    HashMap<String, List<String>> oldParents;

    public updater(HashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateAll(HashMap<String, UpdateFile> files){
        HashMap<String, CompletableFuture<com.google.api.services.drive.model.File>> Futures = new HashMap<>();

        files.forEach((driveName, file)->{
            CompletableFuture<com.google.api.services.drive.model.File> future =
                    null;
            try {
                future = update(driveName, file.getID(), file.getMetadata(), file.getMediaContent());
            } catch (IOException e) {
                future = new CompletableFuture<>();
                future.completeExceptionally(e);
            }
            Futures.put(driveName, future);
        });

        CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> resultFuture = CompletableFuture.supplyAsync(()->{
            return Mapper.reValue(Futures, (in)->{
                return in.join();
            });
        });

        return resultFuture;
    }

    public CompletableFuture<List<com.google.api.services.drive.model.File>> updateAll(
            String driveName,
            List<String> idList,
            @Nullable MetaData metaData,
            FileContent mediaContent) {
        List<CompletableFuture<com.google.api.services.drive.model.File>> futures = new ArrayList<>();

        idList.stream().forEach((id)->{
            CompletableFuture<com.google.api.services.drive.model.File> future = null;
            try {
                future = update(driveName, id, metaData, mediaContent);
            } catch (IOException e) {
                future = new CompletableFuture<>();
                future.completeExceptionally(e);
            }
            futures.add(future);
        });

        CompletableFuture<List<com.google.api.services.drive.model.File>> resultFuture = CompletableFuture.supplyAsync(()->{
            List<com.google.api.services.drive.model.File> result = new ArrayList<>();

            futures.stream().forEach((future)->{
                result.add(future.join());
            });
            return result;

        });

        return resultFuture;

    };

    private CompletableFuture<com.google.api.services.drive.model.File> update(
            String driveName,
             String fileID,
             @Nullable MetaData metaData,
             FileContent mediaContent) throws IOException {
        CompletableFuture<com.google.api.services.drive.model.File> future = new CompletableFuture<>();
        Log.d(TAG, "metaData: " + metaData);
        if(metaData != null) {
            Log.d(TAG, "original parents: " + metaData.getParents());
        }

        IUpdateRequest updateRequest =
        this.mDrives.get(driveName).getClient().update().
                buildRequest(fileID, metaData, mediaContent);

        updateRequest.run(new IUpdateCallBack<com.google.api.services.drive.model.File>() {
                    @Override
                    public void success(com.google.api.services.drive.model.File file) {
                        future.complete(file);
                    }

                    @Override
                    public void failure(String ex) {
                        future.completeExceptionally(new Throwable(ex));
                    }
                });

        if(metaData != null) {
            Log.d(TAG, "parents altered: " + metaData.getParents());
        }
        return future;
    }
}
