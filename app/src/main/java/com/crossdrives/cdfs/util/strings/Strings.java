package com.crossdrives.cdfs.util.strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Strings {
    Collection<String> mStrings;

    public Strings(Collection<String> strings) {
        mStrings = strings;
    }
    public Collection<String> contains(Collection<String> strings){
        return mStrings.stream().filter((key)->{
            return strings.contains(key);
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public Collection<String> notContains(Collection<String> strings){
        return mStrings.stream().filter((key)->{
            return !strings.contains(key);
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
