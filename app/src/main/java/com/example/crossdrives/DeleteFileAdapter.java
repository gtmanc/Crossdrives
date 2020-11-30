package com.example.crossdrives;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeleteFileAdapter extends BaseAdapter {
    private String TAG = "CD.DeleteFileAdapter";
    List<ItemModelBase> mItems;
    LayoutInflater mInflater;
    ;
    public DeleteFileAdapter(Activity activity, List<ItemModelBase> Items) {
        mItems = Items;
        mInflater = activity.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeleteFileAdapter.ViewHolder holder = null;

        Log.d(TAG, "[getView]:position=" + position +"convertView=" + convertView);

        if(position == (getCount()-1))
            Log.d(TAG, "Reach end of list");

        if (convertView == null){

            convertView = mInflater.inflate(R.layout.list_view_item, parent, false);

            holder = new ViewHolder();

            holder.tvItemName = (TextView)convertView.findViewById(R.id.tv_item_name);
            holder.ivCheckBox = (ImageView) convertView.findViewById(R.id.iv_check_box);

            holder = (DeleteFileAdapter.ViewHolder)convertView.getTag();
        }

        ItemModelBase model = mItems.get(position);

        holder.tvItemName.setText(model.getName());

        if (model.isSelected())
            holder.ivCheckBox.setBackgroundResource(R.drawable.ic_baseline_check_box_24);

        else
            holder.ivCheckBox.setBackgroundResource(R.drawable.ic_baseline_check_box_outline_blank_24);

        return convertView;
    }


    class ViewHolder{

        TextView tvItemName;
        ImageView ivCheckBox;

    }

    public void updateRecords(List<ItemModelBase> users){
        this.mItems = users;

        notifyDataSetChanged();
    }
}
