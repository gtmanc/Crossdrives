package com.crossdrives.cdfs.function;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SliceSupplier<T, R> {
    private final String TAG = "CD.SliceSupplier";
    public interface ISliceConsumerCallback<R>{
        void onStart();

        void onSupplied(R r) throws IOException, InterruptedException;

        void onCompleted(int totalSliceSupplied);

        void onFailure(String reason);
    }

    private HashMap<String, Drive> mDrives;
    HashMap<String, List<T>> mItems;
    private int mMaxThread = 3;
    ArrayBlockingQueue<CompletableFuture<R>> mQueue;
    Collection<CompletableFuture<R>> mFutures = new ArrayList<CompletableFuture<R>>();

    private ISliceConsumerCallback mCallback;

    private Function<? super T, CompletableFuture<R>> mOperation;


     public SliceSupplier(HashMap<String, Drive> drives,
                         HashMap<String, List<T>> items,
                         Function<? super T, CompletableFuture<R>> operation) {
        mDrives = drives;
        mItems = items;
        mOperation = operation;
    }


    public SliceSupplier maxOngoingThread(int noOfT){
        mMaxThread = noOfT;
        return this;
    }

    public SliceSupplier setCallback(ISliceConsumerCallback callback){
        mCallback = callback;
        return this;
    }

    public void run(){
        mQueue = new ArrayBlockingQueue<CompletableFuture<R>>(mMaxThread);

        //TODO: may add some checks before the onStarted is called
        mCallback.onStart();

        CompletableFuture.supplyAsync(()->{

            mItems.entrySet().stream().forEach((set)->{
                //Log.d(TAG, "SliceSupply: drive: " + set.getKey());
                set.getValue().stream().forEach((ai)->{
                    //Log.d(TAG, "SliceSupply: Item: " + ai);
                    CompletableFuture<R> opFuture = new CompletableFuture<>();
                    opFuture = mOperation.apply(ai);
                    //If queue is full, the add is blocked until element in queue is free
                    if(!put(opFuture)){
                        Log.e(TAG, "failed to add element to the queue!");
                        mCallback.onFailure("error occurred whiling adding item to the queue!");
                    }
                    opFuture.thenAccept((r) -> {
                        try {
                            mCallback.onSupplied(r);
                        } catch (Exception e) {
                            mCallback.onFailure("error occurred in handling of onSupply!");
                        }
                        //If the elements are drained out in the handling of rest of ongoing
                        //futures, skip the removal
                        if(mQueue.isEmpty()){return;}
                        //Here we use a non-blocking method because we don't want to create extra problem
                        boolean successRemoval = mQueue.remove(this);
                        if(!successRemoval){
                            Log.e(TAG, "fail to remove element from the queue!");
                            mCallback.onFailure("error occurred whiling removing item from the queue!");}
                    });
                });
            });

            //take care the rest of ongoing futures
            Collection<CompletableFuture<R>> futures = new ArrayList<CompletableFuture<R>>();
            mQueue.drainTo(futures);
            futures.forEach((f->{f.join();}));


            int[] cntOfT = new int[]{0};
            int[] cntOfRemainingSlice = new int[0];
            /*
            Iterator iterator = mItems.keySet().iterator();
            while(iterator.hasNext()){
                cntOfRemainingSlice[0] = mItems.get(iterator.next()).size();
                while(cntOfRemainingSlice[0] > 0) {
                    if (cntOfT[0] < mMaxThread) {
                        CompletableFuture<R> opFuture = new CompletableFuture<>();
                        opFuture = mOperation.apply(mItems.get("").get(0));
                        opFuture.thenAccept((r) -> {
                            mCallback.onSupplied(r);
                            cntOfT[0]--;
                            cntOfRemainingSlice[0]--;
                        });
                        cntOfT[0]++;
                    }
                }
            }
*/
            mCallback.onCompleted(mItems.size());
            return null;
        });
    }

    /*
        A helper to add element to the queue.
        Input:
        element: the element to add to the queue

        Output:
        Result[boolean]:
        delegate the responsibility of handling exception to the caller
     */
    private boolean put(CompletableFuture<R> element){
        boolean result = true;
        try {
            mQueue.put(element);
        } catch (InterruptedException e) {
            result = false;
        }
        return result;
    }
}
