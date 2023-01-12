package com.crossdrives.cdfs.create;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.IDProducer;
import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.create.ICreateCallBack;
import com.crossdrives.driveclient.model.File;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Create {
    static final String TAG = "CD.Create";

    private final String MINETYPE_FOLDER = IConstant.MIMETYPE_FOLDER;

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    CDFS mCDFS;
    final String mName;     //name of the item to create
    //CDFS parents which contains the item. If not specified, the item will be placed directly in root.
    //The list describes the topology of the parents. e.g. the last is folder where we are
    List<CdfsItem> mParent;



    public Create(CDFS mCDFS, String name, List<CdfsItem> parents) {
        this.mCDFS = mCDFS;
        this.mName = name;
        this.mParent = parents;
    }

    public Create setFeature(){
        //Currently only creating folder is implemented.
        return this;
    }

    public Task<File> execute(){
        Task<com.crossdrives.driveclient.model.File> task;
        CdfsItem whereWeAre = mParent.isEmpty()? createRootCdfsItem() : mParent.get(mParent.size()-1);

        task = Tasks.call(mExecutor, new Callable<File>() {
            @Override
            public File call() throws Exception {
                File result = new File();
                HashMap<String, CompletableFuture<com.google.api.services.drive.model.File>>
                        createRemoteFolderFutures = new HashMap<>();

                Log.d(TAG, "Fetch map...");
                //callback(Delete.State.GET_MAP_STARTED);
                MapFetcher mapFetcher = new MapFetcher(mCDFS.getDrives());
                CompletableFuture<HashMap<String, OutputStream>> mapsFuture = mapFetcher.pullAll(whereWeAre);
                HashMap<String, OutputStream> maps = mapsFuture.join();
                Log.d(TAG, "map fetched");
                //callback(Delete.State.GET_MAP_COMPLETE);

                //Create a folder in remote
                mCDFS.getDrives().keySet().stream().forEach((driveName)->{
                    createRemoteFolderFutures.put(driveName, createFolderRemote(driveName, mName, mParent));
                });

                //Wait until the folders are created. Build the drive item id list that we will used to
                //generate the CDFS Id in the later step.
                Collection<String> idList = new ArrayList<>();
                Mapper.reValue(createRemoteFolderFutures, future->{
                    return future.join().getId();
                }).values().stream().forEach((id)->{
                    idList.add(id);
                });
                //Create a default allocation file and store it to the folder just created

                //map to allocation container so that we can easily process later
                HashMap<String, AllocContainer> mapped = Mapper.reValue(maps, (stream)->{
                    return AllocManager.toContainer(stream);
                });

                //Add new allocation item with attribute folder
                HashMap<String, AllocContainer> modified = Mapper.reValue(mapped, (k, v)->{
                    String driveName = k;
                    AllocContainer container = v;
                    container.addItem(AllocManager.createItemFolder(driveName, mName, whereWeAre.getPath(),
                            IDProducer.deriveID(idList)));
                    return container;
                });

                MapUpdater updater = new MapUpdater(mCDFS.getDrives());

                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(modified, whereWeAre);
                updateFuture.join();

                //Prepare result before we exit
                com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
                file.setName(mName);
                result.setFile(file);
                return result;
            }
        });

        return task;
    }

    CompletableFuture<com.google.api.services.drive.model.File> createFolderRemote(String driveName, String name, List<CdfsItem> topologyParent){
        CompletableFuture<com.google.api.services.drive.model.File> future = new CompletableFuture<>();
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(MINETYPE_FOLDER);
        fileMetadata.setParents(buildParentDriveIdList(driveName, topologyParent));
        mCDFS.getClient(driveName).create().buildRequest(fileMetadata).run(new ICreateCallBack<com.google.api.services.drive.model.File>() {
            @Override
            public void success(com.google.api.services.drive.model.File file) {
                Log.d(TAG, "Create file OK. ID: " + file.getId());
                future.complete(file);
            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "Failed to create file: " + ex.toString());
                future.completeExceptionally(new Throwable(ex.toString()));
            }
        });
        return future;
    }

    CdfsItem createRootCdfsItem(){
        CdfsItem item = new CdfsItem();

        item.setPath("");
        return item;
    }

    /*
        This is only required for Google drive. Microsoft use path instead.
        An empty list is returned if root is specified.
     */
    List<String> buildParentDriveIdList(@NonNull String driveName, @Nullable List<CdfsItem> items){
        List<String> idList = new ArrayList<>();

        if(items.size() == 0){return idList;}

        items.stream().forEachOrdered(item->{
            idList.add(item.getMap().get(driveName).get(0));
        });
        return idList;
    }
}
