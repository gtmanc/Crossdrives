package com.crossdrives.cdfs.remote;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.updateFile;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.driveclient.update.IUpdateCallBack;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/*
    update meta data or content of file for user's drives
*/
public class updater {

    final String TAG = "CD.Locker";
    ConcurrentHashMap<String, Drive> mDrives;


    public updater(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateAll(HashMap<String, updateFile> files){
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

    CompletableFuture<com.google.api.services.drive.model.File> update(
            String driveName,
             String fileID,
             com.google.api.services.drive.model.File metaData,
             FileContent mediaContent) throws IOException {
        CompletableFuture<com.google.api.services.drive.model.File> future = new CompletableFuture<>();
        //Log.d(TAG, "local file to upload: " + localFile.getName());

        this.mDrives.get(driveName).getClient().update().
                buildRequest(fileID, metaData, mediaContent).run(new IUpdateCallBack<com.google.api.services.drive.model.File>() {
                    @Override
                    public void success(com.google.api.services.drive.model.File file) {
                        future.complete(file);
                    }

                    @Override
                    public void failure(String ex) {
                        future.completeExceptionally(new Throwable(ex));
                    }
                });
        return future;
    }
}
