package com.crossdrives.cdfs.model;

import java.util.ArrayList;
import java.util.List;

public class AllocContainer {
    int version;
    List<AllocationItem> items = new ArrayList<>();

    public int getVersion(){ return this.version;}
    public void setVersion(int version){this.version = version;};

    public List<AllocationItem> getAllocItem(){return items;}
    public void setAllocItem(AllocationItem item){this.items.add(item);}
    public void setAllocItem(List<AllocationItem> item){this.items = item;}
}