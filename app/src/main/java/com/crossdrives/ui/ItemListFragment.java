package com.crossdrives.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ItemListFragment extends QueryResultFragment{

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        globalVm.getMoveItemStateLd().observe(this, moveItemStateObserver);
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }
}
