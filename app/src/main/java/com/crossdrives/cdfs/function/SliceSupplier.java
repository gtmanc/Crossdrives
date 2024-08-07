package com.crossdrives.cdfs.function;

import com.crossdrives.cdfs.data.Drive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SliceSupplier<T, R> {
    interface ISliceConsumerCallback<R>{
        void onStart();

        void onSupplied(R r);

        //Called when the last item is pass to the operation. The user needs to take care the ongoing
        //threads.
        void onCompleted(int totalSliceSupplied);

        void onFailure();
    }

    private HashMap<String, Drive> mDrives;
    HashMap<String, List<T>> mItems;
    private int mMaxThread = 3;
    ArrayBlockingQueue<R> mQueue;
    Collection<CompletableFuture<R>> mFutures = new ArrayList<CompletableFuture<R>>();
    private boolean mStarted = false;

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
        mQueue = new ArrayBlockingQueue<R>(mMaxThread);

        CompletableFuture.supplyAsync(()->{
            int[] cntOfT = new int[]{0};
            int[] cntOfRemainingSlice = new int[0];

            //TODO: may add some checks before the onStarted is called
            if(mStarted){mCallback.onStart();}

            mItems.entrySet().stream().forEach((set)->{
               mQueue..offer()
            });



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

            mCallback.onCompleted(mItems.size());
            return null;
        });
    }
}
