package com.example.crossdrives;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class QueryFileAdapter extends RecyclerView.Adapter<QueryFileAdapter.ViewHolder> implements View.OnLongClickListener{
    private String TAG = "CD.QueryFileAdapter";
    List<ItemModelBase> mItems;
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    static private View mViewItem = null, mViewLoading = null;
    private OnItemClickListener mClickListener;
    boolean mCheckBoxVisible = false;

    public QueryFileAdapter(List<ItemModelBase> Items){mItems = Items;}

    //An interface for the caller activity
    public static interface OnItemClickListener {
        void onItemClick(View view , int position);

        void onItemLongClick(View view , int position);

        void onImageItemClick(View view , int position);
    }

    @NonNull
    @Override
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
        } else {
            Log.d(TAG, "viewType is loading");
            view = mViewLoading;
        }

        holder = new ViewHolder(view);

        if(view != mViewLoading) {
            holder.ItemView.setOnClickListener(this.itemOnClickListener);
            holder.ivMore.setOnClickListener(this.ImageMoreClickListener);
            holder.ItemView.setOnLongClickListener(this);
//            holder.ItemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    int position = holder.getAdapterPosition();
//                    Log.d(TAG, "[ItemView.OnClickListener] position: " + position);
//
//                    ItemModelBase item = mItems.get(position);
//                    Toast.makeText(view.getContext(), "你点击了View" + item.getName(), Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            holder.ItemView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    return false;
//                }
//            });
//
//            holder.Image.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    int position = holder.getAdapterPosition();
//                    Log.d(TAG, "[Image.OnClickListener] position:" + position);
//                    ItemModelBase item = mItems.get(position);
//                    Toast.makeText(view.getContext(), "你点击了图片" + item.getName(), Toast.LENGTH_SHORT).show();
//                }
//            });
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

        if (holder.ivCheckBox != null) {
            ItemModelBase item = mItems.get(position);
            //set position to tag in view so that we know the position when click listener is called
            holder.ItemView.setTag(position);
            holder.ivMore.setTag(position);
            //holder.ivCheckBox.setImageResource(item.getImageId());
            //holder.ivCheckBox = (ImageView) convertView.findViewById(R.id.iv_check_box);
            holder.tvName.setText(item.getName());

            /*
                 Show the check box?
             */
            holder.ivCheckBox.setVisibility(View.GONE);
            if(mCheckBoxVisible == true){
                holder.ivCheckBox.setVisibility(View.VISIBLE);
            }

            /*
             Checked?
             */
            if (item.isSelected()) {
                Log.d(TAG, "set item [" + Integer.toString(position) + "]" + "checked");
                //holder.ivCheckBox.setBackgroundResource(R.drawable.checked);
                holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_24);
            }
            else
                //holder.ivCheckBox.setBackgroundResource(R.drawable.check);
                holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_outline_blank_24);
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
        ImageView ivCheckBox, ivMore;
        TextView tvName;
        ProgressBar progressBar = null;

        public ViewHolder(View view) {
            super(view);
            Log.d(this.TAG, "[ViewHolder]: enter ");

            if(view == mViewItem) {
                ItemView = view;
                ivCheckBox = view.findViewById(R.id.iv_check_box);
                ivMore = view.findViewById(R.id.iv_more_vert);
                tvName = view.findViewById(R.id.tv_item_name);
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
        Log.d(TAG, "[getItemViewType]:position=" + position);
        if(mItems.get(position) == null)
        {
            type = VIEW_TYPE_LOADING;
            Log.d(TAG, "View is loading");
        }

        return type;
    }

//public class QueryFileAdapter extends BaseAdapter {
//    private String TAG = "CD.QueryFileAdapter";
//    List<ItemModelBase> mItems;
//    LayoutInflater mInflater;
//    boolean mCheckBoxVisible = false;
//
//    public QueryFileAdapter(Activity activity, List<ItemModelBase> Items) {
//        mItems = Items;
//        mInflater = activity.getLayoutInflater();
//    }
//
//    @Override
//    public int getCount() {
//        return mItems.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return position;
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        ViewHolder holder = null;
//
//        Log.d(TAG, "[getView]:position=" + position +"convertView=" + convertView);
//
////        if(position == (getCount()-1))
////            Log.d(TAG, "Reach end of list");
//
//        if (convertView == null){
//
//            convertView = mInflater.inflate(R.layout.list_view_item, parent, false);
//
//            holder = new ViewHolder();
//
//            holder.tvItemName = (TextView)convertView.findViewById(R.id.tv_item_name);
//            holder.ivCheckBox = (ImageView) convertView.findViewById(R.id.iv_check_box);
//
//            convertView.setTag(holder);
//        }
//        else{
//            holder = (ViewHolder)convertView.getTag();
//        }
//
//        ItemModelBase model = mItems.get(position);
//        /*
//         Show the check box?
//         */
//        holder.ivCheckBox.setVisibility(View.GONE);
//        if(mCheckBoxVisible == true){
//            holder.ivCheckBox.setVisibility(View.VISIBLE);
//        }
//
//        /*
//         Checked?
//         */
//        if (model.isSelected())
//            holder.ivCheckBox.setBackgroundResource(R.drawable.checked);
//
//        else
//            holder.ivCheckBox.setBackgroundResource(R.drawable.check);
//
//        holder.tvItemName.setText(model.getName());
//
//        return convertView;
//    }
//
//
//    class ViewHolder{
//
//        TextView tvItemName;
//        ImageView ivCheckBox;
//    }
//
//    public void updateRecords(List<ItemModelBase> users){
//        this.mItems = users;
//
//        notifyDataSetChanged();
//    }
//
    public void setCheckBoxVisible(boolean visible){


        if(visible == true)
            mCheckBoxVisible = true;
        else
            mCheckBoxVisible = false;
    }
}
