package com.crossdrives.cdfs.util.print;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;

import java.util.Collection;
import java.util.HashMap;

public class Printer {
    String TAG;
    Container container;
    AllocationItem allocationItem;

    public Printer(String tag) {
        TAG = tag;
    }

    public AllocationItem getAllocationItem(){
        if(allocationItem == null) {
            allocationItem = new AllocationItem();}
        return allocationItem;
    }

    public Container getContainer(){
        if(container == null) {
            container = new Container();}
        return container;
    }

    public class Container{
        public void out(String head, HashMap<String, AllocContainer> container, String tail){
            printIfAvailable(head);
            container.entrySet().stream().forEach((set)->{
                Log.d(TAG, "drive: " + set.getKey());
//                set.getValue().getAllocItem().stream().forEach((item)->{
//                    Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
//                });
                out(null, set.getValue(), null);
            });
            printIfAvailable(tail);
        }

        public void out(String head, AllocContainer container, String tail){
            printIfAvailable(head);
            container.getAllocItem().stream().forEach((item)->{
                Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
            });
            printIfAvailable(tail);
        }
    }


    public class AllocationItem{
        public void out(String head, com.crossdrives.cdfs.model.AllocationItem ai, String tail){
            printIfAvailable(head);
            Log.d(TAG, "cdfs name: " + ai.getName());
            Log.d(TAG, "id: " + ai.getItemId());
            Log.d(TAG, "seq: " + ai.getSequence());
            Log.d(TAG, "TotSeg: " + ai.getTotalSeg());
            Log.d(TAG, "Path: " + ai.getPath());
            printIfAvailable(tail);
        }

        public void out(String head, HashMap<String, Collection<com.crossdrives.cdfs.model.AllocationItem>> ai, String tail){
            printIfAvailable(head);
            ai.entrySet().stream().forEach((set)->{
                Log.d(TAG, "drive: " + set.getKey());
//                set.getValue().getAllocItem().stream().forEach((item)->{
//                    Log.d(TAG, "name: " + item.getName() + " seq: " + item.getSequence() + " TotSeq: " + item.getTotalSeg() + " parent: " + item.getPath());
//                });
                out(null, set.getValue(), null);
            });
            printIfAvailable(tail);
        }

        public void out(String head, Collection<com.crossdrives.cdfs.model.AllocationItem> ai, String tail){
            printIfAvailable(head);
            ai.stream().forEach((item)->{
                out(null, item, null);
            });
            printIfAvailable(tail);
        }
    }

    private void printIfAvailable(String head) {
        if (head != null) {
            Log.d(TAG, head);
        }
    }
}
