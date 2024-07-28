package com.crossdrives.cdfs.util.collection;

import com.example.crossdrives.AccountManager;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Diff {

    public static <T> List<T> between(List<T> l1, List<T> l2, BiFunction<? super T, ? super T, Boolean> differ){
        Stream<T> diff1, diff2;
        diff1 = l1.stream().filter((e1)->{
            return !(l2.stream().filter((e2)->{
                return differ.apply(e1, e2);
            }).count() > 0);
        });
        diff2 = l2.stream().filter((e1)->{
            return !(l1.stream().filter((e2)->{
                return differ.apply(e1, e2);
            }).count() > 0);
        });
        return Stream.concat(diff1, diff2).collect(Collectors.toList());
    }

}
