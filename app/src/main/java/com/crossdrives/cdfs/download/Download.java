package com.crossdrives.cdfs.download;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.Compositor;
import com.crossdrives.cdfs.allocation.ICompositeCallback;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.EntryCreator;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Download {
    static final String TAG = "CD.Download";
    CDFS mCDFS;
    String mFileID;
    String mParent;
    final int MAX_CHUNK = 10;

    public Download(CDFS mCDFS, String fileID, String parent) {
        this.mCDFS = mCDFS;
        mFileID = fileID;
        mParent = parent;
    }

    public Task<String> execute(){
        Task task;

        task = Tasks.call(new Callable<String>() {

            @Override
            public String call() throws Exception {
                final String[] result = new String[1];
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();

                Compositor compositor = new Compositor(maps, mFileID, MAX_CHUNK);

                final boolean[] isComposited = {false};
                ArrayBlockingQueue<AllocationItem> toDownloadQ = new ArrayBlockingQueue<>(MAX_CHUNK);
                AllocationItem itemToDownload = new AllocationItem();
                while(!isComposited[0]){
                    compositor.run(new ICompositeCallback() {
                        @Override
                        public void onSliceRequested(String driveName, String id, int seq) {
                            AllocationItem ai = new AllocationItem();
                            ai.setDrive(driveName);
                            ai.setItemId(id);
                            ai.setSequence(seq);
                            toDownloadQ.add(ai);
                        }

                        @Override
                        public void onComplete(String CompositedFile) {
                            isComposited[0] = true;
                            result[0] = CompositedFile;
                        }

                        @Override
                        public void OnExceptionally(Throwable throwable) {
                            isComposited[0] = true;
                        }
                    });

                    itemToDownload = takeFromQueue(toDownloadQ);   //will block if no elements exists
                    CompletableFuture<MediaData> sliceFuture = download(itemToDownload.getDrive(), itemToDownload.getItemId(), itemToDownload.getSequence());

                    sliceFuture.thenAccept((mediaData)->{
                        try {
                            compositor.fillSliceContent(mediaData.getAdditionInt(), mediaData.getOs());
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                };

                return result[0];
            }
        });
        return task;
    }

    CompletableFuture<MediaData> download(String driveName, String id, int seq){
        CompletableFuture<MediaData> future = new CompletableFuture<>();

        mCDFS.getDrives().get(driveName).getClient().download().buildRequest(id).setAdditionInt(seq)
        .run(new IDownloadCallBack<MediaData>() {
            //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
            @Override
            public void success(MediaData mediaData) {
                Log.d(TAG, "download finished.");
                future.complete(mediaData);

            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "download failed" + ex.toString());
                future.completeExceptionally(new Throwable(ex));
            }
        });
        return future;
    }

    AllocationItem takeFromQueue(ArrayBlockingQueue<AllocationItem> q){
        AllocationItem item = null;
        try {
            item = q.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return item;
    }
}
