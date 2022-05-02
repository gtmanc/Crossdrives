package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.Infrastructure;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.about.IAboutCallBack;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.About;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DriveQuota {
    private final String TAG = "CD.DriveQuota";
    ConcurrentHashMap<String, Drive> mDrives;
    ICallback callback;

    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    interface ICallback<T>{
        public void onSuccess(T result);
        public void onFailure(String ex);
        public void onComplete(T result);
    }
    HashMap<String, CompletableFuture<About>> Futures= new HashMap<>();

    public DriveQuota(ConcurrentHashMap<String, Drive> mDrives) {
        this.mDrives = mDrives;
    }

    public Task<HashMap<String, About.StorageQuota>> fetchAllOf(ICallback callback){
        this.callback = callback;
        Task<HashMap<String, About.StorageQuota>> task=null;

        /*
            Submit fetch about task for each
        * */
        mDrives.forEach((name, drive)->{
            Log.d(TAG, "Start to get user about. Drive: " + name);
            CompletableFuture<About> future = new CompletableFuture<>();

            sExecutor.submit(() -> {
                drive.getClient().about().buildRequest().run(new IAboutCallBack() {
                    @Override
                    public void success(About about) {
                        Log.d(TAG, "about got! ");
                        future.complete(about);
                    }
                    @Override
                    public void failure(String ex) {
                        Log.w(TAG, ex);
                        future.completeExceptionally(new Throwable(ex));
                    }
                });
            });


            Futures.put(name, future);
        });

        /*
            Wait until all of the result are available.
         */
//        HashMap<String, About.StorageQuota> quotas = new HashMap<>();
//        Map<String, About> combined = Futures.entrySet().stream().map((entry)-> {
//            Map.Entry<String, About> entry1 = new Map.Entry<String, About>() {
//                @Override
//                public String getKey() {
//                    return entry.getKey();
//                }
//
//                @Override
//                public About getValue() {
//                            /*
//                                join will block current thread
//                             */
//                    return entry.getValue().join();
//                }
//
//                @Override
//                public About setValue(About value) {
//                    return null;
//                }
//            };
//            return entry1;
//        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
//
//        Log.d(TAG, "completed! ");
//
//        HashMap<String, About> result = new HashMap<>(combined);
//        result.forEach((k, v)-> {
//            quotas.put(k, v.getStorageQuota());
//        });

        task = Tasks.call(sExecutor, new Callable<HashMap<String, About.StorageQuota>>() {
            @Override
            public HashMap<String, About.StorageQuota> call() throws Exception {
                HashMap<String, About.StorageQuota> quotas = new HashMap<>();
                Map<String, About> combined = Futures.entrySet().stream().map((entry)-> {
                    Map.Entry<String, About> entry1 = new Map.Entry<String, About>() {
                        @Override
                        public String getKey() {
                            return entry.getKey();
                        }

                        @Override
                        public About getValue() {
                            /*
                                join will block current thread
                             */
                            return entry.getValue().join();
                        }

                        @Override
                        public About setValue(About value) {
                            return null;
                        }
                    };
                    return entry1;
                }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

                Log.d(TAG, "completed! ");

                HashMap<String, About> result = new HashMap<>(combined);
                result.forEach((k, v)-> {
                    quotas.put(k, v.getStorageQuota());
                });
                return quotas;
            }
        });

        return task;


//        HashMap<String, About> combined = Stream.generate(()-> ).
//                map((f)-> f.get()).collect((a)->Log.d(TAG, a.toString()));

//        Log.d(TAG, combined);

//        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
//
//            return "Future1";
//        });
//        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
//            return "Future2";
//        });
//
//        CompletableFuture<String> f11 = future1.thenCompose((s1)->{
//            CompletableFuture<String> future = new CompletableFuture<>();
//            s1 = s1.concat(" thenAccept"); Log.d(TAG, s1);future.complete(s1);return future;});
//        CompletableFuture<String> f12 = future2.thenCompose((s1)->{
//            CompletableFuture<String> future = new CompletableFuture<>();
//            s1 = s1.concat(" thenAccept"); Log.d(TAG, s1);future.complete(s1);return future;});
//
//        CompletableFuture<Void> combinedFuture
//                = CompletableFuture.allOf(f11, f12);
//
//        String combined = Stream.of(f11, f12)
//                .map(CompletableFuture::join)
//                .collect(Collectors.joining(" "));
//        Log.d(TAG, combined);
    }

}
