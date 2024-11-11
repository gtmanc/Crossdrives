package com.crossdrives.cdfs.util.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Mapper {
    public static <K, V> HashMap<K, Collection<V>> toColletion(Map<K, V> map){
        HashMap<K, Collection<V>> result = new HashMap<>();

        map.entrySet().stream().forEach((set)->{
            if(result.get(set.getKey()) == null) {
                result.put(set.getKey(), new ArrayList<V>());
            }
            result.get(set.getKey()).add(set.getValue());
        });

        return result;
    }

    public static <K, V> HashMap<K, List<V>> toList(Map<K, V> map){
        HashMap<K, List<V>> result = new HashMap<>();

        map.entrySet().stream().forEach((set)->{
            if(result.get(set.getKey()) == null) {
                result.put(set.getKey(), new ArrayList<V>());
            }
            result.get(set.getKey()).add(set.getValue());
        });

        return result;
    }
}
