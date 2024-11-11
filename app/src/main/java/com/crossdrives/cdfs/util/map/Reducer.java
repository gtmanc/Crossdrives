package com.crossdrives.cdfs.util.map;

import com.crossdrives.cdfs.data.Drive;

import java.util.Collection;
import java.util.HashMap;

public class Reducer <T> {

//    private HashMap<String, Drive> NameMatchedDrives(Collection<String> names){
//        return toKeyMatched(names, mCDFS.getDrives());
//    }
    HashMap<String, T> hmap;

    public Reducer(HashMap<String, T> map) {
        hmap = map;
    }

    public HashMap<String, T> toKeyMatched(Collection<String> keys){
        HashMap<String, T> out = new HashMap<>();
        keys.stream().forEach((key)->{
            if(hmap.get(key) != null){
                out.put(key, hmap.get(key));
            }
        });
        return out;
    }
}
