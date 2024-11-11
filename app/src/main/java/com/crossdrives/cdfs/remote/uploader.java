package com.crossdrives.cdfs.remote;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.upload.IUploadCallBack;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class uploader {
    HashMap<String, Drive> mDrives;

    public uploader(HashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    /*
        Input:
            metaData for each available drives. where:
            file:   metaData will be directly sent to drive request
            originalLocalFile: the path name to the local file to upload
     */
    public CompletableFuture<HashMap<String, com.crossdrives.driveclient.model.File>> uploadAll(HashMap<String, com.crossdrives.driveclient.model.File> files){
        HashMap<String, CompletableFuture<com.crossdrives.driveclient.model.File>> Futures = new HashMap<>();

        files.forEach((driveName, file)->{
            CompletableFuture<com.crossdrives.driveclient.model.File> future =
            future = upload(driveName, file.getFile(), file.getOriginalLocalFile());

            Futures.put(driveName, future);
        });

        CompletableFuture<HashMap<String, com.crossdrives.driveclient.model.File>> resultFuture = CompletableFuture.supplyAsync(()->{
            return Mapper.reValue(Futures, (in)->{
                return in.join();
            });
        });

        return resultFuture;
    }

    CompletableFuture<com.crossdrives.driveclient.model.File> upload(
            String driveName,
            com.google.api.services.drive.model.File metaData,
            java.io.File file){
        CompletableFuture<com.crossdrives.driveclient.model.File> future = new CompletableFuture<>();
        //Log.d(TAG, "local file to upload: " + localFile.getName());

        this.mDrives.get(driveName).getClient().upload().
                buildRequest(metaData, file).run(new IUploadCallBack() {
                    @Override
                    public void success(com.crossdrives.driveclient.model.File file) {
                        future.complete(file);
                    }

                    @Override
                    public void failure(String ex, java.io.File originalFile) {
                        future.completeExceptionally(new Throwable(ex));
                    }
                });
        return future;
    }
}
