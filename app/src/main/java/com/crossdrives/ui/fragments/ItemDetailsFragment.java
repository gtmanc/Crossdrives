package com.crossdrives.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Service;
import com.crossdrives.cdfs.details.Result;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.exception.PermissionException;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.ui.chart.piechart.Item;
import com.crossdrives.ui.chart.piechart.PieTextInCenter;
import com.example.crossdrives.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class ItemDetailsFragment extends Fragment {
    private final String TAG = "CD.ItemDetailsFragment";
    private CdfsItem[] mParentPath;
    private CdfsItem mItem;

    private View mProgressBar;

    private PieTextInCenter pie;
    private TextView tvName;
    private TextView tvSize;
    private TextView tvTimeCreated;
    private TextView tvTimeModified;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ItemDetailsFragmentArgs fragmentArgs;
        Bundle bundle = getArguments();
        fragmentArgs = com.crossdrives.ui.fragments.ItemDetailsFragmentArgs.fromBundle(bundle);
        mParentPath = fragmentArgs.getParentsPath();
        mItem = fragmentArgs.getCdfsItem();
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

        mProgressBar = view.findViewById(R.id.item_details_progressBar);
        pie = view.findViewById(R.id.item_details_piechart);
        tvName = view.findViewById(R.id.text_item_details_name);
        tvSize = view.findViewById(R.id.text_item_details_size);
        tvTimeCreated = view.findViewById(R.id.text_item_details_created_time);
        tvTimeModified = view.findViewById(R.id.text_item_details_modifed_time);

        Task<Result> task = null;
        Service service = CDFS.getCDFSService().getService();
        try {
            task =service.details(Arrays.asList(mParentPath), mItem);
        } catch (PermissionException | MissingDriveClientException e) {
            Log.w(TAG, e.getMessage());
            Log.w(TAG, e.getCause());
            //mProgressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(getActivity().getApplicationContext(), e.getMessage() + e.getCause(), Toast.LENGTH_LONG).show();
        }
        if(task != null){
//            ResultUpdater resultUpdater = new ResultUpdater();
//            task.addOnSuccessListener(resultUpdater.createMoveItemSuccessListener(notification)).
//                    addOnFailureListener(resultUpdater.createMoveItemFailureListener(notification));
            task.addOnSuccessListener(new OnSuccessListener<Result>() {
                @Override
                public void onSuccess(Result result) {
                    Collection<Item> items = buildPieItems(result);
//                    items.forEach((item)->{
//                        Log.d(TAG, "Title:" + item.title);
//                        Log.d(TAG, "Subtitle:" + item.subtitle);
//                        Log.d(TAG, "Percentage:" + item.percentage);
//
//                    });
                    pie.clearBeforeAddItems(items);
                    pie.invalidate();
                    pie.setVisibility(View.VISIBLE);
                    tvName.setText(result.name); tvName.setVisibility(View.VISIBLE);
                    tvSize.setText(result.size/1024 + getString(R.string.unit_item_details_size)); tvSize.setVisibility(View.VISIBLE);
                    tvTimeCreated.setText(result.timeCreated); tvTimeCreated.setVisibility(View.VISIBLE);
                    tvTimeModified.setText(result.timeModified); tvTimeModified.setVisibility(View.VISIBLE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            }).addOnCompleteListener(new OnCompleteListener<Result>() {
                @Override
                public void onComplete(@NonNull Task<Result> task) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            });
        }else{
//            Context context = SnippetApp.getAppContext();
//            notification.removeProgressBar();
//            notification.updateContentTitle(context.getString(R.string.notification_title_move_item_completed));
//            notification.updateContentText(context.getString(R.string.notification_content_move_item_complete_exceptionally));
        }
    }

    Collection<Item> buildPieItems(Result details){
        long totalSize = details.allocatedSize.values().stream().mapToLong((v) -> v).sum();
        return details.allocatedSize.entrySet().stream().map((set)->{
            Item item = new Item();
            item.title = set.getKey();
            //Special handle for folder
            if(mItem.isFolder()){ item.percentage = 1.0f;}
            else{item.percentage = (float) set.getValue()/totalSize;}
            item.subtitle = new Float(set.getValue()/1024).toString() + getString(R.string.unit_item_details_size);
            return item;
        }).collect(Collectors.toCollection(ArrayDeque::new));
    }
}
