package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.remote.Fetcher;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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
    private final String NAME_ALLOCATION_ROOT = "Allocation_root.cdfs";
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
    public void fetchAll(String parent, ICallBackMapFetch<HashMap<String, OutputStream>> callback) {

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
            fetch(states.get(name), name, drive.getClient());
        });

    }

    private void fetch(State state, String name, IDriveClient client){
        /*
            Start with get CDFS folder
         */
        getFolder(state, name, client);
    }

    private void getFolder(State state, String name, IDriveClient client){

//        CompletableFuture<String> future = new CompletableFuture<>();
//
//        sExecutor.submit(() -> {
        state.setState(State.STATE_CHECK_FOLDER);
        //states.put(name, state);
        client.list().buildRequest()
                //sClient.get(0).list().buildRequest()
                .setNextPage(null)
                .setPageSize(0) //0 means no page size is applied
                .filter(FILTERCLAUSE_CDFS_FOLDER)
                //.filter("mimeType = 'application/vnd.google-apps.folder'")
                //.filter(null)   //null means no filter will be applied
                .run(new IFileListCallBack<FileList, Object>() {
                    //As we specified the folder name, suppose only cdfs folder in the list.
                    @Override
                    public void success(FileList fileList, Object o) {
                        String id = null;

                        id = handleResultGetFolder(fileList);
                        if(id != null) {
                            getFileID(state, name, client, id);
                        }
                        else{
                            //Known issue - #42
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

    private String handleResultGetFolder(FileList fileList){
        String id = null;
//      for(int i = 0 ; i < fileList.getFiles().size(); i++){
//          fileList.getFiles().get(i).getName().compareToIgnoreCase("cdfs");
//      }
        if(fileList.getFiles().size() > 0) {
            if (fileList.getFiles().get(0).getName().compareToIgnoreCase("cdfs") == 0) {
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
                .setPageSize(0) //0 means no page size is applied
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
                .run(new IDownloadCallBack<OutputStream>() {

                    @Override
                    public void success(OutputStream outputStream) {
                        Log.d(TAG, "Download allocation file OK");
                        boolean joinResult;
                        output.put(name, outputStream);
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

    public CompletableFuture<HashMap<String, File>> listAll() {
        CompletableFuture<HashMap<String, File>> resultFuture;
        Fetcher fetcher = new Fetcher(mDrives);

        resultFuture = CompletableFuture.supplyAsync(()->{
            Map<String, File> rootIDs;
            Map<String, File> folders = null;
            Map<String, File> maps;
            HashMap<String, File> result = null;
            rootIDs = mDrives.keySet().stream().map(k-> {
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
            }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));


            try {
                CompletableFuture<HashMap<String, FileList>> list = fetcher.listForAll(new HashMap<>(rootIDs));
                HashMap<String, FileList> resultListAll = list.get();

                folders = resultListAll.entrySet().stream().map((set)->{
                    Map.Entry<String, File> entry = new Map.Entry<String, File>() {
                        @Override
                        public String getKey() {
                            return set.getKey();
                        }

                        @Override
                        public File getValue() {
                            File f = getFromFiles(set.getValue(), NAME_CDFS_FOLDER);
                            Log.d(TAG, "CDFS folder: " + f.getName());
                            return f;
                        }

                        @Override
                        public File setValue(File o) {
                            return null;
                        }
                    };
                    return entry;
                }).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()));
            } catch (ExecutionException e) {
                Log.w(TAG, e.getMessage());

            } catch (InterruptedException e) {
                Log.w(TAG, e.getMessage());
            }

            //No CDFS folder found, exit.
            if(folders == null){
                return null;
            }

            try {
                CompletableFuture<HashMap<String, FileList>> list = fetcher.listForAll(new HashMap<>(folders));
                HashMap<String, FileList> files = list.get();
                maps = files.entrySet().stream().map((set)->{
                    Map.Entry<String, File> entry = new Map.Entry<String, File>() {
                        @Override
                        public String getKey() {
                            return set.getKey();
                        }

                        @Override
                        public File getValue() {
                            File f = getFromFiles(set.getValue(), NAME_ALLOCATION_ROOT);
                            Log.d(TAG, "Map file: " + f.getName());
                            return f;
                        }

                        @Override
                        public File setValue(File value) {
                            return null;
                        }
                    };
                    return entry;
                }).collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
                result = new HashMap(maps);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return result;
        });
        return resultFuture;
    }

    CompletableFuture<HashMap<String, OutputStream>> contentForAll(){return null;}


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
