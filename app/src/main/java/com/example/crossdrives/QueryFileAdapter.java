package com.example.crossdrives;

import android.content.Context;
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
//        mViewLoading = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.query_item_loading, parent, false);
        final ViewHolder holder;

//        if (viewType == VIEW_TYPE_ITEM) {
            view = mViewItem;
//        }
//        else {
//            Log.d(TAG, "----viewType is loading----");
//            view = mViewLoading;
//        }
//        Log.d(TAG, "view object:" + view);

        holder = new ViewHolder(view);

        //if(view != mViewLoading) {
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
        //}
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

            /*
             Checked?
             */
            if (item.isSelected()) {
                Log.d(TAG, "Set item [" + Integer.toString(position) + "]" + "CHECKED");
                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_error_outline_24);
                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_24);
                //holder.ItemView.setBackgroundResource(R.drawable.drawer_menu_item_bg_round_padded);
                holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_result_list_bg_selected));
                holder.iv_item_pic.setImageResource(R.drawable.ic_baseline_check_24);
                //mContext.getTheme().resolveAttribute(R.attr.colorQueryListItemPicBGChecked, tv, true);
                holder.iv_item_pic.setBackground(mContext.getDrawable(R.drawable.query_list_item_pic_background));

            }
            else {
                Log.d(TAG, "Set item [" + Integer.toString(position) + "]" + "UNCHECKED");
                //holder.ivCheckBox.setImageResource(R.drawable.ic_baseline_check_box_outline_blank_24);
                //holder.ivCheckBox.setImageResource(R.drawable.query_list_item_background_idle);
                //holder.ivCheckBox.setBackgroundResource(0);
                //holder.ivCheckBox.setBackgroundResource(R.drawable.query_list_item_background_idle);
                //holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_list_item_background_idle));
                holder.ItemView.setBackground(mContext.getDrawable(R.drawable.query_list_item_bg_state));
                holder.iv_item_pic.setImageResource(R.drawable.ic_outline_folder_24);
                holder.iv_item_pic.setBackground(null);
            }
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
//        if(mItems.get(position) == null)
//        {
//            type = VIEW_TYPE_LOADING;
//            Log.d(TAG, "View is loading");
//        }
//
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
    public void setOverflowIconVisible(boolean visible){


        if(visible == true)
            mOverFlowIconVisible = true;
        else
            mOverFlowIconVisible = false;
    }
}
