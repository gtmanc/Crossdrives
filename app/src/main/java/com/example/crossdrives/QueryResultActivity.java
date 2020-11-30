package com.example.crossdrives;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

//Good article for development of recycler view:
// From an expert: https://developer.android.com/guide/topics/ui/layout/recyclerview
// Google: https://www.journaldev.com/24041/android-recyclerview-load-more-endless-scrolling

public class QueryResultActivity extends AppCompatActivity {
    final String STATE_NORMAL = "state_normal";
    final String STATE_ITEM_SELECTION = "state_selection";

    private String TAG = "CD.QueryResultActivity";
    private ArrayList<ItemModelBase> mItems;
    private QueryFileAdapter mAdapter;
    private Intent mIntent;
    private int mPreLast = 0;
    //private ProgressBar mProgressBar;
    private DriveServiceHelper mDriveServiceHelper;
    private Activity mActivity;
    private String mState = STATE_NORMAL;
    private int mSelectedItemCount = 0;
    private RecyclerView.LayoutManager layoutManager;
    Toolbar mToolbar_normal;
    Toolbar mToolbar_contextual;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_query_result);

        mToolbar_normal = findViewById(R.id.toolbar);
        mToolbar_contextual = findViewById(R.id.contextual_toolbar);
        setSupportActionBar(mToolbar_normal);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolbar_normal.setNavigationOnClickListener(onNavigationClick_NormalBar);
        mToolbar_contextual.setNavigationOnClickListener(onNavigationClick_ContextuallBar);
        //toolbar.getBackground().setAlpha(0);
        Bundle bundle = this.getIntent().getExtras();
        //ListView listview = (ListView) findViewById(R.id.listview_query);
        RecyclerView recyclerview = (RecyclerView) findViewById(R.id.recycler_view);

        Log.d(TAG, "onCreated");

        //mProgressBar = (ProgressBar) findViewById(R.id.pb);

        //Object DriveServiceHelper should be created in MainActivity
        mDriveServiceHelper = DriveServiceHelper.getInstance(null);

        //queryFile is an asynchronous process so that the listview is created right in the addOnSuccessListener
        mActivity = this;

        // will be assigned later in queryFile()
        layoutManager = new LinearLayoutManager(this);

        queryFile();

