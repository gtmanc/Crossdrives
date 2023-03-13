package com.example.crossdrives;

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
import androidx.recyclerview.widget.RecyclerView;

import com.sun.jna.platform.unix.X11;

import java.util.List;
public class QueryFileAdapter extends RecyclerView.Adapter<QueryFileAdapter.ViewHolder> implements View.OnLongClickListener{
    private String TAG = "CD.QueryFileAdapter";
    List<SerachResultItemModel> mItems;
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    static private View mViewItem = null, mViewLoading = null;
    private OnItemClickListener mClickListener;
    Context mContext;
    boolean mOverFlowIconVisible = true;

    public QueryFileAdapter(List<SerachResultItemModel> Items, Context context){mItems = Items; mContext = context;}

    //An interface for the caller activity
    public static interface OnItemClickListener {
        void onItemClick(View view , int position);

        void onItemLongClick(View view , int position);

        void onImageItemClick(View view , int position);
    }

    @NonNull
    @Override
    /*This method gets called each time system needs a concrete view object for showing the item.
    The view object created in the method could be reused such that this method is not called each time
    the notifyDataSetChange is called.
    */
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "[onCreateViewHolder] enter..");
        View view;
        mViewItem = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_view_item, parent, false);
        mViewLoading = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.query_item_loading, parent, false);
        final ViewHolder holder;

        if (viewType == VIEW_TYPE_ITEM) {
            view = mViewItem;
        }else {
            Log.d(TAG, "----viewType is loading----");
            view = mViewLoading;
        }
//        Log.d(TAG, "view object:" + view);

        holder = new ViewHolder(view);

        //Set click listener only for view item
        if(holder.progressBar == null) {
            holder.ItemView.setOnClickListener(this.itemOnClickListener);
            holder.ivMore.setOnClickListener(this.ImageMoreClickListener);
            holder.ItemView.setOnLongClickListener(this);
        }
        return holder;
    }


    private View.OnClickListener itemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null)
                Log.d(TAG, "itemOnClickListener: v.tag is null");

            mClickListener.onItemClick(v, (int) v.getTag());
        }
    };

    private View.OnClickListener ImageMoreClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() == null)
                Log.d(TAG, "ImageMoreClickListener: v.tag is null");
            mClickListener.onImageItemClick(v, (int) v.getTag());
        }
    };

    @Override
    public boolean onLongClick(View v) {
        Log.d(TAG, "onLongClick: view object:" + v);
        mClickListener.onItemLongClick(v, (int)v.getTag());

        /*
        Return true to indicate the event has been consumed
        https://stackoverflow.com/questions/5428077/android-why-does-long-click-also-trigger-a-normal-click
        */
        return true;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mClickListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "[onBindViewHolder]:position=" + position);

        if (holder.iv_item_pic != null) {
            Log.d(TAG, "view object:" + holder.ItemView);
            SerachResultItemModel item = mItems.get(position);
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
            Log.d(TAG, "item is folder? " + item.folder);
            holder.ItemView.setBackground(toBackground(item));
            holder.iv_item_pic.setImageResource(toLargeIconId(item));
            holder.iv_item_pic.setBackground(toLargeIconBackground(item));
//            if (item.isSelected()) {
//                Log.d(TAG, "Set item [" + Integer.toString(position) + "]" + "CHECKED");
//                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_error_outline_24);
//                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_24);
//                //holder.ItemView.setBackgroundResource(R.drawable.drawer_menu_item_bg_round_padded);
//                holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_result_list_bg_selected));
//                holder.iv_item_pic.setImageResource(R.drawable.ic_baseline_check_24);
//                //mContext.getTheme().resolveAttribute(R.attr.colorQueryListItemPicBGChecked, tv, true);
//                holder.iv_item_pic.setBackground(mContext.getDrawable(R.drawable.query_list_item_pic_background));
//
//            }
//            else {
//                Log.d(TAG, "Set item [" + Integer.toString(position) + "]" + "UNCHECKED");
//                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_outline_blank_24);
//                //holder.ivCheckBox.setImageResource(R.drawable.query_list_item_background_idle);
//                //holder.ivCheckBox.setBackgroundResource(0);
//                //holder.ivCheckBox.setBackgroundResource(R.drawable.query_list_item_background_idle);
//                //holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_list_item_background_idle));
//                holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_list_item_bg_state));
//                holder.iv_item_pic.setImageResource(R.drawable.baseline_insert_drive_file_24);
//                holder.iv_item_pic.setBackground(null);
//            }
        }else{
            Log.d(TAG, "holder is progress bar");
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private String TAG = "CD.ViewHolder";
        View ItemView = null;
        ImageView iv_item_pic, ivMore;
        TextView tvName, tvDate;
        ProgressBar progressBar = null;

        public ViewHolder(View view) {
            super(view);
            Log.d(this.TAG, "[ViewHolder]: enter ");

            if(view == mViewItem) {
                ItemView = view;
                iv_item_pic = view.findViewById(R.id.list_item_picture);
                ivMore = view.findViewById(R.id.iv_more_vert);
                tvName = view.findViewById(R.id.tv_item_name);
                tvDate = view.findViewById(R.id.tv_item_date);
            }else {
                Log.d(this.TAG, " set progress bar");
                progressBar = view.findViewById(R.id.item_load_progressBar);
            }
        }
    }

//    static class LoadingViewHolder extends RecyclerView.ViewHolder {
//        ProgressBar progressBar;
//
//        public LoadingViewHolder(View view) {
//            super(view);
//            progressBar = view.findViewById(R.id.item_load_progressBar);
//        }
//    }

    @Override
    public int getItemViewType(int position) {
        int type = VIEW_TYPE_ITEM;
//        //Log.d(TAG, "[getItemViewType]:position=" + position);
        if(mItems.get(position) == null)
        {
            type = VIEW_TYPE_LOADING;
//            Log.d(TAG, "View is loading");
        }
//
        return type;
    }

    //
    int toLargeIconId(SerachResultItemModel item){
        int id = R.drawable.baseline_insert_drive_file_24;
        if (item.isSelected()) {
            id = R.drawable.ic_baseline_check_24;
        }
        else if(item.folder){
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

    public void setOverflowIconVisible(boolean visible){


        if(visible == true)
            mOverFlowIconVisible = true;
        else
            mOverFlowIconVisible = false;
    }
}