package com.crossdrives.cdfs.util;

import java.util.Map;

public class EntryCreator {

    public static <T1, T2> Map.Entry toMapEntry(T1 key, T2 value){
        Map.Entry entry = new Map.Entry() {
            @Override
            public T1 getKey() {
                return null;
            }

            @Override
            public T2 getValue() {
                return value;
            }

            @Override
            public Object setValue(Object o) {
                return null;
            }
        };
        return entry;
    }
}
