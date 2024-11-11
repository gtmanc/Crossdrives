package com.crossdrives.cdfs.util.collection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.model.AllocationItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mapper{

    public <T, R> Map<R, T> toMap(Collection<T> collection, Function<T, R> function){
        return collection.stream().map((element)->{
            Map.Entry<R, T> entry = new Map.Entry<R, T>() {
                @Override
                public R getKey() {
                    return function.apply(element);
                }

                @Override
                public T getValue() {
                    return element;
                }

                @Override
                public T setValue(T o) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

    }

    public static <T, R> Map<R, Collection<T>> toMapList(Collection<T> collection, Function<T, R> function){
        HashMap<R, Collection<T>> result = new HashMap<>();

        collection.stream().forEach((element)->{
            R r = function.apply(element);
            if(result.get(r) == null) {
                result.put(r, new ArrayList<T>());
            }
            result.get(r).add(element);
        });

        return result;
    }
}
