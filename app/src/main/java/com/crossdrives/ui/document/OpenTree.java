package com.crossdrives.ui.document;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.list.ListResult;
import com.crossdrives.cdfs.model.CdfsItem;
import com.example.crossdrives.SerachResultItemModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/*
    Design hint from Google:
    A ViewModel usually shouldn't reference a view, Lifecycle, or any class that may hold a reference to the activity context.
    Because the ViewModel lifecycle is larger than the UI's, holding a lifecycle-related API in the ViewModel could cause memory leaks.
 */
public class OpenTree extends ViewModel {
    final String TAG = "CD.OpenTree";

    //folder topology: parents (folders). An empty list indicates we are at 'root'.
    List<CdfsItem> mParents = new ArrayList<>();

    private MutableLiveData<ArrayList<SerachResultItemModel>> mItems = new MutableLiveData<>();
    String mNextPage = null;

    boolean Ongoing;

    private Querier mQuerier = new Querier();

    /*
            parent: set null to get list in base folder (CDFS root)
         */
    public void open(@Nullable CdfsItem parent) throws GeneralServiceException, MissingDriveClientException {

        if(parent != null) {
            mParents.add(parent);
        }

        mQuerier.resetState();
        //mQuerier.getState().fetch();
    }

//    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
//
//        return fetchAsync(mParents.get(getLastIndex(mParents)));
//    }


    public void exitFolder(CdfsItem parent){

        CdfsItem item = getLastItem(mParents);

        if(item == null){return;} //we are in root. do nothing.

        if(item.getId().compareToIgnoreCase(parent.getId()) != 0){
            throw new IllegalArgumentException("Parent ID could not recognized!");
        }
        mParents.remove(item);
    }

    public @NonNull List<CdfsItem> getParents(){
        return mParents;
    }

    public @NonNull MutableLiveData<ArrayList<SerachResultItemModel>> getItems(){return mItems;}

    //public @Nullable String getNextPageToken(){return mNextPage;}

    <T> T getLastItem(List<T> list){
        T item = null;
        if(list.size() != 0){
            item = list.get(list.size()-1);
        }

        return item;
    }

    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
        mQuerier.getState().fetch();
        return mItems;
    }

    MutableLiveData<ArrayList<SerachResultItemModel>> list() throws GeneralServiceException, MissingDriveClientException {
        CdfsItem parent = getLastItem(mParents);
        /*
            call CDFS list to fetch the list asynchronously
         */
        //       try {
        setFetchOngoing(true);
        CDFS.getCDFSService().getService().list(parent)
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult result) {
                        List<CdfsItem> items = result.getItems();
                        ArrayList<SerachResultItemModel> fetched = new ArrayList<>();

                        Log.i(TAG, "Number of files: " + items.size());
                        for (CdfsItem item : items) {
//                                if(file.getModifiedTime() == null){
//                                    Log.w(TAG, "Modified dateTime is null");
//                                }
                            //Log.d(TAG, "files name: " + file.getName());
                            boolean isFolder = false;
                            if(item.isFolder()){isFolder = true;}
                            fetched.add(new SerachResultItemModel(false, item.getName(), item.getId(), item.getDateTime(), isFolder));
                        }

                        //in a worker thread, use the postValue(T) method to update the LiveData object.
                        mItems.postValue(fetched);

//                        mNextPage = result.getFileList().getNextPageToken();
//                            if(mNextPage == null){
//                                Log.d(TAG, "Next page handler is null!");
//                                CloseQuery();
//                            }
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        //mProgressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Unable to query files.", exception);
                        //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                        //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        ArrayList<SerachResultItemModel> fetched = null;
                        mItems.postValue(fetched);
                    }
                }).addOnCompleteListener(new OnCompleteListener<ListResult>() {
                    @Override
                    public void onComplete(@NonNull Task<ListResult> task) {
                        setFetchOngoing(false);
                    }
                });
//        }
//        catch (MissingDriveClientException | GeneralServiceException e) {
//            Log.w(TAG, e.getMessage());
//            Log.w(TAG, e.getCause());
//            ArrayList<SerachResultItemModel> fetched = null;
//            mItems.postValue(fetched);
//        }

        return mItems;
    }

    @Nullable public CdfsItem whereWeAre(){
        return mParents.isEmpty()? null : mParents.get(mParents.size()-1);
    }
    public boolean endOfList(){
        //We assume CDFS list always gives full list
        return true;
        //return mNextPage == null;
    }

    boolean isFetchOngoing(){return Ongoing;}
    void setFetchOngoing(boolean status){Ongoing = status;}

    abstract class State{
        Querier querier;
        public State(Querier querier) {
            this.querier = querier;
        }

        public abstract void fetch() throws GeneralServiceException, MissingDriveClientException;

        //public abstract void onComplete();
    }

//    class CloseState extends State {
//        public CloseState(Querier querier) {
//            super(querier);
//        }
//
//        @Override
//        public void fetch() throws GeneralServiceException, MissingDriveClientException {
//            Log.d(TAG, "State Close.");
////            querier.changeState(new IdleState(querier));
////            treeOpener.open(null);	//set null to query the items in base folder
////            treeOpener.fetchAsync();
////            //fetchList.fetchAsync(mParents, null);
//        }
//
////        @Override
////        public void onComplete(){
////
////        }
//    }

//    class IdleState extends State {
//        public IdleState(Querier querier) {
//            super(querier);
//        }
//
//        @Override
//        public void fetch() throws GeneralServiceException, MissingDriveClientException {
//            Log.d(TAG, "State Idle");
//            querier.changeState(new FetchingState(querier));
////            treeOpener.open(null);	//set null to query the items in base folder
////            treeOpener.fetchAsync();
////            //fetchList.fetchAsync(mParents, null);
//        }
//
//        @Override
//        public void onComplete() {
//
//        }
//    }

    class FirstTimeFetchState extends State{
        public FirstTimeFetchState(Querier querier) {
            super(querier);
        }

        @Override
        public void fetch() throws GeneralServiceException, MissingDriveClientException {
            Log.d(TAG, "First time fetch");
            querier.changeState(new FetchingState(querier));
            querier.fetchAsync();
        }

//        @Override
//        public void onComplete() {
//
//        }
    }
    class FetchingState extends State {

        public FetchingState(Querier querier) {
            super(querier);
        }

        @Override
        public void fetch() throws GeneralServiceException, MissingDriveClientException {
            Log.d(TAG, "State fetching.");
           if (querier.fetchOngoing()){
               Log.d(TAG, "A fetch is ongoing. Skip");
               return;
           }
//
            if(querier.nextPageTokenNull() == null){
                querier.changeState(new EndState(querier));
                Log.d(TAG, "Change to End State.");
            }else{
                querier.fetchAsync();
            }

        }

//        @Override
//        public void onComplete() {
//            querier.changeState(new IdleState(querier));
//        }
    }

    class EndState extends State {

        public EndState(Querier querier) {
            super(querier);
        }

        @Override
        public void fetch() {
            Log.d(TAG, "State End.");
//            querier.changeState(new CloseState(querier));
        }

//        @Override
//        public void onComplete() {
//
//        }
    }

    class Querier{
        State state;

        public Querier() {
            resetState();
        }

        void changeState(State state){
            this.state = state;
        };

        State getState(){return this.state;}

        void resetState(){this.state = new FirstTimeFetchState(this);}

        void fetchAsync() throws GeneralServiceException, MissingDriveClientException {
            OpenTree.this.list();
        }

        boolean fetchOngoing(){
            return isFetchOngoing();
        }

        String nextPageTokenNull(){
            return mNextPage;
        }
    }

}
