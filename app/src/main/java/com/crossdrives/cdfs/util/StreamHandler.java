package com.crossdrives.cdfs.util;

import com.crossdrives.cdfs.exception.GeneralHandling;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class StreamHandler {
    public static boolean closeOutputStream(OutputStream os){
        boolean result = true;
        try {
            os.close();
        } catch (IOException e) {
            result = false;
            GeneralHandling.throwCompletionEx(e, "Exception occurred when closing the output stream");
        }
        return result;
    }

    public static void closeOutputStreamAll(HashMap<String, OutputStream> streams){
        streams.forEach((k, v)->{
            try {
                v.close();
            } catch (IOException e) {
                GeneralHandling.throwCompletionEx(e, "Exception occurred when closing the output stream. " +
                        "The stream is mapped to drive: " + k);
            }
        });
    }
}
