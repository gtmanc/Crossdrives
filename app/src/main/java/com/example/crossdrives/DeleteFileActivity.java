package com.example.crossdrives;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class DeleteFileActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    private String TAG = "CD.DeleteFileActivity";
    Intent mIntent = new Intent();
        List<SerachResultItemModel> mItems;
    DeleteFileAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_query_result);
        setContentView(R.layout.activity_delete_file);

//        NavController navController = Navigation.findNavController(this, R.id.main_content);
//        DrawerLayout drawerLayout= findViewById(R.id.drawer_layout);
//        Log.d(TAG, "Set appBarConfiguration...");
//        AppBarConfiguration appBarConfiguration =
//                new AppBarConfiguration.Builder(navController.getGraph()).setOpenableLayout(drawerLayout).build();
//
//        Toolbar toolbar = findViewById(R.id.dfa_toolbar);
//        Log.d(TAG, "Set setupWithNavController...");
//        NavigationUI.setupWithNavController(
//                toolbar, navController, appBarConfiguration);
//
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_content);
        NavController navController = navHostFragment.getNavController();
        NavigationView navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);
//        Log.d(TAG, "Done...");
//        if (savedInstanceState == null) {
//            DeleteFileFragment fragment = new  DeleteFileFragment();
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .add(R.id.main_content, fragment)
//                    .commit();
//        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main_drawer, menu);
        return true;
    }
    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //mIntent.putExtra("SelectedFiles", mList.get(position));
            // Toast 快顯功能 第三個參數 Toast.LENGTH_SHORT 2秒  LENGTH_LONG 5秒
            //Toast.makeText(QueryResultActivity.this,"點選第 "+(position +1) +" 個 \n內容：" + mList.get(position), Toast.LENGTH_SHORT).show();

            SerachResultItemModel model = mItems.get(position);

            if (model.isSelected())
                model.setSelected(false);

            else
                model.setSelected(true);

            mItems.set(position, model);

            //now update adapter
            mAdapter.updateRecords(mItems);
            setResult(RESULT_OK, mIntent);
        }
    };

    @Override
    public boolean onNavigationItemSelected(MenuItem item){
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.home) {
            Log.d(TAG, "Home selected!");
            NavController navController = Navigation.findNavController(this, R.id.main_content);
            NavigationUI.onNavDestinationSelected(item, navController);
        }else if(id == R.id.drawer_menu_item_two){
            Log.d(TAG, "nav_item_two selected!");
        }else{
            Log.d(TAG, "Unknown selected!");
        }
        return true;
    }
}
