package com.crossdrives.cdfs.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mapper<V> {


    public static <T, R> HashMap<String,R> reValue(HashMap<String,T> input, Function<? super T, ? super R> function){
        Objects.requireNonNull(function);
        Map<String, R> remapped =
        input.entrySet().stream().map((set)->{
            Map.Entry<String, R> entry = new Map.Entry<String, R>() {
                @Override
                public String getKey() {
                    return set.getKey();
                }

                @Override
                public R getValue() {
                    return (R) function.apply(set.getValue());
                }

                @Override
                public R setValue(R value) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

        return new HashMap<>(remapped);
    };

    public static <T,R> HashMap<String,R> reValue(HashMap<String,T> input, BiFunction<String, ? super T, ? super R> bifunction){
        Objects.requireNonNull(bifunction);
        Map<String, R> remapped =
                input.entrySet().stream().map((set)->{
                    Map.Entry<String, R> entry = new Map.Entry<String, R>() {
                        @Override
                        public String getKey() {
                            return set.getKey();
                        }

                        @Override
                        public R getValue() {
                            return (R) bifunction.apply(set.getKey(), set.getValue());
                        }

                        @Override
                        public R setValue(R value) {
                            return null;
                        }
                    };
                    return entry;
                }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));

        return new HashMap<>(remapped);
    };

}
