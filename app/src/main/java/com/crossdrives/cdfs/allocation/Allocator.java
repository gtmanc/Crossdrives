package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.util.Mapper;
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
        final long remoteFree;
        AtomicLong sizeAllocated = new AtomicLong();

        remoteFree = totalRemoteFree(mQuotas);

        if(sizeUpload > remoteFree){
            Log.w(TAG, "available remote space is not enough for allocation");
            throw new CompletionException("available remote space is not enough for allocation", new Throwable(""));
        }

        //return new MethodFillFully().allocate(sizeUpload, mQuotas);
        return new MethodAverage().allocate(sizeUpload, mQuotas);
    }

    /*
        Allocation strategy:
        1. fill the free space of a drive
        2. Average
    */
    interface Method{

        HashMap<String, Long> allocate(long sizeUpload, HashMap<String, About.StorageQuota> quota);
    }

    class MethodFillFully implements Method{

        @Override
        public HashMap<String, Long> allocate(long sizeUpload, HashMap<String, About.StorageQuota> quota) {
            HashMap<String, Long> result = new HashMap<>();


            final long[] notYetAllocated = {sizeUpload};

            quota.entrySet().stream().forEach((quotaEntry)->
            {
                final String drive = quotaEntry.getKey();
                final long used = quotaEntry.getValue().getUsage();
                final long limit = quotaEntry.getValue().getLimit();
                final long available = limit - used;
                long allocated = notYetAllocated[0];
                if (notYetAllocated[0] > available) {
                    allocated = available;
                }
                result.put(drive, allocated);
                notYetAllocated[0] -= allocated;
            });

            /*
                If not yet allocated is not zero. it must be something wrong
            */
            if(notYetAllocated[0] > 0) {
                Log.w(TAG, "Something wrong in allocation strategy");}

            Log.d(TAG, "allocation result: " + result);
            return result;
        }
    }

    class MethodAverage implements Method {
        class AllocatedQuota {
            long allocated;
            long free;
        }

        @Override
        public HashMap<String, Long> allocate(long sizeUpload, HashMap<String, About.StorageQuota> quotas) {
            long count, remaining;
            //initialize the result
            HashMap<String, AllocatedQuota> resultQuota = Mapper.reValue(quotas, (quota) -> {
                AllocatedQuota aq = new AllocatedQuota();
                aq.allocated = 0;
                aq.free = quota.getLimit() - quota.getUsage();
                return aq;
            });

            remaining = sizeUpload;
            int try_cnt = 0;
            while (remaining > 0 && try_cnt < 10) {
                count = resultQuota.values().stream().filter((v) -> v.free >= 0).count();
                Log.d(TAG, "number of drives still have free space: " + count);
                Log.d(TAG, "remaining to allocate: " + remaining);
                final long averaged = (remaining / count);
                if(averaged <= 0){
                    break;      //stop here and the remaining will be taking care later
                }

                resultQuota.entrySet().forEach((entry) -> {
                    long q;
                    q = averaged;
                    if (q > entry.getValue().free) {
                        q = entry.getValue().free;
                    }
                    resultQuota.get(entry.getKey()).allocated += q;
                    resultQuota.get(entry.getKey()).free -= q;
                    Log.d(TAG, "allocated quota: drive: " + entry.getKey() + " allocated length: "
                            + resultQuota.get(entry.getKey()).allocated + " free: " + resultQuota.get(entry.getKey()).free);
                });

                remaining = sizeUpload - resultQuota.values().stream().mapToLong(v -> v.allocated).sum();
                try_cnt++;
            }

            //take care the remaining. The remaining must be less than the number of drive which has free space
            final long[] rest = {remaining};
            resultQuota.values().stream().filter((v) -> v.free >= 0).forEach((q)->{
                long toAllocate = 0;
                if(rest[0] >= 1){
                    toAllocate = 1;
                }
                q.allocated +=toAllocate;
                q.free-=toAllocate;
                rest[0]--;
            });

            return Mapper.reValue(resultQuota, (q) -> {
                return q.allocated;
            });
        }
    }

    long totalRemoteFree(HashMap<String, About.StorageQuota> quota){
        return quota.values().stream().mapToLong((q)->
                {return (q.getLimit()- q.getUsage());})
                .sum();
    }
}
