package com.example.crossdrives;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

public class DeleteFileFragment extends Fragment implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener,
        DrawerLayout.DrawerListener{
    private String TAG = "CD.DeleteFileFragment";
    DrawerLayout mDrawer = null;
    int mMenuIDSelected = 0;

    private View mView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        View v = inflater.inflate(R.layout.delete_file_fragment, container, false);
        //v.findViewById(R.id.hit_me_button).setOnClickListener(this);

//        NavigationView navigationView = (NavigationView) v.findViewById(R.id.nav_view);
//        navigationView.setNavigationItemSelectedListener(this);
        return v;
        //return
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        mView = view;

//        NavController navController = Navigation.findNavController(view);
//        DrawerLayout drawerLayout= view.findViewById(R.id.layout_fragment_test);
//        mDrawer = drawerLayout;
//        drawerLayout.addDrawerListener(this);

//        AppBarConfiguration appBarConfiguration =
//                new AppBarConfiguration.Builder(navController.getGraph()).setOpenableLayout(drawerLayout).build();
//        Toolbar toolbar = view.findViewById(R.id.ft_toolbar);
//
//        NavigationUI.setupWithNavController(
//                toolbar, navController, appBarConfiguration);
//
//        NavigationView navigationView = view.findViewById(R.id.ft_nav_view);
//        navigationView.setNavigationItemSelectedListener(this);

    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {
           //Test for pop up back stack
           NavController navController = Navigation.findNavController(mView);

           if(!navController.popBackStack(R.id.query_result_fragment, true)){
               Log.w(TAG, "no stack can be popup!");
           }

        }
    };

    @Override
    public void onClick(View v) {
        NavDirections a = DeleteFileFragmentDirections.navigateToMasterAccount();
        NavHostFragment.findNavController(this).navigate(a);
    }
    @Override
    public boolean onNavigationItemSelected(MenuItem item){
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        mMenuIDSelected = item.getItemId();
        //close drawer right here. Otherwise, the drawer is still there if screen is switched back from next one
        mDrawer.closeDrawers();

        //The screen transition will take place in callback onDrawerClosed. This is because we have to ensure that the
        //drawer is closed exactly before screen proceed to next one
        if (id == R.id.home) {
            Log.d(TAG, "Home selected!");
//            NavDirections a = DeleteFileFragmentDirections.navigateToHome();
//            NavHostFragment.findNavController(this).navigate(a);
        }else if(id == R.id.drawer_menu_item_two){
            Log.d(TAG, "nav_item_two selected!");
        }else{
            Log.d(TAG, "Unknown selected!");
            mMenuIDSelected = 0;
        }
        return true;
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        //Log.d(TAG, "Drawer listener: drawer is slided...");
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        //Log.d(TAG, "Drawer listener: drawer is open...");
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {

    }

    @Override
    public void onDrawerStateChanged(int newState) {
        //Log.d(TAG, "Drawer listener: drawer state changed...");
    }
}