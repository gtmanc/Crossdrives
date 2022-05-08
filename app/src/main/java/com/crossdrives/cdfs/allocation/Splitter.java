package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;

import com.crossdrives.msgraph.SnippetApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Splitter {
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    File file;
    String baseName = "splitted";
    Integer chunkCount = 0;
    byte[] bf = new byte[1024];
    ISplitProgressCallback mCallback;
    FileInputStream fIn = null;
    final int chunkSize = 10 * 1000 * 1000; //10 Mega Byes

    public Splitter(File file, List<Long> lengths) {
        this.file = file;
    }

    public void split(ISplitProgressCallback callback){
        Context context = SnippetApp.getAppContext();
        mCallback = callback;
        String name = baseName+ chunkCount.toString();
        String ex = null;

        try {
            fIn = context.openFileInput(file.getPath());

        } catch (FileNotFoundException e) {
            ex = e.getMessage();
        } catch (IOException e) {
            ex = e.getMessage();
        }

        if(ex == null) {

            //sExecutor.submit(()->{
            long remaining = file.length();
            while (remaining > 0) {
                FileOutputStream fOut = null;
                int rd_len = 0;
                try {
                    fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
                    rd_len = chunkCopy(fIn, fOut);
                } catch (FileNotFoundException e) {
                    ex = e.getMessage();
                    break;
                } catch (IOException e) {
                    ex = e.getMessage();
                    break;
                }
                remaining -= rd_len;
                chunkCount++;
                mCallback.progress(new File(context.getDataDir() + name));
            }
            //});
        }

        if(ex != null){
            mCallback.onFailure(ex);
        }
    }

    private int chunkCopy(FileInputStream fIn, FileOutputStream fOut) throws IOException {
        int remaining = chunkSize;
        int rd_len = 0;
        int totalCopied = 0;

        if(fIn != null && fOut!= null){
            while (remaining > 0) {
                rd_len = bf.length;
                if (remaining < bf.length) {
                    rd_len = (int)remaining;
                }
                rd_len = fIn.read(bf, 0, rd_len);
                fOut.write(bf);
                remaining -= rd_len;
                totalCopied += rd_len;
            };
        }

        return totalCopied;
    }
}
