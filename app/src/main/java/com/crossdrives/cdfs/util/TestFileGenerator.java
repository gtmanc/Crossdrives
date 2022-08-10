package com.crossdrives.cdfs.util;

import android.app.Activity;
import android.content.Context;

import com.crossdrives.msgraph.SnippetApp;

import java.io.FileNotFoundException;
import java.io.OutputStream;

public class TestFileGenerator {
    String name;
    long maxLength;
    final int CHUNK_SIZE = 1024;


    public OutputStream run() throws FileNotFoundException {
        Context context = SnippetApp.getAppContext();
        //String file = context.getFilesDir().getPath() + "\\" + name;
        char[] buf = new char[CHUNK_SIZE];
        int prod_len = 0, offset = 0;
        OutputStream out = context.openFileOutput(name, Activity.MODE_PRIVATE);

        while(prod_len < maxLength) {
            for(int i = 0; i < CHUNK_SIZE; i++) buf[i] = Integer.toString(prod_len + i).charAt(0);
            out.write(buf, 0, CHUNK_SIZE);
            offset += CHUNK_SIZE;
        }
    }
}
