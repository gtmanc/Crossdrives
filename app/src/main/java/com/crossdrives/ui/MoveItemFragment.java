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
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.crossdrives.cdfs.model.CdfsItem;
import com.example.crossdrives.R;
import com.example.crossdrives.SerachResultItemModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
        GlobalUiStateVm.MoveItemState state = globalVm.getMoveItemStateLd().getMoveItemState();
        int count = state.increaseBackstackEntryCount();
        Log.d(TAG, "Increased backstack entry count: " + count);
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

        Toolbar toolbar = view.findViewById(R.id.qr_toolbar);
//        toolbar.setTitle("Move item");
        toolbar.setNavigationOnClickListener(onNavIconClickListener);

//        FragmentManager fm = getActivity().getSupportFragmentManager();
//        Log.d(TAG, "Back stack entry count: " + fm.getBackStackEntryCount());

    }

    private View.OnClickListener onNavIconClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            handleOnUpButton();
        }
    };

    OnBackPressedCallback backPressCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            handleOnUpButton();
        }
    };

    private void handleOnUpButton(){
        final String DOWNWARD = "Downward";
        final String UPWARD = "Upward";
        boolean atTopMost = false;
        String action = DOWNWARD;

        GlobalUiStateVm.MoveItemState state = globalVm.getMoveItemStateLd().getMoveItemState();
        int backstackEntryCount = state.getBackstackEntryCount();
        Log.d(TAG, "handleOnBackPressed. Current backstack entry count: " + backstackEntryCount);

        CdfsItem[] itemArray = treeOpener.getParentArray(false);
        List<CdfsItem> list = new LinkedList<CdfsItem>(Arrays.asList(itemArray));

//            globalVm.getMoveItemStateLd().getMoveItemState().isInProgress = false;
        NavController navController = Navigation.findNavController(mView);
//            if(!navController.popBackStack(R.id.query_result_fragment, false)){
        if(atStartDest){
            Log.d(TAG, "We are at start dest.");
//                NavBackStackEntry backStackEntry = navController.getBackStackEntry(R.id.moveItemWorkflowGraph);
//                Log.w(TAG, "backStackEntry: " + backStackEntry);
//                state.MoveUpward = true;
        }

        if(backstackEntryCount == 1){Log.d(TAG, "Only one entry exists in back stack");}

        // Use a linkedList so that we can remove the last item easier later
        // https://stackoverflow.com/questions/2965747/why-do-i-get-an-unsupportedoperationexception-when-trying-to-remove-an-element-f
        list.remove(list.size()-1);
        Log.d(TAG, "Dump parent list:");
        list.stream().forEach((cdfsItem)->{
            Log.d(TAG, cdfsItem.getName());
        });

        // Determine the route
        // There are three cases we have to handle. If
        // 1. We are at root: exit workflow
        // 2. Topmost of the backstack: open new parent. The back stack count is decreased by 1
        // 3. All the others: pop up backstack. The back stack count is decreased by 1
        // The back stack count is always increased in onCreate()
        backstackEntryCount = state.decreaseBackstackEntryCount();
        if(list.isEmpty()) {
            Log.d(TAG, "We are at root. Exit Move item flow");
            exitWorkflow(navController, globalVm, state.srcDestId);

        }else if(backstackEntryCount == 0){
            Log.d(TAG, "Open new parent");
            navController.navigate(MoveItemFragmentDirections.navigateToMyselfPopupTo(list.toArray(new CdfsItem[0])));
        }else{
            if (!navController.popBackStack()) {Log.w(TAG, "no stack can be popup!");}
        }
    }

    private Toolbar.OnMenuItemClickListener onBottomAppBarMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            NavController navController = Navigation.findNavController(mView);
            GlobalUiStateVm.MoveItemState state = globalVm.getMoveItemStateLd().getMoveItemState();
            if(item.getItemId() == R.id.bottomAppBarItemMove){
                Log.d(TAG, "Bottom app bar: ok button is pressed.");
                exitWorkflow(navController, globalVm, state.srcDestId);
                NavBackStackEntry backStackEntry = navController.getBackStackEntry(state.srcDestId);//R.id.main_list_fragment, child_list_fragment

                backStackEntry.getSavedStateHandle().set(KEY_SELECTED_DEST, treeOpener.getParentArray(false));
            }else if(item.getItemId() == R.id.bottomAppBarItemCancel){
                Log.d(TAG, "Bottom app bar: cancel button is pressed.");
                exitWorkflow(navController, globalVm, state.srcDestId);
            }else{
                Log.w(TAG, "Bottom app bar: Unknown action item");
            }
            return true;
        }
    };
    private void exitWorkflow(NavController navController, GlobalUiStateVm stateVm, int serDestId ){
        //navController.navigate(MoveItemFragmentDirections.exitMoveWorkflow());
//        navController.navigate(serDestId, null, new NavOptions.Builder()
//                .setPopUpTo(serDestId, false, true)
//                .build());
        navController.popBackStack(serDestId, false);
        stateVm.getMoveItemStateLd().getMoveItemState().InProgress = false;
    }

    private void setMoveButtonBehavior(MenuItem item, boolean atStartDest){
        //if we are in the start destination, disable the Move button.
        if(atStartDest){
            //The Move button is set to enable by default
            item.setIcon(R.drawable.baseline_check_24_gray_out);
            item.setEnabled(false);
        }
    }

    private boolean distinguish(CdfsItem[] startDest, CdfsItem[] currentDest) {
        //sizes are equivalent?
        Log.d(TAG, "length of start dest: " + startDest.length);
        Log.d(TAG, "length of current dest: " + currentDest.length);
        Log.d(TAG, "1st ID of start dest: " + startDest[0].getId());
        Log.d(TAG, "1st of current dest: " + currentDest[0].getId());
        if (startDest.length != currentDest.length)
            return false;

        //Because we ensure the length of the two array are equivalent, so we can do the exhaustively check
        Iterator<CdfsItem> i1 = Arrays.stream(startDest).iterator();
        Iterator<CdfsItem> i2 = Arrays.stream(currentDest).iterator();
        boolean identical = true;
        while (i1.hasNext() && i2.hasNext()) {
            if (!i1.next().getId().equals(i2.next().getId())) {
                identical = false;
            }
        }
        return identical;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.d(TAG, "onSaveInstanceState:" + this + "LF state: " + this.getLifecycle().getCurrentState());
    }


}
