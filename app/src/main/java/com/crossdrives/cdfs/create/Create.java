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
import com.crossdrives.cdfs.exception.ItemNotFoundException;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
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
            parents: list of involved parents in CdfsItem. An empty list indicates root.
    */
    public Create(CDFS cdfs, String name, @NonNull List<CdfsItem> parents) {
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
        String pathParent = whereWeAre == null ? IConstant.CDFS_PATH_BASE : whereWeAre.getPath();

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

                //HashMap<String, List<String>> parentIDLists = new HashMap<>();
                //Create the folder in the remote target folder. The parent ID list is also built up
                //in this step.
                mCDFS.getDrives().keySet().stream().forEach((driveName)->{
                    //Get drive id of base folder
                    CompletableFuture<com.google.api.services.drive.model.File> baseFolderIdFuture = mapFetcher.getBaseFolder(driveName);
                    com.google.api.services.drive.model.File file = baseFolderIdFuture.join();
                    throwExIfNull(file,"Base folder is missing!","");
                    Log.d(TAG, "Base folder ID got: " + file.getId());
                    List<String> list = buildSingleParentDriveIdList(driveName, mParents, file.getId());
                    //parentIDLists.put(driveName, list);
                    createRemoteFolderFutures.put(driveName, createFolderRemote(driveName, mName, list));
                });

                //Wait until the folders are created. Build the drive item id list that we will used to
                //generate the CDFS Id in the later step. The new created folder drive ID is also appended
                // to tje parent ID list that we will used in lter step.
                Collection<String> idListConcantenated = new ArrayList<>();
                HashMap<String, String> idNewCreatedItems = new HashMap<>();
                Mapper.reValue(createRemoteFolderFutures, future->{
                    return future.join().getId();
                }).entrySet().stream().forEach((entry)->{
                    String driveName = entry.getKey();
                    String id = entry.getValue();
                    Log.d(TAG, "New folder ID: " + id);
                    idListConcantenated.add(id);
                    //parentIDLists.get(driveName).add(id);
                    idNewCreatedItems.put(driveName, id);
                });
                Log.d(TAG, "Size of ID list built up: " + idListConcantenated.size());

                //Just map to allocation container so that we can easily process later
                HashMap<String, AllocContainer> mapContainer = Mapper.reValue(mapFile, (stream)->{
                    throwExIfNull(stream,"Map file is missing!","");
                    return AllocManager.toContainer(stream);
                });

                //Add the new allocation item with attribute folder to the container
                final String cdfsId = IDProducer.deriveID(idListConcantenated);
                HashMap<String, AllocContainer> modified = Mapper.reValue(mapContainer, (k, v)->{
                    String driveName = k;
                    AllocContainer container = v;
                    AllocationItem item = AllocManager.createItemFolder(driveName, mName, pathParent, cdfsId, idNewCreatedItems.get(k));
                    container.addItem(item);
                    return container;
                });

                //update the map files
                Log.d(TAG, "Update map file in parent...");
                MapUpdater updater = new MapUpdater(mCDFS.getDrives());
                CompletableFuture<HashMap<String, com.google.api.services.drive.model.File>> updateFuture
                        = updater.updateAll(modified, whereWeAre);
                updateFuture.join();
                Log.d(TAG, "Parent map file updated.");

                //Create a default local allocation file and store(upload) it to the folder just created
                LocalFileCreator lfc = new LocalFileCreator(SnippetApp.getAppContext());
                AllocManager am = new AllocManager(mCDFS);
                final java.io.File defaultMapLocal = lfc.create(Names.allocFile(cdfsId), am.newAllocation());
                //json = lfc.read(Names.allocFile(cdfsId));

                //Prepare the metaData for uploader
                HashMap<String, File> defaultMapFiles= new HashMap<>();
                mCDFS.getDrives().keySet().forEach((key)->{
                    File cdfsMetaData = new File();
                    com.google.api.services.drive.model.File googleMetaData = new com.google.api.services.drive.model.File();
                    googleMetaData.setName(defaultMapLocal.getName());
                    List<String> singleParentList = new ArrayList<>();
                    singleParentList.add(idNewCreatedItems.get(key));
                    Log.d(TAG, "Parent for upload default map: " + singleParentList.get(0));
                    googleMetaData.setParents(singleParentList);
//                    Log.d(TAG, "Parents for upload default map:");
//                    parentIDLists.get(key).stream().forEach((id)->{
//                        Log.d(TAG, id);
//                    });
                    cdfsMetaData.setOriginalLocalFile(defaultMapLocal);
                    cdfsMetaData.setFile(googleMetaData);
                    defaultMapFiles.put(key, cdfsMetaData);
                    Log.d(TAG, "Default local map files:" + defaultMapFiles);
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

    CompletableFuture<com.google.api.services.drive.model.File> createFolderRemote(String driveName, String name, @NonNull List<String> driveIdLists){
        CompletableFuture<com.google.api.services.drive.model.File> future = new CompletableFuture<>();
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(MINETYPE_FOLDER);
        fileMetadata.setParents(driveIdLists);
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

    /*
    */
    List<String> buildSingleParentDriveIdList(@NonNull String driveName, @NonNull List<CdfsItem> items, @NonNull String baseFolderId){
        List<String> idList = new ArrayList<>();

        if(items.size() == 0) {
            idList.add(0, baseFolderId);    //base folder.
        }else{
            long indexSecondToLast = items.stream().count()-2;
            CdfsItem foundItem = items.stream().skip(indexSecondToLast).findFirst().get();
            //Assume the item must be a folder. Directly get the first element
            idList.add(foundItem.getMap().get(driveName).get(0));
        }

        return idList;
    }

    <T> void throwExIfNull(T t, String message, String cause) throws ItemNotFoundException {
        if(t == null) {
            throw new ItemNotFoundException(message, new Throwable(cause));
        }
    }

    /*
        Obsolete.
        A helper function for building up the drive ID list for a specified drive.
        This is only required for Google drive. Microsoft uses path instead.
        Input:
            driveName:
            items: item list in CDFSItem. Can not be null. An empty list indicates CDFS root.
        Return:
            Drive ID list of the specified drive that ID of the base folder is inserted at head.
            An empty list is returned if root is specified.
     */
    List<String> buildMultipleParentDriveIdList(@NonNull String driveName, @NonNull List<CdfsItem> items, @NonNull String baseFolderId){
        List<String> idList = new ArrayList<>();

        idList.add(0, baseFolderId);    //insert the base folder id at the head to build up a complete drive ID list.

        if(items.size() == 0){return idList;}

        items.stream().forEachOrdered(item->{
            idList.add(item.getMap().get(driveName).get(0));
        });



        return idList;
    }
}
