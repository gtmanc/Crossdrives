package com.crossdrives.cdfs.remote;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.about.IAboutCallBack;
import com.google.api.services.drive.model.About;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Enquirer {
    private final String TAG = "CD.Enquirer";
    HashMap<String, Drive> mDrives;


    public Enquirer(HashMap<String, Drive> drives) {
        this.mDrives = drives;
    }

    public CompletableFuture<HashMap<String,About>> getAll() {
        HashMap<String, CompletableFuture<About>> futures = new HashMap<>();
        CompletableFuture<HashMap<String,About>> resultFuture = new CompletableFuture<>();
        resultFuture = CompletableFuture.supplyAsync(() -> {
            HashMap<String, About> remapped;
            mDrives.forEach((driveName, drive) -> {
                CompletableFuture<About> future = get(driveName);
                futures.put(driveName, future);
            });
            remapped = Mapper.reValue(futures, (aboutFuture) -> {
                return aboutFuture.join();
            });

            return remapped;
        });
        return resultFuture;
    }

    public CompletableFuture<About> get(String driveName){
        CompletableFuture<About> future = new CompletableFuture<>();
        mDrives.get(driveName).getClient().about().buildRequest().run(new IAboutCallBack() {
            @Override
            public void success(About about) {
                //Log.d(TAG, "about got! ");
                future.complete(about);
            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, ex);
                future.completeExceptionally(new Throwable(ex));
            }
        });

        return future;
    }
}
