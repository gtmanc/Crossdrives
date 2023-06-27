package com.example.crossdrives;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Service;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.driveclient.model.File;
import com.crossdrives.msgraph.SnippetApp;
import com.crossdrives.ui.QueryResultFragmentDirections;
import com.crossdrives.ui.helper.CreateFolderDialogBuilder;
import com.crossdrives.ui.helper.CreateFolderDialogResultResolver;
import com.crossdrives.ui.listener.ProgressUpdater;
import com.crossdrives.ui.listener.ResultUpdater;
import com.crossdrives.ui.notification.Notification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

//Good article for development of recycler view:
// From an expert: https://developer.android.com/guide/topics/ui/layout/recyclerview
// Google: https://www.journaldev.com/24041/android-recyclerview-load-more-endless-scrolling

public class QueryResultActivity extends AppCompatActivity implements DrawerLayout.DrawerListener{
    final String STATE_NORMAL = "state_normal";
    final String STATE_ITEM_SELECTION = "state_selection";

    private String TAG = "CD.QueryResultActivity";
    private ArrayList<SerachResultItemModel> mItems;
    //private QueryFileAdapter mAdapter;
    private Intent mIntent;
    private int mPreLast = 0;
    //private ProgressBar mProgressBar;
    private DriveServiceHelper mDriveServiceHelper;
    private Activity mActivity;
    private String mState = STATE_NORMAL;
    private int mSelectedItemCount = 0;
    private RecyclerView.LayoutManager layoutManager;
    NavController navController;
    Toolbar mToolbar_normal;
    Toolbar mToolbar_contextual;

    DrawerLayout drawerLayout;

    private BottomSheetBehavior bottomSheetBehavior;

    NavigationView mNavigationView, mBottomNavigationView;

    static final public String KEY_PARENT_PATH = "parentPath";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_query_result);

        // Crashes if Navigation.findNavController is used because androidx.fragment.app.FragmentContainerView
        // is used as the layout to contain contents of main body of screen.
        navController =
                //Navigation.findNavController(this, R.id.main_content);
                NavHostFragment.findNavController(getSupportFragmentManager().findFragmentById(R.id.main_content));
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PARENT_PATH, "Root");
        navController.setGraph(R.navigation.nav_graph, bundle);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(onFabClick);

        drawerLayout = findViewById(R.id.layout_query_result_activity);
        drawerLayout.addDrawerListener(this);
        /*Toolbar tooBar = findViewById(R.id.qr_toolbar);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(navController.getGraph()).setDrawerLayout(drawerLayout).build();
        NavigationUI.setupWithNavController(
                tooBar, navController, appBarConfiguration);*/

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(OnNavigationItemSelectedListener);
        mNavigationView.getMenu().findItem(R.id.nav_item_hidden).setVisible(false);
        View hv = mNavigationView.getHeaderView(0);
        hv.setOnClickListener(onHeaderClick);

//        mBottomNavigationView = findViewById(R.id.bottomNavigationView);
//        mBottomNavigationView.setNavigationItemSelectedListener(OnBottomNavItemSelectedListener);

        View bottomSheet = findViewById(R.id.bottomNavigationView);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);//this one has been set to true in layout
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

    }

    NavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

//			if(item.isChecked())
//				Log.d(TAG, "checked!");

            //close drawer right here. Otherwise, the drawer is still there if screen is switched back from next one
            drawerLayout.closeDrawers();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            //The screen transition will take place in callback onDrawerClosed. This is because we have to ensure that the
            //drawer is closed exactly before screen proceed to next one
            if (id == R.id.drawer_menu_item_master_account) {
                Log.d(TAG, "Master account selected!");
            }else if(id == R.id.drawer_menu_item_two){
                Log.d(TAG, "nav_item_two selected!");
            }else if(id == R.id.nav_item_hidden){
                Log.d(TAG, "nav_item_three selected!");
            }else{
                Log.d(TAG, "Unknown selected!");
            }
            return true;
        }
    };

    View.OnClickListener onHeaderClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG, "header is clicked. View: " + v);
            //mCountPressDrawerHeader++;

            NavDirections a = com.crossdrives.ui.QueryResultFragmentDirections.navigateToSystemTest();
            navController.navigate(a);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }};

    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
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

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        MenuItem item = mNavigationView.getCheckedItem();
        Log.d(TAG, "navigate to: ");
        if(item != null){
            if(item.getItemId() == R.id.drawer_menu_item_master_account) {
                Log.d(TAG, "Master account fragment");
//                QueryResultFragmentDirections.NavigateToMasterAccount action =
//                        QueryResultFragmentDirections.navigateToMasterAccount();
//                action.setMyArg(100);
                NavDirections a = QueryResultFragmentDirections.navigateToMasterAccount(null);
                Navigation.findNavController(this, R.id.main_content).navigate(a);
            }
            else if(item.getItemId() == R.id.drawer_menu_item_two){
                Log.d(TAG, "delete file fragment");
                NavDirections a = QueryResultFragmentDirections.navigateToDeleteFile();
                Navigation.findNavController(this, R.id.main_content).navigate(a);
            }
            else{
                Log.d(TAG, "Oops, unknown ID");
            }
        }
        else{
            Log.w(TAG, "drawer checked item is null");
        }
        //It's unclear how to clear(reset) a checked item once it is checked.
        //A workaround is used: set to the hidden item so that we can avoid the unexpected transition
        mNavigationView.setCheckedItem(R.id.nav_item_hidden);
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    View.OnClickListener onFabClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG, "fab is clicked");

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//			Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), FABOptionDialog.class);
//			//intent.putExtra("Brand", SignInManager.BRAND_MS);
//			//mStartForResult.launch(intent);
//			FABOptionAlertDialog dialog = new FABOptionAlertDialog();
//			dialog.show(getParentFragmentManager(), "FABOptionAlertDialog");
        }
    };
}