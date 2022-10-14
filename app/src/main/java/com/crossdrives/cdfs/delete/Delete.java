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
import com.crossdrives.cdfs.util.Wait;
import com.crossdrives.driveclient.delete.IDeleteCallBack;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    IDeleteProgressListener mListener;

    public Delete(CDFS cdfs, String id, String parent, IDeleteProgressListener listener) {
        this.mCDFS = cdfs;
        this.mFileID = id;
        this.mParent = parent;
        mListener = listener;
    }

    class Response{
        com.crossdrives.driveclient.model.File file;
        Throwable throwable;
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
                Log.d(TAG, "Not matched items removed. Maps ready to update: " + updatedMaps);
                Log.d(TAG, "Summary of the map files to update:");
                printListContainer(updatedMaps);
                HashMap<String, Collection<AllocationItem>> toDeleteList = toList(maps, mFileID);
                //we allocate the queue in the main working thread because it could be used for retry.
                HashMap<String, LinkedBlockingDeque<AllocationItem>> remainingQ = new HashMap<>();
                HashMap<String, Collection<AllocationItem>> remainingListMap = new HashMap<>();
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
                    Log.d(TAG, "Start to delete items in drive[" + driveName + "]. Number of item to delete: " + set.getValue().size());
                    int[] countSucceed = {0}, countFailed = {0};
                    /*
                        Start deletion threads(futures). The number of threads depends on how many drives registered
                        Note the threads end when the necessary delete requests are submitted only.
                     */
                    CompletableFuture<String> deletionThread = CompletableFuture.supplyAsync(()->{
                        AtomicInteger NumRunningDeletionFutures = new AtomicInteger();
                        ArrayList<AllocationItem> list = new ArrayList<>(set.getValue());
                        LinkedBlockingDeque<AllocationItem> toDeleteQ = new LinkedBlockingDeque<>();
                        //to deal with the case that number of element is more than size of queue.
                        toDeleteQ.addAll(set.getValue());
                        Collection<AllocationItem> remainingList = set.getValue().stream().collect(Collectors.toCollection(ArrayList::new));
                        remainingListMap.put(driveName, remainingList);
                        toDeleteQ.add(PoisonPill());
                        //iterate until all of the deletion threads have been submitted
                        while(!toDeleteQ.isEmpty()) {
                            if(NumRunningDeletionFutures.get() > MAX_CHUNK){
                                Log.d(TAG, "Running deletion futures overs the max[" + MAX_CHUNK + "]");
                                Delay.delay(1000);
                                continue;
                            }
//                            else{
//                                Log.d(TAG, "Running deletion futures: " + NumRunningDeletionFutures.get());
//                            }

                            CompletableFuture<com.crossdrives.driveclient.model.File> future;
                            AllocationItem item = null;
                            try {
                                item = toDeleteQ.take();
                            } catch (InterruptedException e) {
                                exceptions.add(e);
                                Log.w(TAG, e.getMessage());
                                return null;
                            }
                            if(item.getSequence() == 0){
                                Log.d(TAG, "End of list.");
                                if(!toDeleteQ.isEmpty()) {
                                    Log.w(TAG, "Something wrong! Queue is not empty.");
                                }
                                break;
                            }
                            NumRunningDeletionFutures.incrementAndGet();
                            future = delete(set.getKey(), item);
                            //Log.d(TAG, "Returned from delete().");
                            future.thenAccept((file) -> {
                                Log.d(TAG, "Item deleted OK. Drive: " + driveName + ". Seq: " + file.getInteger());
                                AllocationItem found = remainingList.stream().filter((r)->
                                {return (r.getSequence() == file.getInteger());}).findAny().get();
                                if(!remainingList.remove(found)){
                                    Log.w(TAG, "Item may not removed from remaining list as expctedly!");
                                }

                                countSucceed[0]++;
                            })
                            .exceptionally(ex->{
                                exceptions.add(new Throwable(ex));
                                Log.w(TAG, ex);
                                countFailed[0]++;
                                return null;
                            }).whenComplete((r,e)->{
                                //Log.d(TAG, "whenCompleted. deletion futures: " + NumRunningDeletionFutures.get());
                                NumRunningDeletionFutures.getAndDecrement();
                            });
                            //Log.d(TAG, "set future callback OK.");
//                            future.handle((file, t)->{
//                                Log.d(TAG, "Failed to delete item. Drive: " + driveName + ". Item: " + file.getFile().getName());
//                                NumRunningDeletionFutures.getAndDecrement();
//                                AllocationItem ai = getAiFromFile(file);
//                                remainingQ.get(driveName).add(ai);
//                                return null;
//                            });
                        }
                        /*
                            Wait until all the delete requests are done
                        */
                        int expected = set.getValue().size();
                        Wait wait = new Wait(expected);
                        while(!wait.isCompleted(countSucceed[0], countFailed[0]));
                        Log.d(TAG, "Deletion completed. Drive[ " + set.getKey() + "]");
                        return null;    //Just a placeholder for the result
                    });
                    /*
                        TODO: deal with the two cases that orphen drive items are left:
                        1. The deletion thread completes with error
                        2. Terminated with excpetionally of Deletion thread
                     */
                    deletionThread.exceptionally(ex->{
                        exceptions.add(ex);
                        Log.w(TAG, "Deletion thread: " + ex.getMessage());
                        ex.printStackTrace();
                        return null;
                    });
                    futures.put(driveName, deletionThread);
                });

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

    com.crossdrives.driveclient.model.File setAiToFile(AllocationItem ai){
        com.crossdrives.driveclient.model.File file = new com.crossdrives.driveclient.model.File();
        File gApiFile = new File();

        gApiFile.setId(ai.getItemId());

        file.setFile(gApiFile);
        file.setInteger(ai.getSequence());
        file.setString(ai.getCdfsId());
        
        return file;
    }

    AllocationItem getAiFromFile(com.crossdrives.driveclient.model.File file){
        AllocationItem ai = new AllocationItem();
        ai.setCdfsId(file.getString());
        ai.setSequence(ai.getSequence());
        return ai;
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

    CompletableFuture<com.crossdrives.driveclient.model.File> delete(String driveName, AllocationItem item){
        Log.d(TAG, "Submit request to drive client. drive:" + driveName + ". Seq:" + item.getSequence() + ".");
        CompletableFuture<com.crossdrives.driveclient.model.File> future = new CompletableFuture<>();
        //com.google.api.services.drive.model.File file = new File();
        com.crossdrives.driveclient.model.File file;

        Log.d(TAG, "future created.");
        file = setAiToFile(item);
        mCDFS.getDrives().get(driveName).getClient().delete().buildRequest(file).run(new IDeleteCallBack<com.crossdrives.driveclient.model.File>() {
            @Override
            public void success(com.crossdrives.driveclient.model.File file) {
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

    AllocationItem PoisonPill(){
        AllocationItem ai = new AllocationItem();
        //Set sequence to an impossible value for indicating PoisonPill.
        ai.setSequence(0);
        return ai;
    }

}
