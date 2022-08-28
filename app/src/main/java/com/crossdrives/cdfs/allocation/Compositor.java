package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Delay;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;
import com.crossdrives.msgraph.SnippetApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Compositor {
    static String TAG = "CD.Compositor";
    HashMap<String, OutputStream> maps;
    String mfileID;

    ConcurrentHashMap<Integer, OutputStream> toComposite = new ConcurrentHashMap<>();
    final int BUF_SIZE = 1024;
    long compositeOffset = 0;
    OutputStream compositeOut;
    int mMaxChunkSize;

    /*
        maps:
            the map files corresponding the folder that the item exists
        fileID:
            the ID of the item to download
        maxChunkBuffered:
            the allowed maximum number of the local slice of the item can be stored
            during compositing.
     */
    public Compositor(HashMap<String, OutputStream> maps, String fileID, int maxChunkBuffered) {
        this.maps = maps;
        mfileID = fileID;
        mMaxChunkSize = maxChunkBuffered;
    }

    public long run(ICompositeCallback callback) throws IOException {
        /*
            filter out the map items which we are not interested
         */
        HashMap<String, Collection<AllocationItem>> filtered = filter(maps, mfileID);
//        filtered.entrySet().stream().forEach(set->{
//            Log.d(TAG, "Items matched. In drive " + set.getKey());
//            set.getValue().forEach(v->Log.d(TAG, " seq: " + v.getSequence()));
//        });
        List<Map.Entry<String, AllocationItem>> toRequest = mergeMapsThenSort(filtered);
        //Do checks to ensure the fetched map files are valid
        if(!checkExist(toRequest, mfileID)){
            Log.d(TAG, "file to composite doesn't exit in the map files! Abort...");
            callback.OnExceptionally(new Throwable("Compositor: file to composite doesn't exit in the map files"));
            return 0;
        }
        if(checkCount(toRequest) < 0){
            Log.d(TAG, "The could be missing map items! Abort...");
            callback.OnExceptionally(new Throwable("Compositor: The could be missing map items!"));
            return 0;
        }

        Context context = SnippetApp.getAppContext();

        //Simply get name from any of the items because they are supposed to be the same
        String name = toRequest.get(0).getValue().getName();
        //String compositedFile = context.getFilesDir().getPath() + "\\" + name;
        //String compositedFile = context.getExternalFilesDir(null) + "\\" + name;
        String compositedFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + name;
        //compositeOut = context.openFileOutput(name, Activity.MODE_PRIVATE);
        compositeOut = new FileOutputStream(new File(compositedFile));

        callback.onStart(toRequest.size());
        CompletableFuture<String> future = CompletableFuture.supplyAsync(()->{
            int seq = AllocationItem.SEQ_INITIAL;
            int reqIndex=0, compositeIndex = 0, totalSegment = toRequest.size();

            while(compositeIndex < totalSegment){
                /*
                    Request slice whenever there is slice is needed. But we have to mke sure
                    we don't request too many slice at the same time because will take too much resource.
                */
                if(reqIndex < totalSegment && toComposite.size() <= (mMaxChunkSize)){
                    String driveName = toRequest.get(reqIndex).getKey();
                    String id = toRequest.get(reqIndex).getValue().getItemId();
                    callback.onSliceRequested(driveName, id, seq);
                    reqIndex++;
                    seq++;
                }

                OutputStream slice = toComposite.get(compositeIndex);
                if(slice != null){
                    Log.d(TAG, "Composite slice. queue index: " + compositeIndex);
                    String driveName = toRequest.get(compositeIndex).getKey();
                    long size = toRequest.get(compositeIndex).getValue().getSize();
                    try {
                        composite(slice, size);
                    } catch (IOException e) {
                        Log.w(TAG, "Composite failed!" + e.getMessage());
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                    callback.onSliceCompleted(driveName, seq);
                    //toComposite.remove(compositeIndex);
                    compositeIndex++;

                    if(!StreamHandler.closeOutputStream(slice)){Log.w(TAG, "Close output stream (slice) failed!");}
                }

                Delay.delay(1000);
            }

            if(!StreamHandler.closeOutputStream(compositeOut)){Log.w(TAG, "Close output stream (compositeOut) failed!");}

            callback.onCompleted(compositedFile);
            return compositedFile;

        });

        future.exceptionally((e)->{
            Log.w(TAG, "Exceptionally!" + e.getMessage());
            e.printStackTrace();
            callback.OnExceptionally(e);
            return null;
        });



        return toRequest.get(0).getValue().getCDFSItemSize();
    }


    /*
        fill media content (slice of file) in outputStream. Usually this should be called on a different thread
     */
    public boolean fillSliceContent(int seq, OutputStream mediaStream) throws Throwable {
        boolean result = true;
        int i = seq - AllocationItem.SEQ_INITIAL;
        if(toComposite.get(i) != null){
            Log.w(TAG, "duplicated seq found! Input Seq: " + seq);
            throw new Throwable("Compositor: duplicated seq found when filling content to compositor. Input Seq: " + seq);
        }

        Log.d(TAG, "Put slice. Seq: " + seq);
        toComposite.put(i, mediaStream);
        return result;
    }

    //
    HashMap<String, Collection<AllocationItem>> filter(HashMap<String, OutputStream> maps, String id){

        HashMap<String, Collection<AllocationItem>> itemStream = Mapper.reValue(maps, (stream)->{
            AllocContainer container = AllocManager.toContainer(stream);

            return container.getAllocItem().stream().filter((item)->
                    //Log.d(TAG, "")
            {return item.getCdfsId().equals(id);}).collect(Collectors.toCollection(ArrayList::new));
        });

        StreamHandler.closeOutputStreamAll(maps);
        return itemStream;
    }

    List<Map.Entry<String, AllocationItem>> mergeMapsThenSort(HashMap<String, Collection<AllocationItem>> items){
        final List result;
        final Collection<Map.Entry<String, AllocationItem>> merged = new ArrayList<>();
        Collection<Map.Entry<String, AllocationItem>> sorted = new ArrayList<>();

        //Merge
        items.forEach((k, v)->{
            //Stream<Map<String, AllocationItem>> s =
            Collection<Map.Entry<String, AllocationItem>> s =
                    v.stream().map((item)->{
                        //Log.d(TAG, "item not yet mapped: " + item.getName() + " " + item.getSequence());
                        Map.Entry<String, AllocationItem> entry = new Map.Entry<String, AllocationItem>() {
                            @Override
                            public String getKey() {
                                return k;
                            }

                            @Override
                            public AllocationItem getValue() {
                                return item;
                            }

                            @Override
                            public AllocationItem setValue(AllocationItem value) {
                                return null;
                            }
                        };
                        Log.d(TAG, "entry mapped: drive: " + entry.getKey() + " Name: " +
                                entry.getValue().getName() + " Seq: " + entry.getValue().getSequence());
                        return entry;
                    }).collect(Collectors.toCollection(ArrayList::new));
            merged.addAll(s);
        });

        merged.forEach((entry)->Log.d(TAG, "Merged item: " + entry.getValue().getSequence()));
        sorted = merged.stream().sorted((entry1, entry2)->{
            int v1 = entry1.getValue().getSequence();
            int v2 = entry2.getValue().getSequence();
            return Integer.compare(v1, v2);
        }).collect(Collectors.toCollection(ArrayList::new));

        sorted.forEach((entry)->Log.d(TAG, "Sorted item: " + entry.getValue().getSequence()));
        result = sorted.stream().collect(Collectors.toList());
        //printList(result);

        return result;
    }

    // The collection must contain at least a item which the CDFS ID matches up
    boolean checkExist(Collection<Map.Entry<String, AllocationItem>> items, String fileID){
        boolean result = true;
        //The CDFS item exists?
        if(items.stream().filter((entry)->
        {return entry.getValue().getCdfsId().equals(fileID);}).count() == 0){
            result = false;
        }

        return result;
    }

    // The number of items in the collection must equal to totalSeg
    int checkCount(Collection<Map.Entry<String, AllocationItem>> items){
        int count = 0;
        Optional<Map.Entry<String, AllocationItem>> optional = items.stream().findAny();
        int totalSeg = 0;

        //if collection is empty, exit with error.
        if(!optional.isPresent()){
            return count;
        }

        totalSeg = optional.get().getValue().getTotalSeg();

        count = totalSeg;
        if(items.stream().count() != totalSeg ){
            count = 0;
        }

        return count;
    }

    boolean composite(OutputStream os, long length) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        int rd_len, offset = 0;

        ByteArrayOutputStream bos = (ByteArrayOutputStream)os;
        ByteArrayInputStream bis = new ByteArrayInputStream( bos.toByteArray());



        while ( (rd_len = bis.read(buf)) >= 0){
            compositeOut.write(buf, 0, rd_len);
            offset += rd_len;
        }

        compositeOffset += offset;

        bis.close();
        bos.close();
        os.close();

        return true;
    }

    void printList(List<Map.Entry<String, AllocationItem>> list){
        Iterator<Map.Entry<String, AllocationItem>> iterator = list.iterator();
        while(iterator.hasNext()){
            Map.Entry<String, AllocationItem> entry = iterator.next();
            String key = entry.getKey();
            AllocationItem value = entry.getValue();
            Log.d(TAG, "Item: " + key + " " + value.getSequence());
        }
    }
}
