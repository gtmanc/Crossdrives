package com.crossdrives.cdfs.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CDFSItem {
    String mDriveName;
    String mPath;
    String mName;
    ConcurrentHashMap<String, AllocationItem> map = new ConcurrentHashMap<>();
    //List<AllocationItem> mList = new ArrayList<>();
}
