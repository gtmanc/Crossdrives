package com.example.crossdrives;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

public class QueryResultFragment extends Fragment implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener,
        DrawerLayout.DrawerListener{
    private String TAG = "CD.QueryResultFragment";
    DrawerLayout mDrawer = null;
    NavigationView mNavigationView;

    private DriveServiceHelper mDriveServiceHelper;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<ItemModelBase> mItems;
    private RecyclerView mRecyclerView = null;
    private QueryFileAdapter mAdapter;

    final String STATE_NORMAL = "state_normal";
    final String STATE_ITEM_SELECTION = "state_selection";
    private String mState = STATE_NORMAL;
    private int mSelectedItemCount = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mDriveServiceHelper = DriveServiceHelper.getInstance(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.query_result_fragment, container, false);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        DrawerLayout drawerLayout = view.findViewById(R.id.layout_query_result);
        mDrawer = drawerLayout;
        drawerLayout.addDrawerListener(this);

        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(navController.getGraph()).setOpenableLayout(drawerLayout).build();
        Toolbar toolbar = view.findViewById(R.id.qr_toolbar);

        NavigationUI.setupWithNavController(
                toolbar, navController, appBarConfiguration);

        mNavigationView = view.findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mNavigationView.getMenu().findItem(R.id.nav_item_hidden).setVisible(false);

        initialQuery(view);

        queryFile(view);
    }
    /*
        Initialization of query
     */
    private void initialQuery(final View v){
        Log.i(TAG, "initialQuery...");
        mRecyclerView = v.findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(getContext());
        //It seems to be ok we create a new layout manager ans set to the recyclarview.
        //It is observed each time null is got if view.getLayoutManager is called
        mRecyclerView.setLayoutManager(mLayoutManager);

        //be sure to register the listener after layout manager is set to recyclerview
        mRecyclerView.addOnScrollListener(onScrollListener);
    }
    /*
     *  First time query
     */
    private void queryFile(final View v){

        if (mDriveServiceHelper != null) {
            Log.i(TAG, "Querying for files.");

            //mProgressBar.setVisibility(View.VISIBLE);

            mDriveServiceHelper.resetQuery();
            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            //ListView listview = (ListView) findViewById(R.id.listview_query);


                            mItems = new ArrayList<>();
                            Log.i(TAG, "Number of files: " + f.size());
                            for (File file : fileList.getFiles()) {
                                //Log.d(TAG, "files name: " + file.getName());
                                mItems.add(new ItemModelBase(false, file.getName(), file.getId()));
                            }

                            mAdapter = new QueryFileAdapter(mItems);
                            mAdapter.setOnItemClickListener(itemClickListener);
                            mRecyclerView.setAdapter(mAdapter);

                            //listview.setAdapter(mAdapter);

                            //The files are ready to be shown. Dismiss the progress cycle
                            //mProgressBar.setVisibility(View.GONE);

                            //setResult(RESULT_OK, mIntent);
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
            Log.i(TAG, "Querying for files continue.");

            //mProgressBar.setVisibility(View.VISIBLE);

            //Insert a null item so that the adapter knows that progress bar needs to be shown to the user
            mItems.add(null);
            Log.i(TAG, "Notify inserted");
            mAdapter.notifyItemInserted(mItems.size() - 1);

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            int i = 0;
                            //now we are done with the query. take out the progress bar from the list
                            Log.i(TAG, "Notify removed");
                            mItems.remove(mItems.size() - 1);
                            mAdapter.notifyItemRemoved(mItems.size());

                            Log.i(TAG, "Number of files fetched: " + f.size());

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

                            //setResult(RESULT_OK, mIntent);
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
    private QueryFileAdapter.OnItemClickListener itemClickListener = new QueryFileAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Pressed!", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Short press item:" + position);
            Log.i(TAG, "Count of selected:" + mSelectedItemCount);
            ItemModelBase item = mItems.get(position);

            if (view == view.findViewById(R.id.iv_more_vert)) {
                Log.i(TAG, "More_vert pressed!");
            }

            if (mState == STATE_NORMAL) {
                //TODO: open detail of file
            } else {
                if (item.isSelected()) {
                    /*
                    click on the selected item, again exit item selection state
                    */
                    setItemChecked(item, position, false);
                    if (mSelectedItemCount == 0) {
                        mAdapter.setCheckBoxVisible(false);
                        mState = STATE_NORMAL;

                        //switchNormalActionBar();
                    }
                } else {
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
            Log.i(TAG, "Long press item:" + position);
            Log.i(TAG, "Count of selected:" + mSelectedItemCount);

            ItemModelBase item = mItems.get(position);

            if(mState == STATE_NORMAL) {
                /*
                Switch to item selection state
                 */
                setItemChecked(item, position, true);
                mAdapter.setCheckBoxVisible(true);
                mState = STATE_ITEM_SELECTION;

                //switchContextualActionBar();
            }else {
                if(item.isSelected()) {
                    /*
                    Long press on the selected item, exit item selection state
                    */
                    setItemChecked(item, position, false);
                    if(mSelectedItemCount == 0) {
                        mAdapter.setCheckBoxVisible(false);
                        mState = STATE_NORMAL;

                       // switchNormalActionBar();
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
            Log.i(TAG, "onImageItemClick:" + position);
        }
    };
    private void setItemChecked(ItemModelBase item, int position, boolean checked){

        if(checked == false && mSelectedItemCount <= 0) {
            Log.i(TAG, "No item should be unchecked!!");
            return;
        }

        item.setSelected(checked);
        if(checked == true)
            mSelectedItemCount++;
        else
            mSelectedItemCount--;
    }
    @Override
    public void onClick(View v) {

    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
//        Log.i(TAG, "onDrawerSlide: "+ slideOffset);
//        MenuItem item = mNavigationView.getCheckedItem();
//        if (item != null){
//            Log.d(TAG, "set item unchecked: "+ item.getItemId());
//            item.setChecked(false);
//        }

    }


    @Override
    public void onDrawerOpened(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        MenuItem item = mNavigationView.getCheckedItem();
        if(item != null){
            Log.d(TAG, "checked item: "+ item.getItemId());
            if(item.getItemId() == R.id.home) {
                NavDirections a = DeleteFileFragmentDirections.navigateToHome();
                NavHostFragment.findNavController(this).navigate(a);
            }
        }
        //It's unclear how to clear(reset) a checked item once it is checked.
        //A workaround is used: set to the hidden item so that we can avoid the unexpected transition
        mNavigationView.setCheckedItem(R.id.nav_item_hidden);
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(item.isChecked())
            Log.d(TAG, "checked!");

        //close drawer right here. Otherwise, the drawer is still there if screen is switched back from next one
        mDrawer.closeDrawers();

        //The screen transition will take place in callback onDrawerClosed. This is because we have to ensure that the
        //drawer is closed exactly before screen proceed to next one
        if (id == R.id.home) {
            Log.d(TAG, "Home selected!");
        }else if(id == R.id.nav_item_two){
            Log.d(TAG, "nav_item_two selected!");
        }else if(id == R.id.nav_item_hidden){
            Log.d(TAG, "nav_item_three selected!");
        }else{
            Log.d(TAG, "Unknown selected!");
        }
        return true;
    }
}
