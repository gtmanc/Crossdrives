package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import com.crossdrives.ui.document.OpenTree;
import com.crossdrives.ui.document.OpenTreeFactory;
import com.example.crossdrives.R;

public class MainListFragment extends QueryResultFragment{
    private String TAG = "CD.MainListFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        treeOpener = new ViewModelProvider(this, new OpenTreeFactory(parentList)).get(OpenTree.class);
        treeOpener.setListener(treeOpenListener);
//		treeOpener.getItems().observe(this, listChangeObserver)
        Log.d(TAG, "TreeOpen object: " + treeOpener);

        treeOpener.getItems().observe(this, list -> mAdapter.submitList(list));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

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

    }
}
