package com.crossdrives.cdfs.allocation;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.exception.ItemNotFoundException;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.cdfs.util.strings.Strings;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.crossdrives.driveclient.model.MediaData;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MapFetcher {
    private final String TAG = "CD.MapFetcher";
    HashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    /*
        Query strings
     */
    private final String NAME_CDFS_FOLDER = Names.CDFS_FOLDER;
    private final String PREFIX_ALLOCATION_FILE = Names.PREFIX_ALLOC_FILE;
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + Names.CDFS_FOLDER + "'";

    private HashMap<String, State> states = new HashMap<>();
    private HashMap<String, OutputStream> output = new HashMap<>();
    ICallBackMapFetch callback;

    /*
        State for fetch
     */
    class State{
        static public final int STATE_IDLE = 0;
        static public final int STATE_CHECK_FOLDER = 1;
        static public final int STATE_CHECK_FILE = 2;
        static public final int STATE_DOWNLOAD_FILE = 3;
        static public final int STATE_FINISHED = 4;

        int state = STATE_IDLE;

        public void setState(int state) { this.state = state;}
        public int getState() {return state;}

    }

    public MapFetcher(HashMap<String, Drive> drives) {
        super();

        mDrives = drives; }

    /*
        Currently input parent is not used. Therefore, we even have not yet decided whether a ID or path should be assigned.
        It is reserved for the change if the allocation map is changed to folder basis.
        Currently, it is drive basis. (all items are in a single allocation map)
    */
    public void fetchAll(@Nullable String parentName, ICallBackMapFetch<HashMap<String, OutputStream>> callback) {

        this.callback = callback;

        /*
            Create all states. We have to do this at once before we start the fetchs.
        */
        mDrives.forEach((key, value)->{
            State state = new State();
            states.put(key,state);
        });

        /*
            Start fetching one by one
        * */
        mDrives.forEach((name, drive)->{
            Log.d(TAG, "Start to fetch root allocation file. Drive: " + name);
            fetch(states.get(name), name, drive.getClient(), parentName);
        });

    }

    private void fetch(State state, String driveName, IDriveClient client, String parentName){
        /*
            Start with get CDFS folder
         */
        getFolder(state, driveName, client, parentName);
    }

    void getFolder(State state, String driveName, IDriveClient client, String parentName){
        String clause = "mimeType = '" + MINETYPE_FOLDER  +
                "' and name = ";

        if(parentName == null){
            clause = clause.concat("'" + NAME_CDFS_FOLDER + "'");
        }else{
            clause = clause.concat("'" + parentName + "'");
        }


        state.setState(State.STATE_CHECK_FOLDER);
        //states.put(name, state);
        client.list().buildRequest()
                //sClient.get(0).list().buildRequest()
                .setNextPage(null)  //TODO: #42
                .setPageSize(0) //0 means no page size is applied
                .filter(clause)
                //.filter("mimeType = 'application/vnd.google-apps.folder'")
                //.filter(null)   //null means no filter will be applied
                .run(new IFileListCallBack<FileList, Object>() {
                    @Override
                    public void success(FileList fileList, Object o) {
                        String id = null;

                        id = GetFolderId(fileList, parentName);
                        if(id != null) {
                            getFileID(state, driveName, client, id);
                        }
                        else{
                            //#48 TODO
                            callback.onCompletedExceptionally(new Throwable("CDFS folder is missing"));
                        }
                        //future.complete(result);
                    }

                    @Override
                    public void failure(String ex) {
                        callback.onCompletedExceptionally(new Throwable(ex));
                        //future.completeExceptionally(new Throwable(""));
                    }
                });

    }

    /*
        Find 1st item which name matches the specified
     */
    private String GetFolderId(FileList fileList, String name){
        String id = null;
//      for(int i = 0 ; i < fileList.getFiles().size(); i++){
//          fileList.getFiles().get(i).getName().compareToIgnoreCase("cdfs");
//      }
        if(fileList.getFiles().size() > 0) {
            if (fileList.getFiles().get(0).getName().compareToIgnoreCase(name) == 0) {
                id = fileList.getFiles().get(0).getId();
            }
            else {
                Log.w(TAG, "Folders are found. But no allocation file in cdfs folder!");
            }
        }
        else{
            Log.w(TAG, "No folder is found");
        }

        return id;
    }

    private void getFileID(State state, String name, IDriveClient client, String parentid){
        String query = "'" + parentid + "' in parents";

        state.setState(State.STATE_CHECK_FILE);
        //states.put(name, state);

        Log.d(TAG, "Check allocation file. Query:  " + query);
        client.list().buildRequest()
                //sClient.get(0).list().buildRequest()
                .setNextPage(null)
                .setPageSize(0) //0 means no page size is applied TODO #42
                //.filter("name = 'allocation.cdfs'" + "in 'CDFS'")
                .filter(query)
                //.filter(null)   //null means no filter will be applied
                .run(new IFileListCallBack<FileList, Object>() {
                    @Override
                    public void success(FileList fileList, Object o) {
                        String id = null;
                        Log.d(TAG, "Result of List got. Drive: " + name);
                        id = handleResultGetFile(fileList);
                        if(id != null) {
                            download(state, name, client, id);
                        }else{
                            callback.onCompletedExceptionally(new Throwable("Allocation file is missing"));
                        }
                        //future.complete(result);
                    }

                    @Override
                    public void failure(String ex) {
                        callback.onCompletedExceptionally(new Throwable(ex));
                        //future.completeExceptionally(new Throwable(""));
                    }
                });
    }

    private String handleResultGetFile(FileList fileList){
        AtomicReference<String> id = new AtomicReference<>();
        Optional<File> files = null;
//        if(fileList.getFiles().size() > 0) {
//            if (fileList.getFiles().get(0).getName().compareToIgnoreCase(NAME_ALLOCATION_ROOT) == 0) {
//                id.set(fileList.getFiles().get(0).getId());
//            } else {
//                Log.w(TAG, "Files are found. But no root allocation file in cdfs folder!");
//            }
//        }else{
//            Log.w(TAG, "No file is found in CDFS folder");
//        }

        if(fileList.getFiles().size() > 0) {Log.d(TAG, "Files found in CDFS folder.");}

        //#48 TODO
        files = fileList.getFiles().stream().filter((file)->{
            return file.getName().compareToIgnoreCase(Names.allocFile(null)) == 0 ?  true : false;  //root
        }).findAny();
        if(!files.isPresent()){Log.w(TAG, "No root allocation file presents!");}
        files.ifPresent((file) -> {
            Log.d(TAG, "Root allocation file presents.");
            id.set(file.getId());});
        return id.get();
    }

    private void download(State state, String name, IDriveClient client, String fileid){
        state.setState(State.STATE_DOWNLOAD_FILE);
        //states.put(name, state);

        Log.d(TAG, "Download allocation file. Query");
        client.download().buildRequest(fileid)
                .run(new IDownloadCallBack<MediaData>() {

                    @Override
                    public void success(MediaData mediaData) {
                        Log.d(TAG, "Download allocation file OK");
                        boolean joinResult;
                        output.put(name, mediaData.getOs());
                        //future.complete(result);
                        state.setState(State.STATE_FINISHED);
                        joinResult = joinResult();
                        if(joinResult == true) {
                            Log.d(TAG, "Join result got. Now call back");
                            callback.onCompleted(output);
                        }

                    }

                    @Override
                    public void failure(String ex) {
                        boolean joinResult;
                        Log.w(TAG, ex);
                        state.setState(State.STATE_FINISHED);
                        joinResult = joinResult();
                        if(joinResult == true) {
                            callback.onCompletedExceptionally(new Throwable(ex));
                        }
                        //future.completeExceptionally(new Throwable(""));
                    }
                });
    }

    private boolean joinResult(){
        AtomicBoolean result = new AtomicBoolean(true);
        Log.d(TAG, "size of states: " + states.size());

        states.forEach((name, state) -> {
            state = states.get(name);
            Log.d(TAG, "State: " + state.getState());
            if(state.getState() != State.STATE_FINISHED){
                result.set(false);
            }
        });

        return result.get();
    }

    /*
        Get metadata of map files which stored in user's drives
        Input:
            CDFS parent item. Directly set to root in the method if null is input.

        Return:
            A task which will return with map files. Note the map could be NULL if map file is not found
            in user's drive.
     */
    public CompletableFuture<HashMap<String, File>> listAll(@Nullable CdfsItem parent) {
        CompletableFuture<HashMap<String, File>> resultFuture;
        Fetcher fetcher = new Fetcher(mDrives);

        resultFuture = CompletableFuture.supplyAsync(()-> {
            HashMap<String, File> metaDataFolder;
            //CompletableFuture<HashMap<String, File>> foldersFuture = getFolderAll(parent);

            //if any is null. exit
            //Log.d(TAG, "Check CDFS folders... ");
            //baseFolders = foldersFuture.join();
            metaDataFolder = getMetaDataAll(parent);
            Log.d(TAG, "Parent mapped ID list: " + metaDataFolder);

            //something wrong if no mapped ID for the parent
            if(metaDataFolder.entrySet().stream().anyMatch(((set)-> set.getValue()== null))){
                Log.w(TAG, "CDFS folder is missing!");
                return null;
            }

            final CompletableFuture<HashMap<String, FileList>> listFuture = fetcher.listAll(metaDataFolder);
            final HashMap<String, FileList> fileList = listFuture.join();
            //final HashMap<String, FileList> fileListAtDest = getListAtDestination(parent, fileList, fetcher);
            HashMap<String, File> maps = Mapper.reValue(fileList, (key, list)->{
                File f = null;
                String id = parent.getName().equals(IConstant.CDFS_NAME_ROOT) ? null : parent.getId();
                if(list != null) {
                    f = getFromFiles(list, Names.allocFile(id));
                }

                if(f == null) {
                    //f = new File();
                    Log.w(TAG, "No map file found in the specified folder! Drive:" + key);
                }
                //throwExIfNull(f, "Map item is not found. Drive: " + key, "");
                return f;
            });

//            CompletableFuture<HashMap<String, FileList>> list = fetcher.listAll(baseFolders);
//            HashMap<String, File> maps = Mapper.reValue(list.join(), (fileList)->{
//                File f = getFromFiles(fileList, PREFIX_ALLOCATION + parent + EXT_ALLOCATION);
//                if(f ==null){
//                    Log.w(TAG, "Map file is missing! ");
//                }
//                return f;
//            });

            return maps;
        });
        return resultFuture;
    }
    /*
        Get metadata of map files according to speified drive names.
        If the names contain all of the drive names of map of the cdfs item, it is identical to listAll()
        Iuput:
            parent:
            names:  the drive names that metadata of the map files will be read
    */
    public CompletableFuture<HashMap<String, File>> list(@Nullable CdfsItem parent, @NonNull Collection<String> names) {
        return CompletableFuture.supplyAsync(()->{
            Map<String, List<String>> reduced = parent.getMap().entrySet().stream().filter((set) ->
                    names.contains(set.getKey())).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));

            if(reduced.isEmpty()){Log.w(TAG, "The specified names don't contain any of the key in the map!");}

            HashMap<String, File> metaData= Mapper.reValue(new HashMap<>(reduced), list->{
                File f = new File();
                f.setId(list.get(0));
                return f;
            });

            //something wrong if no mapped ID for the parent
            if(metaData.entrySet().stream().anyMatch(((set)-> set.getValue()== null))){
                Log.w(TAG, "CDFS folder is missing!");
                return null;
            }

            Fetcher fetcher = new Fetcher(mDrives);
            final CompletableFuture<HashMap<String, FileList>> listFuture = fetcher.listAll(metaData);
            final HashMap<String, FileList> fileList = listFuture.join();
            //final HashMap<String, FileList> fileListAtDest = getListAtDestination(parent, fileList, fetcher);
            HashMap<String, File> maps = Mapper.reValue(fileList, (key, list)->{
                File f = null;
                String id = parent.getName().equals(IConstant.CDFS_NAME_ROOT) ? null : parent.getId();
                if(list != null) {
                    f = getFromFiles(list, Names.allocFile(id));
                }

                if(f == null) {
                    //f = new File();
                    Log.w(TAG, "No map file found in the specified folder! Drive: " + key);
                }
                //throwExIfNull(f, "Map item is not found. Drive: " + key, "");
                return f;
            });

            return maps;
        });
    }

    /*
        Get drive meta data of a CDFS item for each drive. The method simply read mata data from
        the map in the given Cdfs item.
        Input:
            item: cdfs item
     */
    public HashMap<String, File> getMetaDataAll(@NonNull CdfsItem item){
        HashMap<String, File> file;

        Log.d(TAG, "Get meta data all.");

        file = new HashMap<>();
        //Log.d(TAG, "length: " + item.getMap().get("Google").size());
        item.getMap().forEach((k, v)->{
            File f = new File();
            Log.d(TAG, "ID: " + v.get(0));
            f.setId(v.get(0));
            Log.d(TAG, "drive: " + k + ". id[0]: " + v.get(0));
            file.put(k, f);
        });

        return file;
    }

    //obsolete function. deprecated
    public CompletableFuture<HashMap<String, File>> getMetaDataRoot(){
        Fetcher fetcher = new Fetcher(mDrives);
        Map<String, File> rootIDs;

        HashMap<String, File> result = null;
        rootIDs = mDrives.keySet().stream().map(k -> {
            Map.Entry<String, File> entry = new Map.Entry<String, File>() {
                @Override
                public String getKey() {
                    return k;
                }

                @Override
                public File getValue() {
                    File f = new File();
                    f.setId(""); //empty string indicates root is specified
                    return f;
                }

                @Override
                public File setValue(File s) {
                    return null;
                }
            };
            return entry;
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        final CompletableFuture<HashMap<String, FileList>> list = fetcher.listAll(new HashMap<>(rootIDs));
        CompletableFuture<HashMap<String, File>> resultFuture;
        resultFuture = CompletableFuture.supplyAsync(()-> {
            Map<String, File> folders = null;

            HashMap<String, FileList> resultListAll = list.join();

            folders = Mapper.reValue(list.join(), (files)->{
                File f = getFromFiles(files, NAME_CDFS_FOLDER);//TODO #42
                if(f!=null){
                    Log.d(TAG, "OK. CDFS folder found. ID: " + f.getId());
                }else{
                    Log.w(TAG, "Base folder is missing.");
                }
                return f;
            });

            return new HashMap<>(folders);
        });
        return resultFuture;
    }

    /*
        get metadata of the base folder in user drive.
        Input:
            driveName: drive name.
     */
    public CompletableFuture<File> getBaseFolder(String driveName){
        Fetcher fetcher= new Fetcher(mDrives);
        CompletableFuture<FileList> fileListFuture;

        fileListFuture =  fetcher.list(driveName, "");

        CompletableFuture<File> folder =
        CompletableFuture.supplyAsync(()->{
            File f = getFromFiles(fileListFuture.join(), Names.baseFolder());
            if(f!=null){
                Log.d(TAG, "OK. CDFS folder found. ID: " + f.getId());
            }else{
                Log.w(TAG, "Base folder is missing. Drive: " + driveName);
            }

            return f;
        });

        return folder;
    }

    /*
        Input:
            List of parents. Directly set to root in the method if null is input.
     */
    public CompletableFuture<HashMap<String, OutputStream>> pullAll(@Nullable CdfsItem parent){
        CompletableFuture <HashMap<String, File>> mapIDsFuture =
        listAll(parent);
        return pullAllByID(Mapper.reValue(mapIDsFuture.join(), (file)->{
            String id = null;
            //The file object could be null if no map file is found in user drive
            if(file != null){id = file.getId();}
            else{Log.w(TAG, "Drive id to pull is null");}
            return id;
        }));
    }

    CompletableFuture<HashMap<String, OutputStream>> pullAllByID(HashMap<String, String> fileIDs){
        Fetcher fetcher = new Fetcher(mDrives);
        return fetcher.pullAll(fileIDs);
    }

    File getFromFiles(FileList fileList, String name){
        Optional<File> files;
        File result = null;
        if(fileList.getFiles().size() > 0) {
            files = fileList.getFiles().stream().filter((file) -> {
                return file.getName().compareToIgnoreCase(name) == 0 ? true : false;
            }).findAny();

            if (files.isPresent()) {
                result = files.get();

            } else {
                Log.w(TAG, "Can not find the specified item: " + name);
            }
        }else{
            Log.w(TAG, "No files found in the specified folder");
        }

        return result;
    }
}
