package com.crossdrives.cdfs.function;

import com.crossdrives.cdfs.data.Drive;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SliceConsumer<T, R> {
    interface ISliceConsumerCallback<R>{
        void onStart();

        //Only called if the requested items are not fill at once. i.e. mItems is not null.
        //A returned null indicates there is no further requested. However, user needs to take care
        //the ongoing operation threads.
        R onRequested();

        void onConsumed(R r);

        //Only called if the requested items are filled when the constructor is invoked
        void onCompleted(int totalSliceSupplied);

        void onFailure();
    }
    private HashMap<String, Drive> mDrives;

    //Only used and valid when the requested items are not provided at once
    HashMap<String, List<T>> mItems;
    private int mNoOfThread = 3;
    private boolean mStarted = false;

    //The flag is only used and valid when the requested items are not provided at once
    private boolean mStop = false;

    private ISliceConsumerCallback<R> mCallback;

    private Function<? super R, CompletableFuture<R>> mOperation;

    public SliceConsumer(HashMap<String, Drive> mDrives,
                         Function<? super R, CompletableFuture<R>> mOperation) {
        this.mDrives = mDrives;
        this.mOperation = mOperation;
    }

    public SliceConsumer maxOngoingThread(int noOfT){
        mNoOfThread = noOfT;
        return this;
    }

    public SliceConsumer setCallback(ISliceConsumerCallback callback){
        mCallback = callback;
        return this;
    }

    void run(){
        int[] cntOfT = new int[0];

        if(mStarted){mCallback.onStart();}

        CompletableFuture.supplyAsync(()->{
            boolean stop = false;
            R requestedSlice;

            while(mStop){
                if(cntOfT[0] < mNoOfThread) {
                    requestedSlice = mCallback.onRequested();
                    if(requestedSlice != null) {
                        CompletableFuture<R> opFuture = new CompletableFuture<>();
                        opFuture = mOperation.apply(requestedSlice);
                        opFuture.thenAccept((r) -> {
                            mCallback.onConsumed(r);
                            cntOfT[0]--;
                        });
                        cntOfT[0]++;
                    }else{
                        mStop = true;
                    }
                }
            }

            return null;
        });
    }
}
