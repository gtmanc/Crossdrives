package com.crossdrives.cdfs.util.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

//This is solution to handle the issue that Java Collectors.toMap throws a NullPointerException if one of the values is null
//From: https://stackoverflow.com/questions/24630963/nullpointerexception-in-collectors-tomap-with-null-entry-values/32648397#32648397
public class Collectors {
    public static <T, K, U>
    Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                     Function<? super T, ? extends U> valueMapper) {
        return java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toList(),
                list -> {
                    Map<K, U> result = new HashMap<>();
                    for (T item : list) {
                        K key = keyMapper.apply(item);
                        if (result.putIfAbsent(key, valueMapper.apply(item)) != null) {
                            throw new IllegalStateException(String.format("Duplicate key %s", key));
                        }
                    }
                    return result;
                });
    }
}
