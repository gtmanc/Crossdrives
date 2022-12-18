package com.crossdrives.cdfs.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CdfsItem {
    //CDFS display name
    String name;

    //CDFS ID
    String Id;

    //CDFS display path. Or the parents. e.g. AAA\BBB\CCC
    String path;

    //Indicator whether the item is a folder(parent) or not
    boolean folder;

    //drive item IDs in each user's drive
    ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();

    public String getName() {
        return name;
    }

    public String getId() {
        return Id;
    }

    public String getPath() {
        return path;
    }

    public ConcurrentHashMap<String, List<String>> getMap() {
        return map;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        Id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMap(ConcurrentHashMap<String, List<String>> map) {
        this.map = map;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }
}
