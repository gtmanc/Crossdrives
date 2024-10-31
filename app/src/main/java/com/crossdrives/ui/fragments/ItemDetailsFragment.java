package com.crossdrives.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.crossdrives.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ItemDetailsFragment extends Fragment {
    private final String TAG = "CD.ItemDetailsFragment";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.item_details_fragment, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.item_details_toolbar);

        NavController navController = Navigation.findNavController(view);
        DrawerLayout drawerLayout = getActivity().findViewById(R.id.layout_query_result_activity);
        //mDrawer = drawerLayout;
        //Do not use graph because we set the graph manually in QueryResultActivity's onCreate().
        //Use getGraph will lead to null graph once configuration changes
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(R.id.main_list_fragment).setOpenableLayout(drawerLayout).build();
        NavigationUI.setupWithNavController(
                toolbar, navController, appBarConfiguration);

        //fab object is null if called in onCreate
        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

    }
}
