package com.crossdrives.base;

import java.util.Date;
import java.util.List;

public class BaseItem {
    String name;

    //CDFS ID
    String id;

    //CDFS display path which contains the item. Or the parents. e.g. AAA\BBB\CCC
    String path;

    //Cdfs IDs of a item. The order is top most to bottom.
    List<String> parents;

    //Indicator whether the item is a folder(parent) or not
    boolean folder;

    Long size;

    String timeCreated;

    //last modified time
    String timeModified;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public List<String> getParents() {
        return parents;
    }

    public boolean isFolder() {
        return folder;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public void setTimeCreated(String t) {
        timeCreated = t;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(String timeModified) {
        this.timeModified = timeModified;
    }
}
