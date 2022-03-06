package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.list.IFileListCallBack;
import com.google.api.services.drive.model.FileList;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AllocationFetcher {
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
    ICallBackAllocationFetch callback;
    class State{
        static public final int STATE_IDLE = 0;
        static public final int STATE_INPROGRESS = 1;
        static public final int STATE_FINISHED = 2;

        int state = STATE_IDLE;

        public void setState(int state) { this.state = state;}
        public int getState() {return state;}

    }

    public AllocationFetcher(ConcurrentHashMap<String, Drive> drives) { mDrives = drives; }



    public void fetchAll(ICallBackAllocationFetch<String> callback){

        this.callback = callback;
    }

    private void fetch(String name, IDriveClient client){
        State fr = new State();
//        CompletableFuture<String> future = new CompletableFuture<>();
//
//        sExecutor.submit(() -> {
        fr.setState(State.STATE_INPROGRESS);
        states.put(name, fr);
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
                            //future.complete(result);
                            fr.setState(State.STATE_FINISHED);

                            joinResult = joinResult();
                            if(joinResult == true) {
                                callback.onCompleted(null);
                            }
                        }

                        @Override
                        public void failure(String ex) {
                            boolean joinResult;
                            //future.completeExceptionally(new Throwable(""));
                            joinResult = joinResult();

                            fr.setState(State.STATE_FINISHED);
                            if(joinResult == true) {
                                callback.onCompletedExceptionally(null);
                            }
                        }
                    });
//            return future;
//        });
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
