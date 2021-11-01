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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

//Good article for development of recycler view:
// From an expert: https://developer.android.com/guide/topics/ui/layout/recyclerview
// Google: https://www.journaldev.com/24041/android-recyclerview-load-more-endless-scrolling

public class QueryResultActivity extends AppCompatActivity {
    final String STATE_NORMAL = "state_normal";
    final String STATE_ITEM_SELECTION = "state_selection";

    private String TAG = "CD.QueryResultActivity";
    private ArrayList<SerachResultItemModel> mItems;
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

        /*
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

         */
//        NavController navController = Navigation.findNavController(this, R.id.main_content);
//
//        AppBarConfiguration appBarConfiguration =
//                new AppBarConfiguration.Builder(navController.getGraph()).build();
//        Toolbar toolbar = findViewById(R.id.toolbar);;
//
//        NavigationUI.setupWithNavController(
//                toolbar, navController, appBarConfiguration);
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


    /*public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");*/
    // Inflate the menu; this adds items to the action bar if it is present.
    /* getMenuInflater().inflate(R.menu.menu_option, menu); */

//        MenuItem settings = menu.findItem(R.id.action_settings);
//        settings.setOnMenuItemClickListener(OnMenuItemClickListener);

    // Associate searchable configuration with the SearchView
        /*SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));*/

    //searchView.setSubmitButtonEnabled(true);
        /*return true;
    }*/

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

    /*
        We could use navigation UI to go to destinations (fragments).
        See https://developer.android.com/guide/navigation/navigation-ui
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Bundle bundle = null;

        Log.d(TAG, "Action bar item pressed!!");

        if(id == R.id.search){
            Log.d(TAG, "Search button pressed!!");
            onSearchRequested();
        }

        return super.onOptionsItemSelected(item);
    }
}