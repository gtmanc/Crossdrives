package com.crossdrives.cdfs.util.collection;

import com.crossdrives.cdfs.model.AllocationItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Allocation {
    static final String TAG = "CD.Collection";

    public static boolean removeBySeq(java.util.Collection<AllocationItem> collection, Integer seq) {
        boolean result;
        AllocationItem found = collection.stream().filter((r)->
        {return (r.getSequence() == seq);}).findAny().get();
        result = collection.remove(found);

        return result;
    }

    public static boolean removeByName(LinkedBlockingQueue<File> queue, String name) {
        boolean result;
        List l = Arrays.asList(queue.toArray());
        java.util.Collection<File> collection = new ArrayList<File>(l);
        result = queue.remove(collection.stream().filter((e) -> {
//          Log.d(TAG, "Remaining queue item:" + e.getName());
//          Log.d(TAG, "local item:" + file.getOriginalLocalFile().getName());
            return e.getName().equals(name);
        }).findAny().get());
        return result;
    }
}
