package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Mapper;
import com.google.api.services.drive.model.About;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    Allocation strategy
 */
public class Allocator {
    final String TAG = "CD.Allocator";
    HashMap<String, About.StorageQuota> mQuotas;
    long sizeToAllocate;

    HashMap<String, List<AllocationItem>> mItems;


    public Allocator(HashMap<String, About.StorageQuota> quotas, long size) {
        mQuotas = quotas;
        sizeToAllocate = size;
    }

    public Allocator(HashMap<String, About.StorageQuota> quotas, HashMap<String, List<AllocationItem>> items) {
        mQuotas = quotas;
        mItems = items;
        sizeToAllocate = toSizeToAllocate(items);
    }

    public HashMap<String, Long> getAllocationResult(){

        checkRemoteFree();

        //make sure the size of free space is bigger than the slice size

        //

        //return new MethodFillFully().allocate(sizeUpload, mQuotas);
        return new MethodAverage().allocate(sizeToAllocate, mQuotas);
    }

    public HashMap<String, List<AllocationItem>> allocateItems(){
        HashMap<String, AllocationItem> items;

        checkRemoteFree();

        return new MethodAllocateItemsAverage().allocate(mItems, mQuotas);
    }

    /*
        Allocation strategy for determining the size:
        1. fill the free space of a drive
        2. Average
    */
    interface Method{

        HashMap<String, Long> allocate(long sizeUpload, HashMap<String, About.StorageQuota> quota);
    }

    interface IMethodAllocateItems{

        HashMap<String, List<AllocationItem>> allocate(HashMap<String, List<AllocationItem>> items, HashMap<String, About.StorageQuota> quota);
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

    class MethodAllocateItemsAverage implements IMethodAllocateItems{

        @Override
        public HashMap<String, List<AllocationItem>> allocate(HashMap<String, List<AllocationItem>> items, HashMap<String, About.StorageQuota> quota) {

//            Log.d(TAG, "items to allocate:");
//            items.entrySet().stream().forEach(set ->{
//                Log.d(TAG, "drive: " + set.getKey());
//                set.getValue().stream().forEach(item->{Log.d(TAG, item.getName());});
//            });

            //merge the lists
            List<AllocationItem> mergedList = items.values().stream().reduce((even,odd)->{
                //Log.d(TAG, "even:");
                //even.stream().forEach((item)->{Log.d(TAG, item.getName());});
                //Log.d(TAG, "odd:");
                //odd.stream().forEach((item)->{Log.d(TAG, item.getName());});
                List<AllocationItem> merged = new ArrayList<>();
                merged.addAll(even);
                merged.addAll(odd);
                return merged;}).get();

            Log.d(TAG, "Merged items:");
            mergedList.stream().forEach((item -> {Log.d(TAG, item.getName());}));

            HashMap<String, Long> remainingFree = Mapper.reValue(quota, (q)->{return q.getLimit() - q.getUsage();});
            //HashMap<String, Boolean> fullMarked = Mapper.reValue(quota, (q)->{return false;});
            List<String> listDrive = quota.keySet().stream().collect(Collectors.toList());

            ListIterator<String> driveListIterator = listDrive.listIterator();

            Log.d(TAG, "starting allocate...");
            HashMap<String, List<AllocationItem>> result = new HashMap<>();
            //Map<String, AllocationItem> map = mergedList.stream().map((item)->{
            mergedList.stream().forEach((item)->{
                final String[] sectedDrive = new String[1];

                //Log.d(TAG, "Item: " + item.getDrive() + ", " + item.getName());
                //get a drive that the remaining size is enough
                //A loop is employed. The loop is break in the two cases:
                //1. A drive which has enough space to store the item
                //2. The drive list is traversed, but no drive with enough big space is found
                boolean rollovered = false;
                while(true) {
                    //It looks ugly because we implemented a circular list. Unfortunately, there is no choice
                    //because I don't find a java implementation
                    if (!driveListIterator.hasNext()) {
                        Log.d(TAG, "set drive list to beginning...");
                        //leared from https://www.baeldung.com/java-reset-listiterator
                        //We are fine with the approach because the list supposes be short
                        while (driveListIterator.hasPrevious()) {
                            driveListIterator.previous();
                        }
                        rollovered = true;
                    }
                    sectedDrive[0] = driveListIterator.next();
                    if (remainingFree.get(sectedDrive[0]) > item.getSize()) {
                        Log.d(TAG, "target drive: " + sectedDrive[0] + "; allocated item: " + item.getName() + " seq: " + item.getSequence());
                        break;
                    } else if (rollovered == true) {
                        //The drive list has been traversed once, stop the allocation process
                        throw new com.crossdrives.cdfs.exception.ItemNotFoundException("no space can be used for allocation", new Throwable());
                    }
                }

                if (result.get(sectedDrive[0]) == null) { result.put(sectedDrive[0], new ArrayList<>());}
                result.get(sectedDrive[0]).add(item);

//                Map.Entry<String, AllocationItem> e = new Map.Entry<>() {
//                    @Override
//                    public String getKey() {
//                        return sectedDrive[0];
//                    }
//
//                    @Override
//                    public AllocationItem getValue() {
//                        return item;
//                    }
//
//                    @Override
//                    public AllocationItem setValue(AllocationItem o) {
//                        return null;
//                    }
//                };
//                return e;
            });//.collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

            //remap to the format we like to provide
            //Log.d(TAG, "remap to the format we like to provide.");
//            HashMap<String, List<AllocationItem>> map2 =
//            com.crossdrives.cdfs.util.map.Mapper.toList(map);
//            Log.d(TAG, "map2: " + map2.toString());

            return result;

//
//
//            Mapper.reValue(new HashMap<>(map), (k, v)->{
//                List<Map.Entry<String, AllocationItem>> entryList = map.entrySet().stream().filter((set)->{
//                    return set.getKey() == k;
//                }).collect(Collectors.toList());
//
//                List<AllocationItem> itemList =
//                entryList.stream().map((entry)->{
//                    return entry.getValue();
//                }).collect(Collectors.toList());
//                return itemList;
//            });
        }
    }

    /*
        Check whether the total available size of remote drives is bigger than the one of items to allocated.
     */
    private void checkRemoteFree(){
        final long remoteFree;

        remoteFree = totalRemoteFree(mQuotas);

        if(sizeToAllocate > remoteFree){
            Log.w(TAG, "available remote space is not enough for allocation");
            throw new CompletionException("available remote space is not enough for allocation", new Throwable(""));
        }
    }

    private long totalRemoteFree(HashMap<String, About.StorageQuota> quota){
        return quota.values().stream().mapToLong((q)->
                {return (q.getLimit()- q.getUsage());})
                .sum();
    }

    //Calculate the size to allocate
    //Input: allocation items in drives
    //Output: size in byte
    long toSizeToAllocate(HashMap<String, List<AllocationItem>> items) {
        long sum =
                //map to Long stream
                items.values().stream().map((list -> {
                    return list.stream().map((ai) -> {
                        return ai.getSize();
                    });
                })).reduce((prev, curr) -> {
                    //merge streams
                    return Stream.concat(prev, curr);
                }).get().reduce(new Long(0), (even, odd) -> {
                    //do the summing
                    return even + odd;
                });

        return sum;
    }
}
