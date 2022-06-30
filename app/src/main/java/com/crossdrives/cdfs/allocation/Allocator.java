package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.google.api.services.drive.model.About;

import java.util.HashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

/*
    Allocation strategy
 */
public class Allocator {
    final String TAG = "CD.Allocator";
    HashMap<String, About.StorageQuota> mQuotas;
    long sizeUpload;

    public Allocator(HashMap<String, About.StorageQuota> quotas, long size) {
        mQuotas = quotas;
        sizeUpload = size;
    }

    public HashMap<String, Long> getAllocationResult(){
        HashMap<String, Long> allocation = new HashMap<>();
        final long remoteFree;
        AtomicLong sizeAllocated = new AtomicLong();

        /*
            Reset allocation by putting the allocation size zero
         */
        mQuotas.keySet().stream().forEach((k)->
        {allocation.put(k, (long)0);});

        remoteFree = totalRemoteFree(mQuotas);

        if(sizeUpload > remoteFree){
            throw new CompletionException("available remote space is not enough for allocation", new Throwable(""));
        }

        /*
            Strategy: try to allocate the upload size into single drive.
         */
        //count = mQuotas.keySet().stream().count();
        //averaged = (total/count);
        //remainder = (total%count);
        final long[] notYetAllocated = {sizeUpload};

        mQuotas.entrySet().stream().forEach((quotaEntry)->
                {
                    final String drive = quotaEntry.getKey();
                    final long used = quotaEntry.getValue().getUsage();
                    final long limit = quotaEntry.getValue().getLimit();
                    final long available = limit - used;
                    long allocated = notYetAllocated[0];
                    if (notYetAllocated[0] > available) {
                        allocated = available;
                    }
                    allocation.replace(drive, allocated);
                    notYetAllocated[0] -= allocated;
                });
        /*
            If not yet allocated is no zero. it must be something wrong
         */
        if(notYetAllocated[0] > 0) {
            Log.e(TAG, "Something wrong in allocation strategy");}
        return allocation;
    }


    interface Method{

        HashMap<String, Long> allocate(long size, HashMap<String, About.StorageQuota> quota);
    }

    class MethodFullFill implements Method{

        @Override
        public HashMap<String, Long> allocate(long size, HashMap<String, About.StorageQuota> quota) {
            HashMap<String, Long> result = new HashMap<>();


            final long[] notYetAllocated = {size};

            mQuotas.entrySet().stream().forEach((quotaEntry)->
            {
                final String drive = quotaEntry.getKey();
                final long used = quotaEntry.getValue().getUsage();
                final long limit = quotaEntry.getValue().getLimit();
                final long available = limit - used;
                long allocated = notYetAllocated[0];
                if (notYetAllocated[0] > available) {
                    allocated = available;
                }
                result.replace(drive, allocated);
                notYetAllocated[0] -= allocated;
            });

            /*
                If not yet allocated is no zero. it must be something wrong
            */
            if(notYetAllocated[0] > 0) {
                Log.e(TAG, "Something wrong in allocation strategy");}

            return result;
        }
    }

    long totalRemoteFree(HashMap<String, About.StorageQuota> quota){
        return quota.values().stream().mapToLong((q)->
                {return (q.getLimit()- q.getUsage());})
                .sum();
    }
}
