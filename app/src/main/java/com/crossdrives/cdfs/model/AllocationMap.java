package com.crossdrives.cdfs.model;

public class AllocationMap {
    int Version;
    AllocationItem Items;

    public int getVersion(){ return this.Version;}
    public void setVersion(int version){this.Version = version;};

    public AllocationItem getAllocItem(){return Items;}
    public void setAllocItem(AllocationItem item){this.Items = item;}
}
