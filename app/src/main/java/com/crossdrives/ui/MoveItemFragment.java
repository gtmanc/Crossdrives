package com.crossdrives.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.crossdrives.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MoveItemFragment extends QueryResultFragment {
    private String TAG = "CD.MoveItemFragment";
    private View mView = null;

    private Toolbar mBottomAppBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mView = view;
        requireActivity().getOnBackPressedDispatcher().addCallback(backPressCallback);

        mBottomAppBar = view.findViewById(R.id.bottomAppBar);
        mBottomAppBar.setVisibility(View.VISIBLE);
        mBottomAppBar.setOnMenuItemClickListener(onBottomAppBarMenuItemClickListener);
        mBottomAppBar.setText

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
    }

    OnBackPressedCallback backPressCallback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
            //Test for back stack
            globalVm.getMoveItemStateLd().getMoveItemState().isInProgress = false;
            NavController navController = Navigation.findNavController(mView);
            if(!navController.popBackStack(R.id.query_result_fragment, false)){
                Log.w(TAG, "no stack can be popup!");
            }

        }
    };

    private Toolbar.OnMenuItemClickListener onBottomAppBarMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            //YourActivity.this.someFunctionInYourActivity();
            Log.d(TAG, "Bottom app bar menu item action pressed!!");
            return true;
        }
    };

}
