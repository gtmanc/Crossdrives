package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.crossdrives.cdfs.model.CdfsItem;
import com.example.crossdrives.SerachResultItemModel;

public class ChildListFragment extends QueryResultFragment{
    final String TAG = "CD.ChildListFragment";
    View mView;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        mView = view;
    }

    @Override
    void onFolderItemClickNormalState(View view, SerachResultItemModel item){
        CdfsItem[] itemArray = treeOpener.getParentArray(true);
        //Concatenate the dir we will go to produce a complete dir for the need of the destination
        CdfsItem cdfsItem = item.getCdfsItem();
        itemArray[itemArray.length-1] = cdfsItem;
        navigateToOpenFolder(view, itemArray);
    }

    void navigateToOpenFolder(View view, CdfsItem[] itemArray){
        NavController navController = Navigation.findNavController(view);
        navController.navigate(ChildListFragmentDirections.navigateToMyself(itemArray));
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

    void onMenuItemDetailsSelected(NavController navController){
        navController.navigate(ChildListFragmentDirections.navigateToItemDetailsFragment());
    }
}
