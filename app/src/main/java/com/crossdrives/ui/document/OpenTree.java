package com.crossdrives.ui.document;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Result;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.model.AllocationItem;
import com.example.crossdrives.SerachResultItemModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

/*
    Design hint from Google:
    A ViewModel usually shouldn't reference a view, Lifecycle, or any class that may hold a reference to the activity context.
    Because the ViewModel lifecycle is larger than the UI's, holding a lifecycle-related API in the ViewModel could cause memory leaks.
 */
public class OpenTree extends ViewModel {
    final String TAG = "CD.OpenTree";

    //Topology: path including root to folder where we are. 'Root' is always the first element.
    List<AllocationItem> mParents = new ArrayList<>();

    private MutableLiveData<ArrayList<SerachResultItemModel>> mItems = new MutableLiveData<>();
    String mNextPage = null;

    /*
        parent: set null to get list in base folder (CDFS root)
     */
    public void open(@Nullable AllocationItem parent) throws GeneralServiceException, MissingDriveClientException {
        if(parent != null) {
            mParents.add(parent);
        }
    }

//    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
//
//        return fetchAsync(mParents.get(getLastIndex(mParents)));
//    }


    public void exitFolder(AllocationItem parent){
        int i = getLastIndex(mParents);

        if(mParents.get(i).getCdfsId().compareToIgnoreCase(parent.getCdfsId()) != 0){
            throw new IllegalArgumentException("Parent ID could not recognized!");
        }
        mParents.remove(i);;
    }

    public @NonNull MutableLiveData<ArrayList<SerachResultItemModel>> getItems(){return mItems;}

    //public @Nullable String getNextPageToken(){return mNextPage;}

    <T> int getLastIndex(List<T> list){
        int i;
        if(list == null){ i = 0;}
        else{ i = list.size()-1;}
        return i;
    }


    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync() throws GeneralServiceException, MissingDriveClientException {
        AllocationItem parent = mParents.get(getLastIndex(mParents));
        /*
            call CDFS list to fetch the list asynchronously
         */
        //       try {
        CDFS.getCDFSService().getService().list(parent)
                .addOnSuccessListener(new OnSuccessListener<Result>() {
                    @Override
                    public void onSuccess(com.crossdrives.cdfs.Result result) {
                        List<File> f = result.getFileList().getFiles();
                        //ListView listview = (ListView) findViewById(R.id.listview_query);

                        ArrayList<SerachResultItemModel> fetched = new ArrayList<>();

                        Log.i(TAG, "Number of files: " + f.size());
                        for (File file : result.getFileList().getFiles()) {
//                                if(file.getModifiedTime() == null){
//                                    Log.w(TAG, "Modified dateTime is null");
//                                }
                            //Log.d(TAG, "files name: " + file.getName());
                            boolean isFolder = false;
                            if(file.getParents()!=null){isFolder = true;}
                            fetched.add(new SerachResultItemModel(false, file.getName(), file.getId(), file.getModifiedTime(), isFolder));
                        }

                        //in a worker thread, use the postValue(T) method to update the LiveData object.
                        mItems.postValue(fetched);

                        mNextPage = result.getFileList().getNextPageToken();
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

    public boolean endOfList(){return mNextPage == null;}

}
