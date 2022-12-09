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
    List<String> mParents = new ArrayList<>();

    private MutableLiveData<ArrayList<SerachResultItemModel>> mItems = new MutableLiveData<>();
    String mNextPage = null;

    /*
        parentID: set null to get list in base folder (CDFS root)
     */
    public void open(@Nullable String parentId) throws GeneralServiceException, MissingDriveClientException {
        if(parentId != null) {
            mParents.add(parentId);
        }
    }

    public MutableLiveData<ArrayList<SerachResultItemModel>> fetchListByPageAsync() throws GeneralServiceException, MissingDriveClientException {
        return fetchAsync(mParents, mNextPage);
    }


    public String exitFolder(String parentId){
        if(mParents.get(mParents.size()-1).compareToIgnoreCase(parentId) != 0){
            throw new IllegalArgumentException("Parent ID could not recognized!");
        }
        mParents.remove(mParents.size()-1);
        return parentId;
    }

    MutableLiveData<ArrayList<SerachResultItemModel>> fetchAsync(@Nullable List<String> parentIDs, @Nullable String nextPage) throws GeneralServiceException, MissingDriveClientException {
        /*
            call CDFS list to fetch the list asynchronously
         */
        //       try {
        CDFS.getCDFSService().getService().list(nextPage)
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

    public @NonNull MutableLiveData<ArrayList<SerachResultItemModel>> getItems(){return mItems;}

    public @Nullable String getNextPageToken(){return mNextPage;}
}
