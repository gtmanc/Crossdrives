package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;

import com.crossdrives.msgraph.SnippetApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Splitter {
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    File file;
    String baseName = "splitted";
    Integer chunkCount = 0;
    byte[] bf = new byte[1024];
    ISplitProgressCallback mCallback;
    FileInputStream fIn = null;
    final int chunkSize = 10 * 1000 * 1000; //10 Mega Byes
    HashMap<String, Long> Allocation;

    public Splitter(File file, HashMap<String, Long> allocation) {
        this.file = file;
        Allocation = allocation;
    }

    public void split(ISplitProgressCallback callback){
        Context context = SnippetApp.getAppContext();
        mCallback = callback;
        String name = baseName+ chunkCount.toString();
        AtomicReference<String> ex = null;

        try {
            fIn = context.openFileInput(file.getPath());

        } catch (FileNotFoundException e) {
            ex.set(e.getMessage());
        } catch (IOException e) {
            ex.set(e.getMessage());
        }

        if(ex.get() == null) {

            //sExecutor.submit(()->{
            Allocation.entrySet().forEach((entry)->{
                long remaining = entry.getValue();
                mCallback.start(entry.getKey(), remaining);
                while (remaining > 0) {
                    FileOutputStream fOut = null;
                    int rd_len = 0;
                    try {
                        fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
                        rd_len = chunkCopy(fIn, fOut);
                    } catch (FileNotFoundException e) {
                        ex.set(e.getMessage());
                        break;
                    } catch (IOException e) {
                        ex.set(e.getMessage());
                        break;
                    }
                    remaining -= rd_len;
                    chunkCount++;
                    mCallback.progress(new File(context.getDataDir() + name));
                    context.deleteFile(name);
                }
                mCallback.finish(entry.getKey(), remaining);
            });
            //});
        }

        if(ex.get() != null) {
            mCallback.onFailure(ex.get());
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
