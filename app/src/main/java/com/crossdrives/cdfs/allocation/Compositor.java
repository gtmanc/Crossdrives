package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;
import android.icu.text.Edits;
import android.util.Log;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Delay;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;
import com.crossdrives.msgraph.SnippetApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<Map.Entry<String, AllocationItem>> toDownload = mergeMapsThenSort(filtered);
        //Do a check to ensure there is nothing wrong in merge and sort we did.
        if(!checkExist(toDownload, mfileID)){
            Log.d(TAG, "file to composite doesn't exit in the map files");
            callback.OnExceptionally(new Throwable("Compositor: file to composite doesn't exit in the map files"));
            return 0;
        }
        Context context = SnippetApp.getAppContext();
        //Simply get name from any of the items because they are supposed to be the same
        String name = toDownload.get(0).getValue().getName();
        compositeOut = context.openFileOutput(name, Activity.MODE_PRIVATE);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(()->{
            int seq = AllocationItem.SEQ_INITIAL;
            int i = 0;
            String compositedFile = "context.getFilesDir().getPath() + \"/\" + name";
            while(i < toDownload.size()){
                String driveName = toDownload.get(i).getKey();
                String id = toDownload.get(i).getValue().getItemId();
                long size = toDownload.get(i).getValue().getSize();
                //make sure we don't exceed the allowed chunk size.
                if(toComposite.size() <= (mMaxChunkSize)){
                    callback.onSliceRequested(driveName, id, seq);
                }

                OutputStream slice = toComposite.get(i);
                if(slice != null){
                    Log.d(TAG, "Composite slice: index in queue:" + i);
                    try {
                        composite(slice, size);
                    } catch (IOException e) {
                        Log.w(TAG, "Composite failed!" + e.getMessage());
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                    callback.onSliceCompleted(driveName, seq);
                    toComposite.remove(i);
                    seq++;
                    i++;

                    try {
                        slice.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Close output stream failed!" + e.getMessage());
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                }

                Delay.delay(1000);
            }

            callback.onCompleted(compositedFile);
            return compositedFile;
        });

        future.exceptionally((e)->{
            Log.w(TAG, "Exceptionally!" + e.getMessage());
            e.printStackTrace();
            callback.OnExceptionally(e);
            return null;
        });

        compositeOut.close();

        return toDownload.get(0).getValue().getCDFSItemSize();
    }


    /*
        fill media content (slice of file) in outputStream. Usually this should be called on a different thread
     */
    public boolean fillSliceContent(int seq, OutputStream mediaStream) throws Throwable {
        boolean result = true;
        int i = seq - AllocationItem.SEQ_INITIAL;
        if(toComposite.get(i) != null){
            Log.w(TAG, "duplicated seq found");
            throw new Throwable("Compositor: duplicated seq found when filling content to compositor");
        }

        toComposite.put(seq, mediaStream);
        return result;
    }

    //HashMap<String, Stream<AllocationItem>> filter(HashMap<String, OutputStream> maps, String id){
    HashMap<String, Collection<AllocationItem>> filter(HashMap<String, OutputStream> maps, String id){
        //HashMap<String, Stream<AllocationItem>> itemStream = Mapper.reValue(maps, (stream)->{
        HashMap<String, Collection<AllocationItem>> itemStream = Mapper.reValue(maps, (stream)->{
            AllocContainer container = AllocManager.toContainer(stream);

            //The CDFS item exists?
//            if(container.getAllocItem().stream().filter((item)->
//            {return item.getCdfsId()==mFileID;}).count() == 0){
//                throw new CompletionException("Item not found in the map!", new Throwable(""));
//            }

            return container.getAllocItem().stream().filter((item)->
                    //Log.d(TAG, "")
            {return item.getCdfsId().equals(mfileID);}).collect(Collectors.toCollection(ArrayList::new));
        });

        StreamHandler.closeOutputStream(maps);
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
        printList(result);

        return result;
    }

    //
    boolean checkExist(Collection<Map.Entry<String, AllocationItem>> items, String fileID){
        boolean result = true;
        //The CDFS item exists?
        if(items.stream().filter((entry)->
        {return entry.getValue().getCdfsId().equals(fileID);}).count() == 0){
            result = false;
        }

        return result;
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
