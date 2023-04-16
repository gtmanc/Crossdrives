package com.crossdrives.ui.document;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

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
    List<CdfsItem> mParents;

    private ItemLiveData mItems;
    String mNextPage = null;

    Listener mListener;
    boolean Ongoing;

    /*
    Input:
        parentList: list of parent CDFS item. Null or empty list indicates root.
    */
    public OpenTree(List<CdfsItem> parentList) {
        if(parentList != null && !parentList.isEmpty()) {
            Log.d(TAG, "parentList size: " + parentList.size());
        }

        mParents = parentList;
        Log.d(TAG, "ViewModel OpenTree constructed.");
        if(parentList == null){
            Log.d(TAG, "Parent list is null.");
            mParents = new ArrayList<>();
        }
        CdfsItem item = getParent();
        if(item != null){
            Log.d(TAG, "Parent: " + getParent(parentList).getName());
        }else{
            Log.d(TAG, "Empty parent list.");
        }
        mItems = new ItemLiveData(getParent());

    }

    public interface Listener {
        void onFailure(@NonNull Exception ex);
        void onComplete();
    }

    public void setListener(Listener listener){mListener = listener;}

    public class ItemLiveData extends LiveData<ArrayList<SerachResultItemModel>> {
        private Querier mQuerier;

        private boolean firstTimeCreated = true;

        public ItemLiveData(CdfsItem parent) {
            reset(parent);
        }

        public void reset(CdfsItem parent) {
            mQuerier = new Querier(parent);
        }

        public void fetch() throws GeneralServiceException, MissingDriveClientException {
            mQuerier.getState().fetch();
        }

        @Override
        protected void onActive() {
            super.onActive();
            Log.d(TAG, "LiveDate onActive called.");
            //add a dummy item with ID null to inform ListAdaptor to show progress bar.
            if(firstTimeCreated){
                Log.d(TAG, "Add progress bar.");
                ArrayList<SerachResultItemModel> list = new ArrayList<>();
                SerachResultItemModel serachResultItemModel = new SerachResultItemModel();
                serachResultItemModel.setCdfsItem(new CdfsItem());
                list.add(serachResultItemModel);
                postValue(list);
            }

            try {
                mQuerier.getState().fetch();
            } catch (GeneralServiceException | MissingDriveClientException e ) {
                Log.w(TAG, "query data failed: " + e.getMessage());
                postValue(null);
                mQuerier.resetState();
                if(mListener != null){mListener.onFailure(e);}
            }
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            firstTimeCreated = false;
        }

        OnSuccessListener<ListResult> onSuccessListener = new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult result) {
                List<CdfsItem> items = result.getItems();
                ArrayList<SerachResultItemModel> fetched = new ArrayList<>();

                Log.d(TAG, "Number of files: " + items.size());
                for (CdfsItem item : items) {
                    //Log.d(TAG, "files name: " + file.getName());
                    //Log.d(TAG, "folder?: " + item.isFolder());
//                    boolean isFolder = false;
//                    if (item.isFolder()) {
//                        isFolder = true;
//                    }

                    SerachResultItemModel serachResultItemModel = new SerachResultItemModel();
                    serachResultItemModel.setCdfsItem(item);
                    Log.d(TAG, "Map of CDFS item: ");
                    item.getMap().entrySet().stream().forEach(set->{
                        Log.d(TAG, "drive: " + set.getKey() + ". id[0]: " + set.getValue().get(0));
                    });
                    //fetched.add(new SerachResultItemModel(false, item.getName(), item.getId(), item.getDateTime(), isFolder));
                    fetched.add(serachResultItemModel);
                }

                //in a worker thread, use the postValue(T) method to update the LiveData object.
                postValue(fetched);
            }
        };

        OnFailureListener onFailureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //mProgressBar.setVisibility(View.GONE);
                Log.e(TAG, "Unable to query files.", e);
                //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                mQuerier.resetState();
                if(mListener != null){mListener.onFailure(e);}
            }
        };

        OnCompleteListener onCompleteListener = new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                setFetchOngoing(false);
                mListener.onComplete();
            }
        };

        abstract class State {
            Querier querier;

            public State(Querier querier) {
                this.querier = querier;
            }

            public abstract void fetch() throws GeneralServiceException, MissingDriveClientException;
        }

        class FirstTimeFetchState extends State {
            public FirstTimeFetchState(Querier querier) {
                super(querier);
            }

            @Override
            public void fetch() throws GeneralServiceException, MissingDriveClientException {
                Log.d(TAG, "First time fetch");
                querier.changeState(new FetchingState(querier));
                querier.fetchAsync();
            }
        }

        class FetchingState extends State {

            public FetchingState(Querier querier) {
                super(querier);
            }

            @Override
            public void fetch() throws GeneralServiceException, MissingDriveClientException {
                Log.d(TAG, "State fetching.");
                if (querier.fetchOngoing()) {
                    Log.d(TAG, "A fetch is ongoing. Skip");
                    return;
                }
//
                if (querier.nextPageTokenNull() == null) {
                    querier.changeState(new EndState(querier));
                    Log.d(TAG, "Change to End State.");
                } else {
                    querier.fetchAsync();
                }
            }
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
        }

        class Querier {
            State state;
            CdfsItem mParent;

            public Querier(CdfsItem parent) {
                mParent = parent;
                resetState();
            }

            void changeState(State state) {
                this.state = state;
            }

            ;

            State getState() {
                return this.state;
            }

            void resetState() {
                this.state = new FirstTimeFetchState(this);
            }

            void fetchAsync() throws GeneralServiceException, MissingDriveClientException {
                CDFS.getCDFSService().getService().list(mParent)
                        .addOnSuccessListener(onSuccessListener)
                        .addOnFailureListener(onFailureListener)
                        .addOnCompleteListener(onCompleteListener);
            }

            boolean fetchOngoing() {
                return isFetchOngoing();
            }

            String nextPageTokenNull() {
                return mNextPage;
            }
        }

    }

    /*
            parent: set null to get list in base folder (CDFS root)
         */
    public void open(@Nullable CdfsItem parent) {

        if (parent != null) {
            mParents.add(parent);
        }

        //mQuerier.resetState();
        //mQuerier.getState().fetch();
    }

