package com.crossdrives.cdfs.exception;

import android.util.Log;

import java.util.concurrent.CompletionException;

public class GeneralHandling {

    static public void throwCompletionEx(Exception e, String message){
        e.printStackTrace();
        throw new CompletionException(e);
    }
}
