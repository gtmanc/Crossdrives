package com.example.crossdrives;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> implements View.OnLongClickListener{
    private String TAG = "CD.AccountAdapter";

    private ArrayList<AccountListModel> mItems;

    public AccountAdapter(ArrayList<AccountListModel> items) {
        mItems = items;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @NonNull
    @org.jetbrains.annotations.NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @org.jetbrains.annotations.NotNull ViewGroup parent, int viewType) {
        final ViewHolder holder;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.account_list_item, parent, false);

        holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull AccountAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder " + "Position: " + position);
        AccountListModel item = mItems.get(position);
        holder.tvName.setText(item.getName());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View ItemView = null;
        TextView tvName;
        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            ItemView = itemView;
            tvName = itemView.findViewById(R.id.account_name);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
