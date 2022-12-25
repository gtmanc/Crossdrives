package com.crossdrives.cdfs.download;

import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.Compositor;
import com.crossdrives.cdfs.allocation.ICompositeCallback;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.model.MediaData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Download {
    static final String TAG = "CD.Download";
    CDFS mCDFS;
    String mFileID;
    CdfsItem mParent;
    final int MAX_CHUNK = 10;
    final int POISON_PILL = 0;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    /*
        Progress states
     */
    public enum State{
        GET_REMOTE_MAP_STARTED,
        GET_REMOTE_MAP_COMPLETE,
        MEDIA_IN_PROGRESS,
        MEDIA_COMPLETE,
    }

    Download.State mState;
    IDownloadProgressListener mListener;
    int progressTotalSegment = 0;
    int progressTotalDownloaded = 0;

    public Download(CDFS mCDFS, String fileID, CdfsItem parent, IDownloadProgressListener listener) {
        this.mCDFS = mCDFS;
        mFileID = fileID;
        mParent = parent;
        mListener = listener;
    }

    public Task<String> execute(){
        Task task;

        task = Tasks.call(mExecutor, new Callable<String>() {

            @Override
            public String call() throws Exception {
                final String[] result = new String[1];
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());

                callback(State.GET_REMOTE_MAP_STARTED);
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(mParent);
                HashMap<String, OutputStream> maps = mapsFuture.join();

                Log.d(TAG, "map fetched");
                callback(State.GET_REMOTE_MAP_COMPLETE);
                Compositor compositor = new Compositor(maps, mFileID, MAX_CHUNK);

                ArrayBlockingQueue<AllocationItem> toDownloadQ = new ArrayBlockingQueue<>(MAX_CHUNK);
                AllocationItem itemToDownload = new AllocationItem();

                Collection<Throwable> exceptions = new ArrayList<>();
                compositor.run(new ICompositeCallback() {
                    @Override
                    public void onStart(int totalSlice) {
                        progressTotalSegment = totalSlice;
                        callback(State.GET_REMOTE_MAP_COMPLETE);
                    }

                    @Override
                        public void onSliceRequested(String driveName, String id, int seq) {
                            Log.d(TAG, "slice requested: drive: " + driveName + " seq: " + seq);
                            AllocationItem ai = new AllocationItem();
                            ai.setDrive(driveName);
                            ai.setItemId(id);
                            ai.setSequence(seq);
                            toDownloadQ.add(ai);
                        }

                        @Override
                        public void onSliceCompleted(String driveName, int seq) {

                        }

                        @Override
                        public void onCompleted(String CompositedFile) {
                            Log.d(TAG, "composition done: " + CompositedFile);
                            addPoisonPill(toDownloadQ);
                            result[0] = CompositedFile;
                        }

                        @Override
                        public void OnExceptionally(Throwable throwable) {
                            Log.w(TAG, "composition failed!");
                            addPoisonPill(toDownloadQ);
                            exceptions.add(throwable);
                        }
                    });

                while(true){
                    itemToDownload = takeFromQueue(toDownloadQ);   //will block if no elements exists
                    Log.d(TAG, "Seq of item from Q: " + itemToDownload.getSequence());
                    //exit the loop if any of error occurred
                    if(itemToDownload.getSequence() == POISON_PILL){
                        Log.d(TAG, "Poison pill got. exit the loop.");
                        break;
                    }
                    if(!exceptions.isEmpty()){
                        throw new CompletionException("",exceptions.stream().findAny().get());
                    }

                    CompletableFuture<MediaData> sliceFuture = download(itemToDownload.getDrive(), itemToDownload.getItemId(), itemToDownload.getSequence());

                    sliceFuture.thenAccept((mediaData)->{
                        try {
                            Log.w(TAG, "filling content. Seq: " + mediaData.getAdditionInt());
                            compositor.fillSliceContent(mediaData.getAdditionInt(), mediaData.getOs());
                            progressTotalDownloaded++;
                            callback(State.MEDIA_IN_PROGRESS);
                        } catch (Throwable e) {
                            Log.w(TAG, "fill slice content failed! " + e.getMessage());
                            exceptions.add(e);
                        }
                    }).exceptionally(e->{
                        Log.w(TAG, "download slice failed! " + e.getMessage());
                        exceptions.add(e);
                        return null;});
                };

                Log.d(TAG, "Download process finished. Composited file: " + result[0] );
                callback(State.MEDIA_COMPLETE);
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
                Log.d(TAG, "download finished. Seq: " + mediaData.getAdditionInt());
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

    AllocationItem takeFromQueue(ArrayBlockingQueue<AllocationItem> q) throws InterruptedException {
        AllocationItem item = null;

        item = q.take();

        return item;
    }

    void addPoisonPill(ArrayBlockingQueue<AllocationItem> q){
        AllocationItem ai = new AllocationItem();
        //We use total segment and sequence number zero as a poison pill
        ai.setTotalSeg(POISON_PILL);
        ai.setSequence(POISON_PILL);
        q.add(ai);
    }

    void callback(Download.State state){
        mState = state;
        mListener.progressChanged(this);
    }

    public Download.State getState(){return mState;}

    public int getProgressMax(){return progressTotalSegment;}

    public int getProgressCurrent(){return progressTotalDownloaded;}
}
