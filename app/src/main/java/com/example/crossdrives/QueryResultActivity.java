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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class QueryResultActivity extends AppCompatActivity {
    private String TAG = "CD.QueryResultActivity";
    private ArrayList<ItemModelBase> mItems;
    private QueryFileAdapter mAdapter;
    private Intent mIntent;
    private int mPreLast = 0;
    private ProgressBar mProgressBar;
    private DriveServiceHelper mDriveServiceHelper;
    private Activity mActivity;

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

        //listview.setOnItemClickListener(onClickListView);
        listview.setOnScrollListener(onScrollListener);
    }

    private void queryFile(){

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mProgressBar.setVisibility(View.VISIBLE);

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
                        Log.d(TAG, "Reaches to the end");
                        if (mPreLast != lastItem) {
                            //to avoid multiple calls for last item
                            Log.d(TAG, "Last");
                            mPreLast = lastItem;
                            queryFileContinue();
                        }
                    }

                    //Log.d(TAG, "Not the expected view: " + R.id.listview_query);
            }
        }
    };
}
