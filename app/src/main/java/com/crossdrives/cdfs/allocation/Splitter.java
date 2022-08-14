package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.crossdrives.msgraph.SnippetApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
                Collection<Exception> ex = new ArrayList<>();
                while (remaining > 0) {
                    FileOutputStream fOut = null;
                    String name = sliceName + "_" + chunkCount.toString();
                    int rd_len = 0;
                    try {
                        fOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
                        Log.d(TAG,"Write chunk. Name: " + name);
                        rd_len = chunkCopy(fIn, fOut, remaining);
                    } catch ( IOException e) {
                        ex.add(e);
                        break;
                    }
                    //Log.d(TAG, "Read length: " + rd_len + "remaining: " + remaining);
                    try {
                        fOut.close();
                    } catch (IOException e) {
                        ex.add(e);
                        break;
                    }
                    remaining -= rd_len;
                    chunkCount++;
                    file = new File(context.getFilesDir().getPath() + "/" + name);
                    BlockingAdd(file);
                    callback.progress(driveName, file, rd_len);
                }

                if(!ex.isEmpty()){
                    callback.onFailurePerDrive(driveName, ex.stream().findAny().get().getMessage());
                }else{
                    callback.finish(driveName, remaining);
                }
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
        Collection<Exception> ex = new ArrayList<>();

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
                        Log.d(TAG,"Write chunk. Name: " + name);
                        rd_len = chunkCopy(fIn, fOut, remaining);
                    } catch (IOException e) {
                        ex.add(e);
                        break;
                    }
                    //Log.d(TAG, "Read length: " + rd_len + "remaining: " + remaining);
                    try {
                        fOut.close();
                    } catch (IOException e) {
                        ex.add(e);
                        break;
                    }
                    remaining -= rd_len;
                    chunkCount++;
                    file = new File(context.getFilesDir().getPath() + "/" + name);
                    BlockingAdd(file);
                    callback.progress(file, rd_len);
                }

                if(!ex.isEmpty()){
                    callback.onFailure(ex.stream().findAny().get().getMessage());
                }else{
                    callback.finish(remaining);
                }
        });

    }

    public void cleanup(Collection<File> toDelete){
        Context context = SnippetApp.getAppContext();

        toDelete.stream().forEach((item)-> {
            boolean result;
            Log.d(TAG, "delete slice: " + item.getPath());
            result = context.deleteFile(item.getName());
            if(!result){Log.w(TAG, "delete slice failed!");}

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
            if(!result) {
                Log.w(TAG, "The items to remove dont exist! The items: ");
                file.forEach((item)->{
                    Log.w(TAG, item.getName());
                });

            }
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
        boolean firstTime = true;

        if(upload_len < chunkSize) {
            remaining = upload_len;
        }

        Log.d(TAG, "Chunk copy remaining: " + remaining);
        if(fIn != null && fOut!= null){
            while (remaining > 0 && rd_len != -1) {
                rd_len = bf.length;
                if (remaining < bf.length) {
                    rd_len = (int)remaining;
                    Log.d(TAG, "Copy the tail... len: " + rd_len);
                }
                rd_len = fIn.read(bf, 0, rd_len);
                if(firstTime){
                    Log.d(TAG, "First block at head: " + rd_len);
                    Log.d(TAG, "Available len: " + fIn.available());
                    //for(int i = 0 ; i < 10 ; i ++){Log.d(TAG, " " + bf[i]);}
                    Log.d(TAG, "data: " + new String(bf));
                    firstTime = false;
                }
                if (remaining < bf.length) {
                    Log.d(TAG, "last block at tail: " + rd_len);
                    //for(int i = 0 ; i < 10 ; i ++){Log.d(TAG, " " + bf[i]);}
                    Log.d(TAG, "data: " + new String(bf));
                }
                fOut.write(bf);
                remaining -= rd_len;
                totalCopied += rd_len;
            };
        }

        //Log.d(TAG, "Chunk copy. Read length: " + rd_len + "remaining: " + remaining);

        return totalCopied;
    }
}
