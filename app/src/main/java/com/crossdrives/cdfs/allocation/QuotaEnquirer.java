package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.remote.Enquirer;
import com.crossdrives.cdfs.util.Mapper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuotaEnquirer {
    private final String TAG = "CD.QuotaEnquirer";
    ConcurrentHashMap<String, Drive> mDrives;

    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    public QuotaEnquirer(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public Task<HashMap<String, About.StorageQuota>> getAll(){
        Task<HashMap<String, About.StorageQuota>> task=null;

        task = Tasks.call(sExecutor, new Callable<HashMap<String, About.StorageQuota>>() {
            @Override
            public HashMap<String, About.StorageQuota> call() throws Exception {
                HashMap<String, About.StorageQuota> quotas = new HashMap<>();
                Enquirer enquirer = new Enquirer(mDrives);
                CompletableFuture<HashMap<String, About>> abouts =
                enquirer.getAll();

                quotas = Mapper.reValue(abouts.join(), (about)->{
                    return about.getStorageQuota();
                });

                return quotas;
            }
        });

        return task;
    }

}
