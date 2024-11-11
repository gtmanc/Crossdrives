package com.crossdrives.cdfs.allocation.util;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.print.Printer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Mapper {
    static public HashMap<String, Collection<AllocationItem>> toHashMap(Collection<AllocationItem> items){
        HashMap<String, Collection<AllocationItem>> map = new HashMap<>();

        items.stream().forEach((item)->{
            String key = item.getDrive();
            if(map.get(key) == null){ map.put(key, new ArrayList<>());}
            map.get(key).add(item);
        });

        return map;
    }
}
