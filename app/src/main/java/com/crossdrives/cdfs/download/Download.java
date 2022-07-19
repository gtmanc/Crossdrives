package com.crossdrives.cdfs.download;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Download {
    static private final String TAG = "CD.Downloadj";
    CDFS mCDFS;
    String mFileID;
    String mParent;

    public Download(CDFS mCDFS, String fileID, String parent) {
        this.mCDFS = mCDFS;
        mFileID = fileID;
        mParent = parent;
    }

    public Task<OutputStream> execute(){
        Task task;

        task = Tasks.call(new Callable<OutputStream>() {

            @Override
            public OutputStream call() throws Exception {
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();



                sortedItems.stream().forEach((entry)->{
                    CompletableFuture<OutputStream> downloadFuture;
                    downloadFuture = download(entry.getKey(), mFileID, entry.getValue().getSequence());

                });


                return null;
            }
        });


        return task;
    }




    CompletableFuture<OutputStream> download(String driveName, String id, int seq){
        CompletableFuture<OutputStream> future = new CompletableFuture<>();

        mCDFS.getDrives().get(driveName).getClient().download().buildRequest(id).setAdditionInt(seq)
        .run(new IDownloadCallBack<MediaData>() {
            //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
            @Override
            public void success(MediaData mediaData) {
                Log.d(TAG, "download finished.");
                future.complete(mediaData.getOs());

            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "download failed" + ex.toString());
                future.completeExceptionally(new Throwable(ex));
            }
        });
        return future;
    }
}
