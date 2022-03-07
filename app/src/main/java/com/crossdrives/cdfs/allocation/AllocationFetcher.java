package com.crossdrives.cdfs.allocation;

import android.util.Log;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.download.IDownloadCallBack;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.api.services.drive.model.FileList;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AllocationFetcher {
    private final String TAG = "CD.AllocationFetcher";
    ConcurrentHashMap<String, Drive> mDrives;
    private final ExecutorService sExecutor = Executors.newCachedThreadPool();

    /*
        Query strings
     */
    private final String NAME_CDFS_FOLDER = "CDFS";
    private final String MINETYPE_FOLDER = "application/vnd.google-apps.folder";
    private final String FILTERCLAUSE_CDFS_FOLDER = "mimeType = '" + MINETYPE_FOLDER  +
            "' and name = '" + NAME_CDFS_FOLDER + "'";

    private HashMap<String, State> states = new HashMap<>();
    private HashMap<String, OutputStream> output = new HashMap<>();
    ICallBackAllocationFetch callback;

    /*
        State for fetch
     */
    class State{
        static public final int STATE_IDLE = 0;
        static public final int STATE_CHECK_FOLDER = 1;
        static public final int STATE_CHECK_FILE = 2;
        static public final int STATE_DOWNLOAD_FILE = 3;

        int state = STATE_IDLE;

        public void setState(int state) { this.state = state;}
        public int getState() {return state;}

    }

    public AllocationFetcher(ConcurrentHashMap<String, Drive> drives) { mDrives = drives; }


    public void fetchAll(ICallBackAllocationFetch<String> callback){

        this.callback = callback;
    }

    private void fetch(String name, IDriveClient client){
//            return future;
//        });
    }

    private void getFolder(String name, IDriveClient client){
        State state = new State();
//        CompletableFuture<String> future = new CompletableFuture<>();
//
//        sExecutor.submit(() -> {
        state.setState(State.STATE_CHECK_FOLDER);
        states.put(name, state);
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
                        boolean joinResult;
                        String id = null;

                        id = handleResultGetFolder(fileList);
                        if(id != null) {
                            getFileID(state, client, id);
                        }
                        else{
                            //There must be something wrong.
                            callback.onCompletedExceptionally(new Throwable("CDFS folder is missing"));
                        }
                        joinResult = joinResult();
                        if(joinResult == true) {
                            callback.onCompleted(null);
                        }
                        //future.complete(result);
                    }

                    @Override
                    public void failure(String ex) {
                        boolean joinResult;

                        joinResult = joinResult();

                        fr.setState(State.STATE_FINISHED);
                        if(joinResult == true) {
                            callback.onCompletedExceptionally(new Throwable(ex));
                        }
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

    private void getFileID(State state, IDriveClient client, String parentid){
        String query = "'" + parentid + "' in parents";

        state.setState(State.STATE_CHECK_FILE);
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
                        id = handleResultGetFile(fileList);
                        if(id != null) {
                            download(state, client, id);
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
        String id = null;
        if(fileList.getFiles().size() > 0) {
            if (fileList.getFiles().get(0).getName().compareToIgnoreCase("allocation.cdfs") == 0) {
                id = fileList.getFiles().get(0).getId();
            } else {
                Log.w(TAG, "Files re found. But no allocation file in cdfs folder!");
            }
        }else{
            Log.w(TAG, "No file is found in CDFS folder");
        }
        return id;
    }

    private void download(State state, IDriveClient client, String fileid){
        state.setState(State.STATE_DOWNLOAD_FILE);

        client.download().buildRequest(fileid)
                .run(new IDownloadCallBack<OutputStream>() {

                    @Override
                    public void success(OutputStream outputStream) {
                        result.valid = handleResultDownload(outputStream);
                        //future.complete(result);
                    }

                    @Override
                    public void failure(String ex) {
                        //future.completeExceptionally(new Throwable(""));
                    }
                });
    }

    private boolean joinResult(){
        AtomicBoolean result = new AtomicBoolean(true);
        mDrives.forEach((name, drive) -> {
            State state;
            state = states.get(name);
            if(state.getState() != State.STATE_FINISHED){
                result.set(false);
            }
        });
    }
}
