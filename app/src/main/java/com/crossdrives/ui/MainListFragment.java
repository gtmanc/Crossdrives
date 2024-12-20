package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import com.crossdrives.cdfs.model.CdfsItem;
import com.example.crossdrives.R;
import com.example.crossdrives.SerachResultItemModel;

public class MainListFragment extends QueryResultFragment{
    private String TAG = "CD.MainListFragment";
    private View mView = null;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;

        NavController navController = NavHostFragment.findNavController(this);
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.layout_query_result_activity);
        mDrawer = drawerLayout;

        Toolbar toolbar = view.findViewById(R.id.qr_toolbar);

        //Do not use graph because we set the graph manually in QueryResultActivity's onCreate().
        //Use getGraph will lead to null graph once configuration changes
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(R.id.main_list_fragment).setOpenableLayout(drawerLayout).build();

        //When using a fragment-owned app bar, Google recommends using the Toolbar APIs directly.
        //Do not use setSupportActionBar() and the Fragment menu APIs, which are appropriate only for activity-owned app bars.
        //https://developer.android.com/guide/fragments/appbar#fragment
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

        //always register the callback because it is removed in onPause
        requireActivity().getOnBackPressedDispatcher().addCallback(callback);

    }

    @Override
    void onFolderItemClickNormalState(View view, SerachResultItemModel item){
        CdfsItem[] itemArray = treeOpener.getParentArray(true);
        //Concatenate the dir we will go to produce a complete dir for the need of the destination
        CdfsItem cdfsItem = item.getCdfsItem();
        itemArray[itemArray.length-1] = cdfsItem;
        navigateToOpenFolder(view, itemArray);
    }

    private void navigateToOpenFolder(View view, CdfsItem[] itemArray){
        Log.d(TAG, "navigateToItemShortPress");
        NavController navController = Navigation.findNavController(view);
        navController.navigate(MainListFragmentDirections.navigateToChildListFragment(itemArray));
    }

   @Override
    public void onMenuItemDetailsSelected(View view, CdfsItem item){
        CdfsItem[] itemArray = treeOpener.getParentArray(false);
        NavController navController = Navigation.findNavController(view);
        navController.navigate(MainListFragmentDirections.navigateToItemDetailsFragment(itemArray, item));
    }
}



