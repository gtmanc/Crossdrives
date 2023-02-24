package com.crossdrives.cdfs.create;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.allocation.IDProducer;
import com.crossdrives.cdfs.allocation.Names;
import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.allocation.AllocManager;
import com.crossdrives.cdfs.allocation.MapFetcher;
import com.crossdrives.cdfs.allocation.MapUpdater;
import com.crossdrives.cdfs.data.LocalFileCreator;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.remote.uploader;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.create.ICreateCallBack;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.msgraph.SnippetApp;
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
    List<CdfsItem> mParents;


    /*
        Input:
            cdfs: CDFS instance
            name: name of the folder will be created
            parents: list of parents in terms of CdfsItem. An empty list indicates root.
    */
    public Create(CDFS cdfs, String name, List<CdfsItem> parents) {
        this.mCDFS = cdfs;
        this.mName = name;
        this.mParents = parents;
    }

    public Create setFeature(){
        //Currently only creating folder is implemented.
        return this;
    }

    public Task<File> execute(){
        Task<com.crossdrives.driveclient.model.File> task;
        CdfsItem whereWeAre = mParents.isEmpty()? null : mParents.get(mParents.size()-1);

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
                HashMap<String, OutputStream> mapFile = mapsFuture.join();
                Log.d(TAG, "map fetched");
                //callback(Delete.State.GET_MAP_COMPLETE);

                //Create the folder in the remote target folder
                mCDFS.getDrives().keySet().stream().forEach((driveName)->{
                    //Get drive id of base folder
                    CompletableFuture<com.google.api.services.drive.model.File> baseFolderIdFuture = mapFetcher.getBaseFolder(driveName);
                    com.google.api.services.drive.model.File file = baseFolderIdFuture.join();
                    Log.d(TAG, "Base folder ID got: " + file.getId());
                    createRemoteFolderFutures.put(driveName, createFolderRemote(driveName, mName, mParents, file.getId()));
                });

                //Wait until the folders are created. Build the drive item id list that we will used to
                //generate the CDFS Id in the later step.
                Collection<String> idList = new ArrayList<>();
                Mapper.reValue(createRemoteFolderFutures, future->{
                    return future.join().getId();
                }).values().stream().forEach((id)->{
                    idList.add(id);
                });

                //Just map to allocation container so that we can easily process later
                HashMap<String, AllocContainer> mapContainer = Mapper.reValue(mapFile, (stream)->{
                    return AllocManager.toContainer(stream);
                });

                //Add the new allocation item with attribute folder to the container
                final String cdfsId = IDProducer.deriveID(idList);
                HashMap<String, AllocContainer> modified = Mapper.reValue(mapContainer, (k, v)->{
                    String driveName = k;
                    AllocContainer container = v;
                    container.addItem(AllocManager.createItemFolder(driveName, mName, whereWeAre.getPath(), cdfsId));
                    return container;
                });

                //update the map files
                MapUpdater updater = new MapUpdater(mCDFS.getDrives());
                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(modified, whereWeAre);
                updateFuture.join();

                //Create a default local allocation file and store(upload) it to the folder just created
                LocalFileCreator lfc = new LocalFileCreator(SnippetApp.getAppContext());
                AllocManager am = new AllocManager(mCDFS);
                lfc.create(Names.allocFile(cdfsId), am.newAllocation());
                //json = lfc.read(Names.allocFile(cdfsId));

                HashMap<String, File> defaultMapFiles= new HashMap<>();
                mCDFS.getDrives().keySet().forEach((key)->{
                    defaultMapFiles.put(key, lfc.);
                });

                uploader uploader = new uploader(mCDFS.getDrives());
                uploader.uploadAll(defaultMapFiles);

                //Prepare result before we exit
                com.google.api.services.drive.model.File file = new com.google.api.services.drive.model.File();
                file.setName(mName);
                result.setFile(file);
                return result;
            }
        });

        return task;
    }

    CompletableFuture<com.google.api.services.drive.model.File> createFolderRemote(String driveName, String name, @NonNull List<CdfsItem> CdfsParents, String baseFolderId){
        CompletableFuture<com.google.api.services.drive.model.File> future = new CompletableFuture<>();
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        List<String> IdList = buildParentDriveIdList(driveName, CdfsParents);
        fileMetadata.setName(name);
        fileMetadata.setMimeType(MINETYPE_FOLDER);
        IdList.add(0, baseFolderId);    //insert the base folder id to the head because we are working on drive ID domain
        fileMetadata.setParents(IdList);
        mCDFS.getClient(driveName).create().buildRequest(fileMetadata).run(new ICreateCallBack<com.google.api.services.drive.model.File>() {
            @Override
            public void success(com.google.api.services.drive.model.File file) {
                Log.d(TAG, "Create OK. ID: " + file.getId());
                future.complete(file);
            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "Failed to create item: " + ex.toString());
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
        This is only required for Google drive. Microsoft uses path instead.
        Input:
            driveName:
            items: item list in CDFSItem. Can not be null.
        Return:
            if root is specified.
     */
    List<String> buildParentDriveIdList(@NonNull String driveName, @NonNull List<CdfsItem> items){
        List<String> idList = new ArrayList<>();

        if(items.size() == 0){return idList;}

        items.stream().forEachOrdered(item->{
            idList.add(item.getMap().get(driveName).get(0));
        });
        return idList;
    }
}