//        listview.setOnScrollListener(onScrollListener);
//        listview.setOnItemClickListener(onClickListView);
//        listview.setOnItemLongClickListener(onOnItemLongListener);
        recyclerview.addOnScrollListener(onScrollListener);
    }
    /*
    * Initial file query
    */
    private void queryFile(){

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            //mProgressBar.setVisibility(View.VISIBLE);

            mDriveServiceHelper.resetQuery();
            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            //ListView listview = (ListView) findViewById(R.id.listview_query);
                            RecyclerView recyclerView = findViewById(R.id.recycler_view);

                            mItems = new ArrayList<>();
                            Log.d(TAG, "Number of files: " + f.size());
                            for (File file : fileList.getFiles()) {
                                //Log.d(TAG, "files name: " + file.getName());
                                mItems.add(new ItemModelBase(false, file.getName(), file.getId()));
                            }

                            mAdapter = new QueryFileAdapter(mItems);
                            mAdapter.setOnItemClickListener(itemClickListener);
                            recyclerView.setAdapter(mAdapter);

                            recyclerView.setLayoutManager(layoutManager);
                            //listview.setAdapter(mAdapter);

                            //The files are ready to be shown. Dismiss the progress cycle
                            //mProgressBar.setVisibility(View.GONE);

                            setResult(RESULT_OK, mIntent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //mProgressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Unable to query files.", exception);
                            //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                            //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        }
                    });
        }
    }

    private void queryFileContinue(){
        int lastitemindex = mItems.size() - 1;

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files continue.");

            //mProgressBar.setVisibility(View.VISIBLE);

            //Insert a null item so that the adapter knows that progress bar needs to be shown to the user
            mItems.add(null);
            Log.d(TAG, "Notify inserted");
            mAdapter.notifyItemInserted(mItems.size() - 1);

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            int i = 0;
                            //now we are done with the query. take out the progress bar from the list
                            Log.d(TAG, "Notify removed");
                            mItems.remove(mItems.size() - 1);
                            mAdapter.notifyItemRemoved(mItems.size());

                            Log.d(TAG, "Number of files fetched: " + f.size());

                            for (File file : fileList.getFiles()) {
                                //Log.d(TAG, "files name: " + file.getName());
                                //ItemModelBase item = mItems.get(i);
                                mItems.add(new ItemModelBase(false, file.getName(), file.getId()));
                                //item.setName(file.getName());
                                i++;
                            }

                            //now update adapter
                            //mAdapter.updateRecords(mItems);
                            Log.d(TAG, "Notify data set change");
                            //TODO: to clarify why the newly loaded items are not updated to screen if we dont do any further scroll.
                            // i.e. enter the recycler view from previous screen and only few items are initially loaded
                            mAdapter.notifyDataSetChanged();
                            //The files are ready to be shown. Dismiss the progress cycle
                            //mProgressBar.setVisibility(View.GONE);

                            setResult(RESULT_OK, mIntent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //mProgressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Unable to query files.", exception);
                            //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                            //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        }
                    });
        }
    }

    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

            //fetch next page if last item is already shown to the user
            if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == mItems.size() - 1) {
                //bottom of list!
                queryFileContinue();
            }
        }
    };

    private QueryFileAdapter.OnItemClickListener itemClickListener = new QueryFileAdapter.OnItemClickListener(){
        @Override
        public void onItemClick(View view, int position) {
            Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Pressed!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Short press item:" + position);
            Log.d(TAG, "Count of selected:" + mSelectedItemCount);
            ItemModelBase item = mItems.get(position);

            if(view == findViewById(R.id.iv_more_vert)){
                Log.d(TAG, "More_vert pressed!");
            }

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

                        switchNormalActionBar();
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
            //mAdapter.notifyItemChanged(position);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemLongClick(View view, int position) {

            Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Long Pressed!", Toast.LENGTH_SHORT).show();
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

                switchContextualActionBar();
            }else {
                if(item.isSelected()) {
                    /*
                    Long press on the selected item, exit item selection state
                    */
                    setItemChecked(item, position, false);
                    if(mSelectedItemCount == 0) {
                        mAdapter.setCheckBoxVisible(false);
                        mState = STATE_NORMAL;

                        switchNormalActionBar();
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
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onImageItemClick(View view, int position) {
            Log.d(TAG, "onImageItemClick:" + position);
        }
    };

//    private AbsListView.OnScrollListener onScrollListener= new AbsListView.OnScrollListener() {
//
//        @Override
//        public void onScrollStateChanged(AbsListView view, int scrollState) {
//            //Log.d(TAG, "onScrollStateChanged");
//        }
//
//        /*
//
//         */
//        @Override
//        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//            Log.d(TAG, "firstVisibleItem:" +firstVisibleItem);
//            Log.d(TAG, "visibleItemCount:" +visibleItemCount);
//            Log.d(TAG, "totalItemCount:"+ totalItemCount);
//
//            switch(view.getId()) {
//                case R.id.listview_query:
//                default:
//                    // Make your calculation stuff here. You have all your
//                    // needed info from the parameters of this function.
//
//                    // Sample calculation to determine if the last
//                    // item is fully visible.
//                    final int lastItem = firstVisibleItem + visibleItemCount;
//                    //Log.d("Last", "Reaches to the end");
//                    if (lastItem == totalItemCount) {
//                        //Log.d(TAG, "Reaches to the end");
//                        if (mPreLast != lastItem) {
//                            //to avoid multiple calls for last item
//                            //Log.d(TAG, "Last");
//                            mPreLast = lastItem;
//                            queryFileContinue();
//                        }
//                    }
//
//                    //Log.d(TAG, "Not the expected view: " + R.id.listview_query);
//            }
//        }
//    };
//    /*
//    Two states :
//    1. Normal
//    2. Item selection
//    Behavior:
//    click
//    1. If we are not in item selection state, go to the detail of the item
//    2. If we are in item selection state (by long press any of the items), then two possibilities
//    2.1 click on the selected item, exit the selection state
//    2.2 click on the others, select the item. (change the checkbox in the item to "checked")
//    long press
//    1. If we are not in item selection state, switch to item selection state
//    2. If we are already in item selection state, then two possibilities:
//    2.1 Long press on the selected item, exit exit the selection state
//    2.2 Long press on the others, select the item. (change the checkbox in the item to "checked")
//     */
//    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            Log.d(TAG, "Short press item:" + position);
//            Log.d(TAG, "Count of selected:" + mSelectedItemCount);
//            ItemModelBase item = mItems.get(position);
//
//            if(mState == STATE_NORMAL) {
//                //TODO: open detail of file
//            }else {
//                if(item.isSelected()) {
//                    /*
//                    click on the selected item, again exit item selection state
//                    */
//                    setItemChecked(item, position, false);
//                    if(mSelectedItemCount == 0) {
//                        mAdapter.setCheckBoxVisible(false);
//                        mState = STATE_NORMAL;
//                    }
//                }else {
//                    /*
//                    click on the others, select the item. (change the checkbox in the item to "checked")
//                    */
//                    setItemChecked(item, position, true);
//                }
//            }
//
//            //now update adapter
//            mItems.set(position, item);
//            mAdapter.updateRecords(mItems);
//        }
//    };
//
//    private AbsListView.OnItemLongClickListener onOnItemLongListener = new AbsListView.OnItemLongClickListener() {
//        @Override
//        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//            Log.d(TAG, "Long press item:" + position);
//            Log.d(TAG, "Count of selected:" + mSelectedItemCount);
//
//            ItemModelBase item = mItems.get(position);
//
//            if(mState == STATE_NORMAL) {
//                /*
//                Switch to item selection state
//                 */
//                setItemChecked(item, position, true);
//                mAdapter.setCheckBoxVisible(true);
//                mState = STATE_ITEM_SELECTION;
//            }else {
//                if(item.isSelected()) {
//                    /*
//                    Long press on the selected item, exit item selection state
//                    */
//                    setItemChecked(item, position, false);
//                    if(mSelectedItemCount == 0) {
//                        mAdapter.setCheckBoxVisible(false);
//                        mState = STATE_NORMAL;
//                    }
//                }else {
//                    /*
//                    Long press on the others, select the item. (change the checkbox in the item to "checked")
//                    */
//                    setItemChecked(item, position, true);
//                }
//            }
//
//            //now update adapter
//            mItems.set(position, item);
//            mAdapter.updateRecords(mItems);
//
//            /*
//            Return true to indicate the event has been consumed
//            https://stackoverflow.com/questions/5428077/android-why-does-long-click-also-trigger-a-normal-click
//             */
//            return true;
//        }
//    };
//
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

    private void switchContextualActionBar(){

        //Switch to contextual tool bar
        mToolbar_normal.setVisibility(View.GONE);
        mToolbar_contextual.setVisibility(View.VISIBLE);
        setSupportActionBar(mToolbar_contextual);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void switchNormalActionBar(){

        mToolbar_contextual.setVisibility(View.GONE);
        mToolbar_normal.setVisibility(View.VISIBLE);
        setSupportActionBar(mToolbar_normal);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setOnMenuItemClickListener(OnMenuItemClickListener);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //searchView.setSubmitButtonEnabled(true);
        return true;
    }

    private MenuItem.OnMenuItemClickListener OnMenuItemClickListener = new MenuItem.OnMenuItemClickListener(){
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            //YourActivity.this.someFunctionInYourActivity();
            Log.d(TAG, "Menu item action pressed!!");
            return true;
        }
    };

    private View.OnClickListener onNavigationClick_NormalBar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Normal action bar Navigation icon pressed!!");
        }
    };

    private View.OnClickListener onNavigationClick_ContextuallBar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Contextual action bar Navigation icon pressed!!");
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Bundle bundle = null;

        Log.d(TAG, "Action bar item pressed!!");

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.search){
            Log.d(TAG, "Search button pressed!!");
            onSearchRequested();
        }

        return super.onOptionsItemSelected(item);
    }
}
