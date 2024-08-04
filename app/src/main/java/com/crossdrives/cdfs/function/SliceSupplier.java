package com.crossdrives.cdfs.function;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.driveclient.IDriveClient;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class SliceSupplier<T1, R> {
    interface ISliceConsumerCallback<R2>{
        void onStart();

        void onSupplied(R2 r);

        void onCompleted(int totalSliceSupplied);

        void onFailure();

    }

    private HashMap<String, IDriveClient> mCients;
    HashMap<String, List<T1>> mItems;
    private int mNoOfThread = 5;
    private boolean mStarted = false;

    private ISliceConsumerCallback mCallback;

    private Function<? super T1, CompletableFuture<R>> mOperation;


    public SliceSupplier(HashMap<String, IDriveClient> clients,
                         HashMap<String, List<T1>> items,
                         Function<? super T1, CompletableFuture<R>> operation) {
        mCients = clients;
        mItems = items;
        mOperation = operation;
    }


    public SliceSupplier maxOngoingThread(int noOfT){
        mNoOfThread = noOfT;
        return this;
    }

    public SliceSupplier setCallback(ISliceConsumerCallback callback){
        mCallback = callback;
        return this;
    }

    public void run(){

        CompletableFuture.supplyAsync(()->{
            int[] CntOfT = new int[]{0};
            int[] remainingSlice = new int[0];
            remainingSlice[0] = mItems.size();
            if(mStarted){mCallback.onStart();}

            while(remainingSlice[0] > 0){
                if(CntOfT[0] < mNoOfThread) {
                    CompletableFuture<R> opFuture = new CompletableFuture<>();
                    opFuture = mOperation.apply(mItems.get("").get(0));
                    opFuture.thenAccept((r) -> {
                        mCallback.onSupplied(r);
                        CntOfT[0]--;
                        remainingSlice[0]--;
                    });
                    CntOfT[0]++;
                }
            }
            mCallback.onCompleted(mItems.size());
            return null;
        });
    }
}
