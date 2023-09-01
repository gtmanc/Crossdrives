package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.crossdrives.cdfs.model.CdfsItem;
import com.example.crossdrives.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.Iterator;

public class MoveItemFragment extends QueryResultFragment {
    private String TAG = "CD.MoveItemFragment";
    private View mView = null;

    private Toolbar mBottomAppBar;

    private boolean atStartDest = false;

    public static String KEY_SELECTED_DEST = "key_selected_dest";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate:" + this + "LF state: " + this.getLifecycle().getCurrentState());
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        boolean atStartDest = false;

        mView = view;
        requireActivity().getOnBackPressedDispatcher().addCallback(backPressCallback);

        mBottomAppBar = view.findViewById(R.id.bottomAppBar);

        atStartDest = distinguish( globalVm.getMoveItemStateLd().getMoveItemState().startDest,
                treeOpener.getParentArray(false));
        this.atStartDest = atStartDest;
        setMoveButtonBehavior(mBottomAppBar.getMenu().findItem(R.id.bottomAppBarItemMove),
              atStartDest);
        mBottomAppBar.setVisibility(View.VISIBLE);
        mBottomAppBar.setOnMenuItemClickListener(onBottomAppBarMenuItemClickListener);

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
    }

    OnBackPressedCallback backPressCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            //Test for back stack
//            globalVm.getMoveItemStateLd().getMoveItemState().isInProgress = false;
            NavController navController = Navigation.findNavController(mView);
//            if(!navController.popBackStack(R.id.query_result_fragment, false)){
            if(atStartDest){
                Log.d(TAG, "We are at start dest. exit moveItem flow.");
                globalVm.getMoveItemStateLd().getMoveItemState().isInProgress = false;
            }

            if(!navController.popBackStack()){
                Log.w(TAG, "no stack can be popup!");
            }

        }
    };

    private Toolbar.OnMenuItemClickListener onBottomAppBarMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            NavController navController = Navigation.findNavController(mView);
            if(item.getItemId() == R.id.bottomAppBarItemMove){
                Log.d(TAG, "Bottom app bar: ok button is pressed.");
                exitWorkflow(navController, globalVm);
                NavBackStackEntry backStackEntry = navController.getBackStackEntry(R.id.query_result_fragment);
                Log.d(TAG, "backStackEntry: " + backStackEntry.getId());
                backStackEntry.getSavedStateHandle().set("KEY_SELECTED_DEST", treeOpener.getParentArray(false));
            }else if(item.getItemId() == R.id.bottomAppBarItemCancel){
                Log.d(TAG, "Bottom app bar: cancel button is pressed.");
                exitWorkflow(navController, globalVm);
            }else{
                Log.w(TAG, "Bottom app bar: Unknown action item");
            }
            return true;
        }
    };
    private void exitWorkflow(NavController navController, GlobalUiStateVm stateVm){
        navController.navigate(MoveItemFragmentDirections.exitMoveWorkflow());
        stateVm.getMoveItemStateLd().getMoveItemState().isInProgress = false;
    }

    private void setMoveButtonBehavior(MenuItem item, boolean atStartDest){
        //if we are in the start destination, disable the Move button.
        if(atStartDest){
            //The Move button is set to enable by default
            item.setIcon(R.drawable.baseline_check_24_gray_out);
            item.setEnabled(false);
        }
    }

    private boolean distinguish(CdfsItem[] startDest, CdfsItem[] currentDest){
        //sizes are equivalent?
        Log.d(TAG, "length of start dest: " + startDest.length);
        Log.d(TAG, "length of current dest: " + currentDest.length);
        Log.d(TAG, "1st ID of start dest: " + startDest[0].getId());
        Log.d(TAG, "1st of current dest: " + currentDest[0].getId());
        if(startDest.length != currentDest.length)
            return false;

        //Because we ensure the length of the two array are equivalent, so we can do the exhaustively check
        Iterator<CdfsItem> i1 = Arrays.stream(startDest).iterator();
        Iterator<CdfsItem> i2 = Arrays.stream(currentDest).iterator();
        boolean identical = true;
        while(i1.hasNext() && i2.hasNext()){
            if(!i1.next().getId().equals(i2.next().getId())){identical = false;}
        }
        return  identical;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.d(TAG, "onSaveInstanceState:" + this + "LF state: " + this.getLifecycle().getCurrentState());
    }
}
