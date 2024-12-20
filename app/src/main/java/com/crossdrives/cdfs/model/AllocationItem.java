package com.crossdrives.cdfs.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class AllocationItem {
    static public final int SEQ_INITIAL = 1;// sequence number starts with 1

    private enum PropertyName{
        DRIVE("drive"),
        SEQ("seq"),
        TOT_SEQ("tot_seq"),
        CDFS_ID("cdfs_id"),
        ITEM_ID("id"),
        NAME("name"),
        PATH("path"),
        SIZE("size"),
        CDFS_SIZE("cdfs_size"),
        folder("folder"),
        NAME_RAW_CONTENT("name_raw_content");

        private final String prop;

        PropertyName(String prop) {
            this.prop = prop;
        }
    }

    /*
        Item name shown in CDFS
     */
    private String name;

    /*
        Item path shown in CDFS
     */
    private String path;

    /*
        Brand of user drive
    */
    private String drive;

    /*
        file id assigned by user's drive
    */
    private String cdfsId;

    /*
        file id assigned by user's drive
    */
    private String itemId;

    /*
        The segment number of the item. The number must be smaller than mTotalSeg.
     */
    private int sequence;
    /*
        Number of segment of the CDFS item
     */
    private int totalSeg;

    /*
        Size of a allocation item in byte
     */
    private long size;

    /*
        Size of a CDFS item in byte
     */
    private long CDFSItemSize;

    /*
        Attribute folder. Indicates whether this item is a folder or not.
     */
    private boolean folder;

    private String nameRawContent;


    private String createdDateTime;


    private String lastModifiedDateTime;

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

    public void setAttrFolder(boolean isFolder){this.folder = isFolder;}
    public boolean getAttrFolder(){return this.folder;}

    public String getCdfsId() {
        return cdfsId;
    }

    public void setCdfsId(String cdfsId) {
        this.cdfsId = cdfsId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getNameRawContent() {
        return nameRawContent;
    }

    public void setNameRawContent(String nameRawContent) {
        this.nameRawContent = nameRawContent;
    }

    public String getCreatedTime() {
        return createdDateTime;
    }

    public String getLastModifiedTime() {
        return lastModifiedDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public void setLastModifiedDateTime(String lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime;
    }

    public Collection<String> toPropertyNames(){
        Iterator<PropertyName> iterator = Arrays.stream(PropertyName.values()).iterator();
        Collection<String> out = new ArrayList<>();
        while(iterator.hasNext()){
            out.add(iterator.next().name());
        }
        return out;
    }

    public AllocationItem clone(AllocationItem item){
        AllocationItem ai = new AllocationItem();
        ai.name = item.getName();
        ai.path = item.getPath();
        ai.drive = item.getDrive();
        ai.cdfsId = item.getCdfsId();
        ai.itemId = item.getItemId();
        ai.sequence = item.getSequence();
        ai.totalSeg = item.getTotalSeg();
        ai.size = item.getSize();
        ai.CDFSItemSize = item.getCDFSItemSize();
        ai.folder = item.getAttrFolder();
        return ai;
    }
}
