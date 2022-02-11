package com.crossdrives.cdfs.model;

public class AllocationItem {
    /*
        Brand of user drive
    */
    private String brand;
    /*
        The segment number of the item. The number must be smaller than mTotalSeg.
     */
    private int seqNum;
    /*
        Number of segment of the CDFS item
     */
    private int totalSeg;

    /*
        Item path shown in CDFS
     */
    private String path;
    /*
        Item name shown in CDFS
     */
    private String mName;
    /*
        Size of the item in byte
     */
    private long size;

    public String create(){
        String s = null;

        return s;
    }

    public String update(){
        String s = null;

        return s;
    }

    public void delete(){
        String s = null;

    }

    public String getBrand(){return this.brand;}
    public void setBrand(String brand){this.brand = brand;}
}
