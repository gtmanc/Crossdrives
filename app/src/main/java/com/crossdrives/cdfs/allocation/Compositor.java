package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.StreamHandler;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compositor {
    HashMap<String, OutputStream> maps;
    String mfileID;

    public Compositor(HashMap<String, OutputStream> maps, String fileID, int maxChunkBuffered) {
        this.maps = maps;
        mfileID = fileID;
    }

    public long run(ICompositeCallback callback){
        HashMap<String, Stream<AllocationItem>> filtered = filter(maps, mfileID);
        Collection<Map.Entry<String, AllocationItem>> sliceList = mergeMapsThenSort(filtered);
        if(checkExist(sliceList, mfileID)){
            callback.OnExceptionally(new Throwable("file to composite doesn't exit in the map files"));
        }


    }


    public boolean fillSliceContent(int seq, OutputStream content){

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

    Collection<Map.Entry<String, AllocationItem>> mergeMapsThenSort(HashMap<String, Stream<AllocationItem>> items){
        final Collection result;
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

        result = merged[0].collect(Collectors.toCollection(ArrayList::new));
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
}
