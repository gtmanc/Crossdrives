package com.crossdrives.cdfs.allocation;

import android.util.Log;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.exception.ItemNotFoundException;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.cdfs.util.Mapper;
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
import java.util.stream.Collectors;

public class MapFetcher {
    private final String TAG = "CD.MapFetcher";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    /*
        Query strings
     */
    private final String NAME_CDFS_FOLDER = "CDFS";
    final String PREFIX_ALLOCATION = "Allocation_";
    final String EXT_ALLOCATION = ".cdfs";
    private final String NAME_ALLOCATION_ROOT = "Allocation_root.cdfs";
    private final String NAME_ALLOCATION = "Allocation.cdfs";
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";

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

    public MapFetcher(ConcurrentHashMap<String, Drive> drives) {
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
            clause = clause.concat("'" + NAME_CDFS_FOLDER+ "'");
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
            return file.getName().compareToIgnoreCase(NAME_ALLOCATION_ROOT) == 0 ?  true : false;
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
        Get specified map in CDFS folder for each drive
     */
    public CompletableFuture<HashMap<String, File>> listAll(String cdfsPid) {
        CompletableFuture<HashMap<String, File>> resultFuture;
        Fetcher fetcher = new Fetcher(mDrives);

        resultFuture = CompletableFuture.supplyAsync(()-> {
            HashMap<String, File> baseFolders;
            CompletableFuture<HashMap<String, File>> foldersFuture = getFolderAll();

            //if any is null. exit
            Log.d(TAG, "Check CDFS folders... ");
            baseFolders = foldersFuture.join();

            if(baseFolders.values().stream().anyMatch(((v)-> v == null))){
                Log.w(TAG, "CDFS folder is missing!");
                return null;
            }

            final CompletableFuture<HashMap<String, FileList>> list = fetcher.listAll(baseFolders);
            final HashMap<String, FileList> fileListsBase = list.join();
            final HashMap<String, FileList> fileListAtDest = getListAtDestination(parent, fileListsBase, fetcher);
            HashMap<String, File> maps = Mapper.reValue(fileListAtDest, (key, fileList)->{
                File f = getFromFiles(fileList, NAME_ALLOCATION);
                throwExIfNull(f, "Map item is not found. Drive: " + key, "");
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
        Walk through the parents and return the map item list at the destination folder.
     */
    HashMap<String, FileList> getListAtDestination(List<String> pids, HashMap<String, FileList> fileListBase, Fetcher fetcher){
        Collection<Exception> exceptions = new ArrayList<>();

        HashMap<String, FileList>[] fileLists = new HashMap[]{fileListBase};
        CompletableFuture<HashMap<String, FileList>>[] list = new CompletableFuture[]{};
        pids.stream().forEachOrdered((pid)->{
            HashMap<String, File> nextFolder = findItemMatched(fileLists[0], pid);
            list[0] = fetcher.listAll(nextFolder);
            fileLists[0] = list[0].join();
        });

        return fileLists[0];
    }
    /*
        Find the file (item) which the ID is matched to parent ID for each drive.
        i.e. the ID is CDFS ID
        Input:
            fileList: the given item list
            pid     : the parent id we are looking for
        Output:
            Found item in each drive.
     */
    HashMap<String, File> findItemMatched(HashMap<String, FileList> fileLists, String pid){

        //Get map item from the input list
        HashMap<String, File> mapItems = Mapper.reValue(fileLists, (key, list)->{

            File file = getFromFiles(list, NAME_ALLOCATION);
            throwExIfNull(file, "Map file may be missing! " + "Drive:" + key, "");
            return file;
         });

        //download map
        CompletableFuture<HashMap<String, OutputStream>> downloadFuture = pullAllByID(
                Mapper.reValue(mapItems, (file)->{
                    return file.getId();
                }));

        //Find the item which the CDFS ID matches to pid
        HashMap<String, AllocationItem> allocationItems = Mapper.reValue(downloadFuture.join(), (key, stream)->{
            AllocationItem result = null;
            Optional<AllocationItem> optional =
            AllocManager.toContainer(stream).getAllocItem().stream().filter((item)->{
                return item.getCdfsId().compareToIgnoreCase(pid) == 0;
            }).findAny();
            throwExIfNotPresent(optional, "Parent not found! " + "Drive:" + key, "");
            return optional.get();
        });

        //Transform the found item to the output type
        return Mapper.reValue(allocationItems, (ai)->{
            File file = new File();
            file.setId(ai.getItemId());
            return file;
        });
    }


    <T> void throwExIfNotPresent(Optional<T> optional, String message, String cause) throws ItemNotFoundException {
        if(!optional.isPresent()) {
            throw new ItemNotFoundException(message, new Throwable(cause));
        }
    }

    <T> void throwExIfNull(T t, String message, String cause) throws ItemNotFoundException {
        if(t == null) {
            throw new ItemNotFoundException(message, new Throwable(cause));
        }
    }

    /*
        Get folder meta data for each drive
     */
    public CompletableFuture<HashMap<String, File>> getFolderAll(){
        Fetcher fetcher = new Fetcher(mDrives);
        CompletableFuture<HashMap<String, File>> resultFuture;
        resultFuture = CompletableFuture.supplyAsync(()-> {
            Map<String, File> rootIDs;
            Map<String, File> folders = null;

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

            CompletableFuture<HashMap<String, FileList>> list = null;
            list = fetcher.listAll(new HashMap<>(rootIDs));

            HashMap<String, FileList> resultListAll = list.join();

            folders = Mapper.reValue(list.join(), (files)->{
                File f = getFromFiles(files, NAME_CDFS_FOLDER);//TODO #42
                Log.d(TAG, "OK. CDFS folder found. ID: " + f.getId());
                return f;
            });


            return new HashMap<>(folders);
        });
        return resultFuture;
    }

    public CompletableFuture<File> getBaseFolder(String driveName){
        Fetcher fetcher= new Fetcher(mDrives);
        CompletableFuture<FileList> fileListFuture;

        fileListFuture =  fetcher.list(driveName, "");

        CompletableFuture<File> folder =
        CompletableFuture.supplyAsync(()->{
            File f = getFromFiles(fileListFuture.join(), NAME_CDFS_FOLDER);
            Log.d(TAG, "OK. CDFS folder found. ID: " + f.getId());
            return f;
        });

        return folder;
    }

    public CompletableFuture<HashMap<String, OutputStream>> pullAll(List<String> parent){
        CompletableFuture <HashMap<String, File>> mapIDsFuture =
        listAll(parent);
        return pullAllByID(Mapper.reValue(mapIDsFuture.join(), (file)->{
            return file.getId();
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
                Log.w(TAG, "No root allocation file presents!");
            }
        }else{
            Log.w(TAG, "No files found in the specified folder");
        }

        return result;
    }
}
