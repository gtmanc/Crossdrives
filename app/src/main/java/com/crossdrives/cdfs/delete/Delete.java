package com.crossdrives.cdfs.delete;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Delay;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.delete.IDeleteCallBack;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Delete {
    static final String TAG = "CD.Delete";
    CDFS mCDFS;
    final String mFileID;
    String mParent;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    final int MAX_CHUNK = 10;
    final String POISON_PILL = "PoisonPill";

    IDeleteProgressListener mListener;

    public Delete(CDFS cdfs, String id, String parent, IDeleteProgressListener listener) {
        this.mCDFS = cdfs;
        this.mFileID = id;
        this.mParent = parent;
        mListener = listener;
    }

    public Task<File> execute(){
        Task<File> task;

        task = task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public File call() throws Exception {
                File result = new File();
                Collection<Throwable> exceptions = new ArrayList<>();
                HashMap<String, CompletableFuture<String>> futures = new HashMap<>();
                Log.d(TAG, "Fetch map...");
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();
                Log.d(TAG, "map fetched");

                result.setId(mFileID);
                HashMap<String, AllocContainer> updatedMaps = removeNotMatched(maps, mFileID);
                Log.d(TAG, "Not matched removed: " + updatedMaps);
                printListContainer(updatedMaps);
                HashMap<String, Collection<AllocationItem>> toDeleteList = toList(maps, mFileID);

                //printCollectionString(toDeleteList);
                //printDeleteList(updatedMaps, toDeleteList);

                //Update the allocation map files
                MapUpdater updater = new MapUpdater(mCDFS.getDrives());

                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(updatedMaps, mParent);
                updateFuture.join();

                //TODO: deal with once error occurs from now on. i.e. Orphen content file in remote drives.
                //Start to delete the items
                toDeleteList.entrySet().forEach((set)->{
                    String driveName = set.getKey();
                    CompletableFuture<String> deletionThread = CompletableFuture.supplyAsync(()->{
                        AtomicInteger runningDeletion = new AtomicInteger();
                        ArrayList<AllocationItem> list = new ArrayList<>(set.getValue());
                        LinkedBlockingDeque<AllocationItem> toDeleteQ = new LinkedBlockingDeque<>();
                        //to deal with the case that number of element is more than size of queue.
                        toDeleteQ.addAll(set.getValue());
                        toDeleteQ.add(null);
                        //iterate until all of the deletion threads have been submitted
                        while(!toDeleteQ.isEmpty()) {
                            if(runningDeletion.get() > MAX_CHUNK){
                                Delay.delay(1000);
                                continue;
                            }

                            CompletableFuture<File> future;
                            AllocationItem item = null;
                            try {
                                item = toDeleteQ.take();
                            } catch (InterruptedException e) {
                                exceptions.add(e);
                                Log.w(TAG, e.getMessage());
                                return null;
                            }
                            if(item == null){
                                Log.d(TAG, "End of list.");
                                if(!toDeleteQ.isEmpty()) {
                                    Log.w(TAG, "Warning! Queue is not empty.");
                                }
                                return null;
                            }
                            future = delete(set.getKey(), item);
                            runningDeletion.getAndIncrement();
                            future.thenAccept((file) -> {
                                Log.d(TAG, "Item deleted. Drive: " + driveName + ". Item: " + file.getName());
                                runningDeletion.getAndDecrement();
                            }).exceptionally(ex->{
                                exceptions.add(new Throwable(ex));
                                Log.w(TAG, ex);
                                return null;
                            });
                        }
                        return null;    //Just a placeholder for the result
                    });

                    futures.put(driveName, deletionThread);
                });

                if(!exceptions.isEmpty()){
                    throw new CompletionException("",exceptions.stream().findAny().get());
                }

                futures.entrySet().stream().forEach((set)->{
                    set.getValue().join();
                });

                if(!exceptions.isEmpty()){
                    throw new CompletionException("",exceptions.stream().findAny().get());
                }
                return result;
            }
        });

        return task;
    }

    HashMap<String, AllocContainer> removeNotMatched(HashMap<String, OutputStream> maps, String id){
        //Remove the entry set which contains no item matched.
//        Log.d(TAG, "ID to remove: " + id);
//        Map<String, OutputStream> reduced = maps.entrySet().stream().filter((set)->{
//            AllocContainer container = AllocManager.toContainer(set.getValue());
//            return container.getAllocItem().stream().anyMatch((item)->{
//                Log.d(TAG, "Item matched. CDFS ID: " + item.getCdfsId());
//                return !item.getCdfsId().equals(id);});
//        }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));
//        Log.d(TAG, "Reduced: " + reduced);

//        HashMap<String, OutputStream> reducedMaps = new HashMap<>(reduced);
        HashMap<String, AllocContainer> mapped = Mapper.reValue(maps, (stream)->{
            return AllocManager.toContainer(stream);
        });

        //Remove the entry which is not matched
        HashMap<String, AllocContainer> containers = Mapper.reValue(mapped, (container)->{
            List<AllocationItem> list =
                    container.getAllocItem().stream().filter((item)->{
                        return !item.getCdfsId().equals(id);
                    }).collect(Collectors.toList());

            AllocContainer newContainer = AllocManager.newAllocContainer();
            newContainer.addItems(list);
            return newContainer;
        });

        return containers;
    }

    HashMap<String, Collection<AllocationItem>> toList(HashMap<String, OutputStream> maps, String id){
        Log.d(TAG, "Produce delete list...");
        HashMap<String, Collection<AllocationItem>> containers = Mapper.reValue(maps, (stream)->{
            AllocContainer oldContainer = AllocManager.toContainer(stream);
            Collection<AllocationItem> list =
                    oldContainer.getAllocItem().stream().filter((item)->{
                        return item.getCdfsId().equals(mFileID);
                    })
//                            .map((allocationItem)->{
//                        Log.d(TAG, "item to remove: " + allocationItem.getSequence());
//                        return allocationItem.getItemId();
//                    })
                .collect(Collectors.toCollection(ArrayList::new));
            return list;
        });
        return containers;
    }

    CompletableFuture<File> delete(String driveName, AllocationItem item){
        CompletableFuture<File> future = new CompletableFuture<>();
        com.google.api.services.drive.model.File file = new File();
        file.setId(item.getItemId());
        file.se
        mCDFS.getDrives().get(driveName).getClient().delete().buildRequest(file).run(new IDeleteCallBack<File>() {
            @Override
            public void success(File file) {
                future.complete(file);
            }

            @Override
            public void failure(String ex) {
                future.completeExceptionally(new Throwable(ex));
            }
        });
        return future;
    }

    void printListContainer(HashMap<String, AllocContainer> maps){
        maps.entrySet().stream().forEach((set)->{
            String[] allItems = {""};
            Log.d(TAG, "Drive: " + set.getKey() + ". Size of list:" + set.getValue().getAllocItem().size());
            set.getValue().getAllocItem().stream().forEach((item)->{
                allItems[0] = allItems[0].concat(Integer.toString(item.getSequence()));
                allItems[0] = allItems[0].concat(" ");
            });
            Log.d(TAG,  "Seq of items: " + allItems[0]);
        });

    }
    void printCollectionString(HashMap<String, Collection<String>> map){

        map.entrySet().stream().forEach((set)->{
            Log.d(TAG, "Drive: " + set.getKey() + ". Size of collection:" + set.getValue().size());
            String[] concatenated = {""};
            set.getValue().stream().forEach((s)->{
                concatenated[0] = concatenated[0].concat(s);
                concatenated[0] = concatenated[0].concat(" ");
            });
            Log.d(TAG, "IDs: " + concatenated[0]);
        });
    }

    void printDeleteList(HashMap<String, AllocContainer> maps, HashMap<String, Collection<String>> list){
        Log.d(TAG, "Length of delete list: " + list.size());
        list.entrySet().stream().forEach((set)->{
            maps.get(set.getKey()).getAllocItem().stream().filter((item)->{
                return set.getValue().equals(item.getItemId());
            }).forEach((filtered)->{
                Log.d(TAG, "drive: " + set.getKey() + " item: " + filtered.getName());
            });
        });
    }

}
