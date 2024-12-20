package com.crossdrives.cdfs.function;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SliceConsumer<T, R> {
    final String TAG ="CD.SliceConsumer";
    public interface ISliceConsumerCallback<T, R>{
        void onStart();

        //Only called if the requested items are not fill at once. i.e. mItems is not null.
        //A returned null indicates there is no slice to consume anymore.
        T onRequested();

        void onConsumed(R r);

        void onCompleted(Collection<R> consumed);

        void onFailure(String reason);
    }
    private HashMap<String, Drive> mDrives;

    //Only used and valid when the requested items are not provided at once
    HashMap<String, List<T>> mItems;
    private int mMaxThread = 3;
    ArrayBlockingQueue<CompletableFuture<R>> mQueue;

    //The flag is only used and valid when the requested items are not provided at once
    private boolean mStop = false;

    private ISliceConsumerCallback<T, R> mCallback;

    private Function<? super T, CompletableFuture<R>> mOperation;

    private Collection<R> mConsumedSlices = new ArrayList<>();

    public SliceConsumer(HashMap<String, Drive> mDrives,
                         Function<? super T, CompletableFuture<R>> mOperation) {
        this.mDrives = mDrives;
        this.mOperation = mOperation;
    }

    public SliceConsumer maxOngoingThread(int noOfT){
        //mNoOfThread = noOfT;
        return this;
    }

    public SliceConsumer setCallback(ISliceConsumerCallback callback){
        mCallback = callback;
        return this;
    }

    public Collection<R> getConsumed(){
        return mConsumedSlices;
    }

    public CompletableFuture run(){
        mQueue = new ArrayBlockingQueue<CompletableFuture<R>>(mMaxThread);

        //TODO: may add some checks before the onStarted is called
        mCallback.onStart();

        return CompletableFuture.supplyAsync(()->{
            boolean[] stop = new boolean[1];
            stop[0] = false;

            while(true){
                T requestedSlice = mCallback.onRequested();
                if(requestedSlice == null){break;}
                final CompletableFuture<R> future =
                        mOperation.apply(requestedSlice);
                if(!put(future)){
                    Log.e(TAG, "failed to add element to the queue!");
                    mCallback.onFailure("error occurred whiling adding item to the queue!");
                 }
                future.thenAccept((r) -> {
                    mConsumedSlices.add(r);
                    mCallback.onConsumed(r);
                    //Log.d(TAG, "returned from mCallback.onConsumed. size of q: " + mQueue.size());
                    if(mQueue.isEmpty()){
                        Log.w(TAG, "queue is empty before deleting!");
                        return;}
                    boolean successRemoval = mQueue.remove(future);
                    //Log.d(TAG, "size of q: " + mQueue.size());
                    if(!successRemoval){
                        Log.w(TAG, "fail to remove element in queue!");}
                }).exceptionally(ex->{
                    Log.w(TAG, "future task gets error! " + ex.toString());
                    stop[0] = true;
                    mCallback.onFailure("future task gets error. future task: " + future.toString());
                    return null;
                });
            }

            //take care the rest of ongoing futures
            Collection<CompletableFuture<R>> futures = new ArrayList<CompletableFuture<R>>();
            //mQueue.drainTo(futures);
            mQueue.stream().forEach((f)->{futures.add(f);});

            //if we have failure, cancel the ongoing futures
            if(stop[0]){futures.stream().forEach(f->f.cancel(true));}

            futures.forEach((f->{
                //Log.d(TAG, "size of q: " + mQueue.size());
                f.join();}));

            Log.d(TAG, "block until q is empty. size of q: " + mQueue.size());
            while(!mQueue.isEmpty());

            mCallback.onCompleted(mConsumedSlices);
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
