package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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

import java.util.List;

public class ChildListFragment extends QueryResultFragment{
    final String TAG = "CD.ChildListFragment";
    View mView;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        mView = view;
    }

    void navigateToOpenFolder(View view, CdfsItem[] itemArray){
        NavController navController = Navigation.findNavController(view);
        navController.navigate(ChildListFragmentDirections.navigateToMoveItemWorkflowGraph(itemArray));
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        Log.d(TAG, "onOptionsItemSelected");

        //Because we only have a action button (close Button) is action bar, so simply go back to previous screen (query result screen)
        NavController navController = Navigation.findNavController(mView);
        if (!navController.popBackStack()) {
            Log.w(TAG, "no stack can be popup!");
        }
        return super.onOptionsItemSelected(item);
    }
}
