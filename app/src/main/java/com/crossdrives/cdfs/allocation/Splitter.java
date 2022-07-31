package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.crossdrives.msgraph.SnippetApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Splitter {
    final String TAG = "CD.Splitter";
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    Integer chunkCount = 0;
    byte[] bf = new byte[1024];
    InputStream fIn = null;
    final int chunkSize = 10 * 1024 * 1024; //10 Mega Byes
    HashMap<String, Long> Allocation;
    String sliceName;
    int maxChunkSize;
    ArrayBlockingQueue<File> chunkQueue;
    long mAllocatedSize;

    public Splitter(InputStream ins, HashMap<String, Long> allocation, String sliceName) {
        fIn = ins;
        Allocation = allocation;
        this.sliceName = sliceName;
    }

    public Splitter(InputStream ins, HashMap<String, Long> allocation, String sliceName, int maxChunkSize) {
        fIn = ins;
        Allocation = allocation;
        this.sliceName = sliceName;
        this.maxChunkSize = maxChunkSize;
        chunkQueue = new ArrayBlockingQueue<>(maxChunkSize);
    }

    public Splitter(InputStream ins, long size, String sliceName, int maxChunkSize) {
        fIn = ins;
        mAllocatedSize = size;
        this.sliceName = sliceName;
        this.maxChunkSize = maxChunkSize;
        chunkQueue = new ArrayBlockingQueue<>(maxChunkSize);
    }

    public void splitAll(ISplitAllCallback callback){
        Context context = SnippetApp.getAppContext();

        CompletableFuture<Integer> thread = CompletableFuture.supplyAsync(()->{
            Allocation.entrySet().forEach((entry)->{
                String driveName = entry.getKey();
                long remaining = entry.getValue();
                File file = null;
                callback.start(driveName, remaining);
                while (remaining > 0) {
                    FileOutputStream fOut = null;
                    String name = sliceName + "_" + chunkCount.toString();
                    int rd_len = 0;
                    try {
                        fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
                        rd_len = chunkCopy(fIn, fOut, remaining);
                    } catch (FileNotFoundException e) {
                        callback.onFailurePerDrive(driveName, e.getMessage());
                        break;
                    } catch (IOException e) {
                        callback.onFailurePerDrive(driveName, e.getMessage());
                        break;
                    }
                    //Log.d(TAG, "Read length: " + rd_len + "remaining: " + remaining);
                    remaining -= rd_len;
                    chunkCount++;
                    file = new File(context.getFilesDir().getPath() + "/" + name);
                    BlockingAdd(file);
                    callback.progress(driveName, file, rd_len);
                }
                callback.finish(driveName, remaining);
            });
            callback.completedAll();
            return 0;
        });
        thread.exceptionally(ex->{
            Log.w(TAG, "failed: " + ex.getMessage());
            ex.printStackTrace();
            callback.onFailure(ex.getMessage());
            return null;
        });


    }

    public void split(ISplitCallback callback) {
        Context context = SnippetApp.getAppContext();
        //mSplitCallback = callback;
        AtomicReference<String> ex = new AtomicReference<>();

        //TODO: use CompletableFuture instead of submission of executor. Run time exception is suppressed
        //in such a thread.
        sExecutor.submit(()->{
                long remaining = mAllocatedSize;
                File file = null;
            callback.start(remaining);
                while (remaining > 0) {
                    FileOutputStream fOut = null;
                    String name = sliceName + "_" + chunkCount.toString();
                    int rd_len = 0;
                    try {
                        fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
                        rd_len = chunkCopy(fIn, fOut, remaining);
                    } catch (FileNotFoundException e) {
                        ex.set(e.getMessage());
                        break;
                    } catch (IOException e) {
                        ex.set(e.getMessage());
                        break;
                    }
                    //Log.d(TAG, "Read length: " + rd_len + "remaining: " + remaining);
                    remaining -= rd_len;
                    chunkCount++;
                    file = new File(context.getFilesDir().getPath() + "/" + name);
                    BlockingAdd(file);
                    callback.progress(file, rd_len);
                }
            callback.finish(remaining);
        });

        if(ex.get() != null) {
            callback.onFailure(ex.get());
        }
    }

    public void cleanup(Collection<File> toDelete){
        Context context = SnippetApp.getAppContext();


        toDelete.stream().forEach((item)-> {
            Log.d(TAG, "delete item: " + item.getPath());
            context.deleteFile(item.getName());
        });

        remove(toDelete);
    }

    void BlockingAdd(File file){
        if(chunkQueue != null){
            chunkQueue.add(file);
        }
    }

    void remove(Collection<File> file){
        boolean result;
        if(chunkQueue != null){
            if(chunkQueue.size() == 0) {
                Log.w(TAG, "Something wrong. Chunk queue size is empty before removal!");
            }
            result = chunkQueue.removeAll(file);
            if(!result) {Log.w(TAG, "The item to deleted doesn't exist!");}
        }
    }

    File BlockingTake() throws InterruptedException {
        File file = null;
        if(chunkQueue != null){
            file = chunkQueue.take();
        }
        return file;
    }

    private int chunkCopy(InputStream fIn, FileOutputStream fOut, long upload_len) throws IOException {
        long remaining = chunkSize;
        int rd_len = 0;
        int totalCopied = 0;

        if(upload_len < chunkSize) {
            remaining = upload_len;
        }

        if(fIn != null && fOut!= null){
            while (remaining > 0 && rd_len != -1) {
                rd_len = bf.length;
                if (remaining < bf.length) {
                    rd_len = (int)remaining;
                }
                rd_len = fIn.read(bf, 0, rd_len);
//                Log.d(TAG, "Read length: " + rd_len + "remaining: " + remaining
//                + "totalCopied: " + totalCopied);
                fOut.write(bf);
                remaining -= rd_len;
                totalCopied += rd_len;
            };
        }

        //Log.d(TAG, "Chunk copy. Read length: " + rd_len + "remaining: " + remaining);

        return totalCopied;
    }
}
