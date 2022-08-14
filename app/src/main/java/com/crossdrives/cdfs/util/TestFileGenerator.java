package com.crossdrives.cdfs.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crossdrives.msgraph.SnippetApp;
import com.google.common.primitives.Chars;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TestFileGenerator {
    final String TAG = "CD.TestFileGenerator";
    String name;
    long maxSeq;
    final int CHUNK_SIZE = 64;

    /*
        maxSeq      Outcome
        128*1024    KB
        1024*1024   6.93MB
        8*1024*1024 62.9
    */
    public TestFileGenerator(String name, long maxSeq) {
        this.name = name;
        this.maxSeq = maxSeq;
    }

    public FileInputStream run() throws IOException {
        Context context = SnippetApp.getAppContext();
        //String file = context.getFilesDir().getPath() + "\\" + name;
        int seq = 0;
        FileOutputStream out = context.openFileOutput(name, Activity.MODE_PRIVATE);
        OutputStreamWriter writer = new OutputStreamWriter(out);

        char[] hi = new char[]{'h', 'i'};

        while(seq < maxSeq) {
            String concatenated = "";
            for(int i = 0; i < CHUNK_SIZE; i++) {
                String tmp;
                tmp = Integer.toString(seq + i);
                //Log.d(TAG, "tmp[0]: " + tmp[0]);
                concatenated = concatenated.concat(tmp);
                concatenated = concatenated.concat(" ");
            }
//            Log.d(TAG, "Write Chunk. Produced:" + seq +
//                    " 1st char: " + concatenated.charAt(0));
            writer.write(concatenated.toCharArray(), 0, concatenated.length());
            seq += CHUNK_SIZE;
        }

        writer.close();
        FileInputStream in = context.openFileInput(name);
        return in;
    }

}