//    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
//
//        return fetchAsync(mParents.get(getLastIndex(mParents)));
//    }


    public void exitFolder(CdfsItem parent) {

        CdfsItem item = getLastItem(mParents);

        if (item == null) {
            return;
        } //we are in root. do nothing.

        if (item.getId().compareToIgnoreCase(parent.getId()) != 0) {
            throw new IllegalArgumentException("Parent ID could not recognized!");
        }
        mParents.remove(item);
    }

    /*
        Return: never be null. An empty list indicates we are at 'root'.
     */
    public @NonNull List<CdfsItem> getParents() {
        return mParents;
    }

    public @NonNull ItemLiveData getItems() {
        return mItems;
    }

    //public @Nullable String getNextPageToken(){return mNextPage;}

    <T> T getLastItem(List<T> list) {
        T item = null;
        if (list.size() != 0) {
            item = list.get(list.size() - 1);
        }

        return item;
    }

//    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
//        mQuerier.getState().fetch();
//        return mItems;
//    }
    @Nullable
    public List<CdfsItem> getParentList() {
        return mParents;
    }
    @Nullable
    public CdfsItem getParent() {
        return getParent(mParents);
    }

    private CdfsItem getParent(List<CdfsItem> plist){
        CdfsItem item = null;

        if(!plist.isEmpty()){
            item = plist.get(plist.size() - 1);
            Log.d(TAG, "Name of item: " + item.getName());
        }

        return item;
    }

    public boolean endOfList() {
        //We assume CDFS list always gives full list
        return true;
        //return mNextPage == null;
    }

    boolean isFetchOngoing() {
        return Ongoing;
    }

    void setFetchOngoing(boolean status) {
        Ongoing = status;
    }
}


