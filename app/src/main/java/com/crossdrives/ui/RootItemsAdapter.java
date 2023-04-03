package com.crossdrives.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crossdrives.R;
import com.example.crossdrives.SerachResultItemModel;

import java.util.List;

public class RootItemsAdapter extends ListAdapter<SerachResultItemModel, RootItemsAdapter.ViewHolder> implements View.OnLongClickListener{
    private String TAG = "CD.RootItemsAdapter";
    Notifier mNotifier;
    boolean mOverFlowIconVisible = true;
    Context mContext;

    RootItemsAdapter myself = this;

    protected RootItemsAdapter(Context context) {
        super(diffCallback);
        mContext = context;
    }

    @NonNull
    @Override
    /*This method gets called each time system needs a concrete view object for showing the item.
    The view object created in the method could be reused such that this method is not called each time
    the notifyDataSetChange is called.
    */
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_view_item, parent, false);
        final RootItemsAdapter.ViewHolder holder = new RootItemsAdapter.ViewHolder(view);

        //Set click listener only for view item

            holder.ItemView.setOnClickListener(this.itemOnClickListener);
            holder.ivMore.setOnClickListener(this.ImageMoreClickListener);
            holder.ItemView.setOnLongClickListener(this);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "view object:" + holder.ItemView);
        List<SerachResultItemModel> list = getCurrentList();
        SerachResultItemModel item = list.get(position);
        //set position to tag in view so that we know the position when click listener is called
        holder.ItemView.setTag(position);
        holder.ivMore.setTag(position);
        //holder.ivCheckBox.setImageResource(item.getImageId());
        //holder.ivCheckBox = (ImageView) convertView.findViewById(R.id.iv_check_box);
        holder.tvName.setText(item.getName());
        if(item.getDateTime() == null) {
            Log.w(TAG, "DateTime is null");
        }else {
            holder.tvDate.setText(item.getDateTime().toString());
        }
            /*
                 Show the check box?
             */
//            holder.iv_item_pic.setImageResource(R.drawable.ic_outline_folder_24);
        holder.ivMore.setVisibility(View.GONE);
        if(mOverFlowIconVisible == true){
//                Log.d(TAG, "set check box visible");
//                holder.iv_item_pic.setImageResource(R.drawable.ic_checked);
            holder.ivMore.setVisibility(View.VISIBLE);
        }

        //Change entry background and large icon depending on the item state
        Log.d(TAG, "item is folder? " + item.isFolder());
        holder.ItemView.setBackground(toBackground(item));
        holder.iv_item_pic.setImageResource(toLargeIconId(item));
        holder.iv_item_pic.setBackground(toLargeIconBackground(item));
    }


    class ViewHolder extends RecyclerView.ViewHolder{
        private String TAG = "CD.ViewHolder";
        View ItemView = null;
        ImageView iv_item_pic, ivMore;
        TextView tvName, tvDate;
        ProgressBar progressBar = null;
        public ViewHolder(@NonNull View view) {
            super(view);
            Log.d(this.TAG, "[ViewHolder]: enter ");

            //if(view == mViewItem) {
                ItemView = view;
                iv_item_pic = view.findViewById(R.id.list_item_picture);
                ivMore = view.findViewById(R.id.iv_more_vert);
                tvName = view.findViewById(R.id.tv_item_name);
                tvDate = view.findViewById(R.id.tv_item_date);
//            }else {
//                Log.d(this.TAG, " set progress bar");
//                progressBar = view.findViewById(R.id.item_load_progressBar);
//            }
        }
    }

    static final DiffUtil.ItemCallback<SerachResultItemModel> diffCallback = new DiffUtil.ItemCallback<SerachResultItemModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull SerachResultItemModel oldItem, @NonNull SerachResultItemModel newItem) {
            // User properties may have changed if reloaded from the DB, but ID is fixed
            return oldItem.getID() == newItem.getID();
        }

        @Override
        public boolean areContentsTheSame(@NonNull SerachResultItemModel oldItem, @NonNull SerachResultItemModel newItem) {
            // NOTE: if you use equals, your object must properly override Object#equals()
            // Incorrectly returning false here will result in too many animations.
            return oldItem.equals(newItem);
        }
    };

    //An interface for the caller activity
    public static interface Notifier {
        void onItemClick(RootItemsAdapter adapter, View view , int position);

        void onItemLongClick(RootItemsAdapter adapter, View view , int position);

        void onImageItemClick(RootItemsAdapter adapter, View view , int position);

        void onCurrentListChanged(RootItemsAdapter adapter, List<SerachResultItemModel> list);
    }

    private View.OnClickListener itemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null)
                Log.d(TAG, "itemOnClickListener: v.tag is null");

            mNotifier.onItemClick(myself, v, (int) v.getTag());
        }
    };

    private View.OnClickListener ImageMoreClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null)
                Log.d(TAG, "ImageMoreClickListener: v.tag is null");
            mNotifier.onImageItemClick(myself, v, (int) v.getTag());
        }
    };

    @Override
    public boolean onLongClick(View v) {
        Log.d(TAG, "onLongClick: view object:" + v);
        mNotifier.onItemLongClick(myself, v, (int)v.getTag());

        /*
        Return true to indicate the event has been consumed
        https://stackoverflow.com/questions/5428077/android-why-does-long-click-also-trigger-a-normal-click
        */
        return true;
    }

    public void setNotifier(Notifier notifier){mNotifier = notifier;}

    @Override
    public void onCurrentListChanged(@NonNull List<SerachResultItemModel> previousList, @NonNull List<SerachResultItemModel> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        Log.d("TAG", "onCurrentListChanged called.");
        mNotifier.onCurrentListChanged(this, currentList);
    }

    public void setOverflowIconVisible(boolean visible){
        if(visible == true)
            mOverFlowIconVisible = true;
        else
            mOverFlowIconVisible = false;
    }

    int toLargeIconId(SerachResultItemModel item){
        int id = R.drawable.baseline_insert_drive_file_24;
        if (item.isSelected()) {
            id = R.drawable.ic_baseline_check_24;
        }
        else if(item.isFolder()){
            id = R.drawable.ic_outline_folder_24;
        }
        return id;
    }

    Drawable toLargeIconBackground(SerachResultItemModel item){
        Drawable drawable = null;
        if (item.isSelected()) {
            drawable = mContext.getDrawable(R.drawable.query_list_item_pic_background);
        }
        return drawable;
    }

    Drawable toBackground(SerachResultItemModel item){
        Drawable drawable = mContext.getDrawable(R.drawable.query_list_item_bg_state);
        if (item.isSelected()) {
            drawable = mContext.getDrawable(R.drawable.query_result_list_bg_selected);
        }
        return drawable;
    }
}
