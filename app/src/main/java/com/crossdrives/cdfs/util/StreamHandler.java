package com.crossdrives.cdfs.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class StreamHandler {
    public static void closeOutputStream(HashMap<String, OutputStream> streams){
        streams.forEach((k, v)->{
            try {
                v.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
