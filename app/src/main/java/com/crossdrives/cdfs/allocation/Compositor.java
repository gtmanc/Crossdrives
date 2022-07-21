package com.crossdrives.cdfs.allocation;

import android.app.Activity;
import android.content.Context;

import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Delay;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;
import com.crossdrives.msgraph.SnippetApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compositor {
    static String TAG = "CD.Compositor";
    HashMap<String, OutputStream> maps;
    String mfileID;
    List<OutputStream> toComposite = new ArrayList<>();
    final int BUF_SIZE = 1024;
    long compositeOffset = 0;
    OutputStream compositeOut;

    public Compositor(HashMap<String, OutputStream> maps, String fileID, int maxChunkBuffered) {
        this.maps = maps;
        mfileID = fileID;
    }

    public long run(ICompositeCallback callback) throws IOException {
        HashMap<String, Stream<AllocationItem>> filtered = filter(maps, mfileID);
        List<Map.Entry<String, AllocationItem>> toDownload = mergeMapsThenSort(filtered);
        Context context = SnippetApp.getAppContext();
        String name = toDownload.get(0).getValue().getName();
        if(checkExist(toDownload, mfileID)){
            callback.OnExceptionally(new Throwable("Compositor: file to composite doesn't exit in the map files"));
        }

        compositeOut = context.openFileOutput(name, Activity.MODE_PRIVATE);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(()->{
            int seq = 0;
            while(seq < toDownload.size()){
                String driveName = toDownload.get(seq).getKey();
                String id = toDownload.get(seq).getValue().getItemId();
                long size = toDownload.get(seq).getValue().getSize();
                callback.onSliceRequested(driveName, id, seq);

                OutputStream slice = toComposite.get(seq);
                if(slice != null){
                    try {
                        composite(slice, size);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    seq++;

                    try {
                        slice.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Delay.delay(1000);
            }

            callback.onComplete(context.getFilesDir().getPath() + "/" + name);
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
        if(toComposite.get(seq) != null){
            throw new Throwable("Compositor: duplicated seq found when filling content to compositior");
        }

        toComposite.add(seq, mediaStream);
        return result;
    }

    HashMap<String, Stream<AllocationItem>> filter(HashMap<String, OutputStream> maps, String id){
        HashMap<String, Stream<AllocationItem>> itemStream = Mapper.reValue(maps, (stream)->{
            AllocContainer container = AllocManager.toContainer(stream);

            //The CDFS item exists?
//            if(container.getAllocItem().stream().filter((item)->
//            {return item.getCdfsId()==mFileID;}).count() == 0){
//                throw new CompletionException("Item not found in the map!", new Throwable(""));
//            }

            return container.getAllocItem().stream().filter((item)->
            {return item.getCdfsId()==mfileID;});
        });

        StreamHandler.closeOutputStream(maps);
        return itemStream;
    }

    List<Map.Entry<String, AllocationItem>> mergeMapsThenSort(HashMap<String, Stream<AllocationItem>> items){
        final List result;
        final Stream<Map.Entry<String, AllocationItem>>[] merged = new Stream[]{Stream.empty()};

        //remap
        items.forEach((k, v)->{
            //Stream<Map<String, AllocationItem>> s =
            Stream s =
                    v.map((item)->{
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
                        return entry;
                    }).collect(Collectors.toCollection(ArrayList::new)).stream();
            merged[0] = Stream.concat(merged[0], s);
        });

        merged[0] = merged[0].sorted((entry1, entry2)->{
            int v1 = entry1.getValue().getSequence();
            int v2 = entry2.getValue().getSequence();
            return Integer.compare(v1, v2);
        });

        result = merged[0].collect(Collectors.toList());

        return result;
    }

    //
    boolean checkExist(Collection<Map.Entry<String, AllocationItem>> items, String fileID){
        boolean result = true;
        //The CDFS item exists?
        if(items.stream().filter((entry)->
        {return entry.getValue().getCdfsId()==fileID;}).count() == 0){
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
}
