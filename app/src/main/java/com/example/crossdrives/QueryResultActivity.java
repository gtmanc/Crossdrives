package com.example.crossdrives;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

public class QueryResultActivity extends AppCompatActivity {
    final String STATE_NORMAL = "state_normal";
    final String STATE_ITEM_SELECTION = "state_selection";

    private String TAG = "CD.QueryResultActivity";
    private ArrayList<ItemModelBase> mItems;
    private QueryFileAdapter mAdapter;
    private Intent mIntent;
    private int mPreLast = 0;
    private ProgressBar mProgressBar;
    private DriveServiceHelper mDriveServiceHelper;
    private Activity mActivity;
    private String mState = STATE_NORMAL;
    private int mSelectedItemCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_query_result);
        Bundle bundle = this.getIntent().getExtras();
        ListView listview = (ListView) findViewById(R.id.listview_query);

        Log.d(TAG, "onCreated");

        mProgressBar = (ProgressBar) findViewById(R.id.pb);

        //Object DriveServiceHelper should be created in MainActivity
        mDriveServiceHelper = DriveServiceHelper.getInstance(null);

        //queryFile is an asynchronous process so that the listview is created right in the addOnSuccessListener
        mActivity = this;
        queryFile();

        listview.setOnScrollListener(onScrollListener);
        listview.setOnItemClickListener(onClickListView);
        listview.setOnItemLongClickListener(onOnItemLongListener);
    }
    /*
    * Initial file query
    */
    private void queryFile(){

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mProgressBar.setVisibility(View.VISIBLE);

            mDriveServiceHelper.resetQuery();
            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            ListView listview = (ListView) findViewById(R.id.listview_query);
                            mItems = new ArrayList<>();
                            Log.d(TAG, "Number of files: " + f.size());
                            for (File file : fileList.getFiles()) {
                                //Log.d(TAG, "files name: " + file.getName());
                                mItems.add(new ItemModelBase(false, file.getName(), file.getId()));
                            }

                            mAdapter = new QueryFileAdapter(mActivity, mItems);
                            listview.setAdapter(mAdapter);

                            //The files are ready to be shown. Dismiss the progress cycle
                            mProgressBar.setVisibility(View.GONE);

                            setResult(RESULT_OK, mIntent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            mProgressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Unable to query files.", exception);
                            //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                            //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        }
                    });
        }
    }

    private void queryFileContinue(){

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files continue.");

            mProgressBar.setVisibility(View.VISIBLE);

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            int i = 0;

                            Log.d(TAG, "Number of files: " + f.size());

                            for (File file : fileList.getFiles()) {
                                //Log.d(TAG, "files name: " + file.getName());
                                //ItemModelBase item = mItems.get(i);
                                mItems.add(new ItemModelBase(false, file.getName(), file.getId()));
                                //item.setName(file.getName());
                                i++;
                            }

                            //now update adapter
                            mAdapter.updateRecords(mItems);

                            //The files are ready to be shown. Dismiss the progress cycle
                            mProgressBar.setVisibility(View.GONE);

                            setResult(RESULT_OK, mIntent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            mProgressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Unable to query files.", exception);
                            //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                            //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        }
                    });
        }
    }

    private AbsListView.OnScrollListener onScrollListener= new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            //Log.d(TAG, "onScrollStateChanged");
        }

        /*

         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            Log.d(TAG, "firstVisibleItem:" +firstVisibleItem);
            Log.d(TAG, "visibleItemCount:" +visibleItemCount);
            Log.d(TAG, "totalItemCount:"+ totalItemCount);

            switch(view.getId()) {
                case R.id.listview_query:
                default:
                    // Make your calculation stuff here. You have all your
                    // needed info from the parameters of this function.

                    // Sample calculation to determine if the last
                    // item is fully visible.
                    final int lastItem = firstVisibleItem + visibleItemCount;
                    //Log.d("Last", "Reaches to the end");
                    if (lastItem == totalItemCount) {
                        //Log.d(TAG, "Reaches to the end");
                        if (mPreLast != lastItem) {
                            //to avoid multiple calls for last item
                            //Log.d(TAG, "Last");
                            mPreLast = lastItem;
                            queryFileContinue();
                        }
                    }

                    //Log.d(TAG, "Not the expected view: " + R.id.listview_query);
            }
        }
    };
    /*
    Two states :
    1. Normal
    2. Item selection
    Behavior:
    click
    1. If we are not in item selection state, go to the detail of the item
    2. If we are in item selection state (by long press any of the items), then two possibilities
    2.1 click on the selected item, exit the selection state
    2.2 click on the others, select the item. (change the checkbox in the item to "checked")
    long press
    1. If we are not in item selection state, switch to item selection state
    2. If we are already in item selection state, then two possibilities:
    2.1 Long press on the selected item, exit exit the selection state
    2.2 Long press on the others, select the item. (change the checkbox in the item to "checked")
     */
    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "Short press item:" + position);
            Log.d(TAG, "Count of selected:" + mSelectedItemCount);
            ItemModelBase item = mItems.get(position);

            if(mState == STATE_NORMAL) {
                //TODO: open detail of file
            }else {
                if(item.isSelected()) {
                    /*
                    click on the selected item, again exit item selection state
                    */
                    setItemChecked(item, position, false);
                    if(mSelectedItemCount == 0) {
                        mAdapter.setCheckBoxVisible(false);
                        mState = STATE_NORMAL;
                    }
                }else {
                    /*
                    click on the others, select the item. (change the checkbox in the item to "checked")
                    */
                    setItemChecked(item, position, true);
                }
            }

            //now update adapter
            mItems.set(position, item);
            mAdapter.updateRecords(mItems);
        }
    };

    private AbsListView.OnItemLongClickListener onOnItemLongListener = new AbsListView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "Long press item:" + position);
            Log.d(TAG, "Count of selected:" + mSelectedItemCount);

            ItemModelBase item = mItems.get(position);

            if(mState == STATE_NORMAL) {
                /*
                Switch to item selection state
                 */
                setItemChecked(item, position, true);
                mAdapter.setCheckBoxVisible(true);
                mState = STATE_ITEM_SELECTION;
            }else {
                if(item.isSelected()) {
                    /*
                    Long press on the selected item, exit item selection state
                    */
                    setItemChecked(item, position, false);
                    if(mSelectedItemCount == 0) {
                        mAdapter.setCheckBoxVisible(false);
                        mState = STATE_NORMAL;
                    }
                }else {
                    /*
                    Long press on the others, select the item. (change the checkbox in the item to "checked")
                    */
                    setItemChecked(item, position, true);
                }
            }

            //now update adapter
            mItems.set(position, item);
            mAdapter.updateRecords(mItems);

            /*
            Return true to indicate the event has been consumed
            https://stackoverflow.com/questions/5428077/android-why-does-long-click-also-trigger-a-normal-click
             */
            return true;
        }
    };

    private void setItemChecked(ItemModelBase item, int position, boolean checked){

        if(checked == false && mSelectedItemCount <= 0) {
            Log.d(TAG, "No item should be unchecked!!");
            return;
        }

        item.setSelected(checked);
        if(checked == true)
            mSelectedItemCount++;
        else
            mSelectedItemCount--;
    }
}
