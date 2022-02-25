package com.crossdrives.cdfs.model;

public class AllocationItem {
    /*
        Brand of user drive
    */
    private String drive;
    /*
        The segment number of the item. The number must be smaller than mTotalSeg.
     */
    private int sequence;
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
    private String name;
    /*
        Size of a allocation item in byte
     */
    private long size;

    /*
        Size of a CDFS item in byte
     */
    private long CDFSItemSize;

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

    public String getDrive(){return this.drive;}
    public void setDrive(String drive){this.drive = drive;}

    public void setSequence(int seq){
        sequence = seq;}
    public int getSequence(){return sequence;}

    public void setTotalSeg(int total){totalSeg = total;}
    public int getTotalSeg(){return totalSeg;}

    public void setPath(String path){this.path = path;}
    public String getPath(){return this.path;}

    public void setName(String name){this.name = name;}
    public String getName(){return this.name;}

    public void setSize(long size){this.size = size;}
    public long getSize(){return this.size;}

    public void setCDFSItemSize(long size){this.CDFSItemSize = size;}
    public long getCDFSItemSize(){return this.CDFSItemSize;}

}
