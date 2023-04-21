package com.crossdrives.cdfs.util;

import com.crossdrives.cdfs.data.Drive;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
        Deal with the following three cases:
        1. A folder is created with signed in drive(s) and one or some of the drives are signed out.
        2. A folder is created with signed in drive(s) and one or some of the drives are signed in additionally.
     */

public class ApplicableDriveBuilder {
    public static ConcurrentHashMap<String, Drive> build(ConcurrentHashMap<String, Drive> signedDrives, ConcurrentHashMap<String, List<String>> itemIdList){
        Map<String, Drive> map = signedDrives.entrySet().stream().filter((set)->{
                    return itemIdList.keySet().stream().anyMatch((k)-> set.getKey().equals(k));
                }).map(set->{
                    Map.Entry<String, Drive> entry = new Map.Entry<String, Drive>() {
                        @Override
                        public String getKey() {
                            return set.getKey();
                        }

                        @Override
                        public Drive getValue() {
                            return set.getValue();
                        }

                        @Override
                        public Drive setValue(Drive value) {
                            return null;
                        }
                    };

                    return entry;})
                .collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

        return new ConcurrentHashMap(map);
    }
}
