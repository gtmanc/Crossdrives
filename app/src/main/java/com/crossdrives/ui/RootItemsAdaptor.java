package com.crossdrives.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crossdrives.SerachResultItemModel;

import java.util.List;

public class RootItemsAdaptor extends ListAdapter<SerachResultItemModel, RootItemsAdaptor.ViewHolder> {
    Notifier mNotifier;
    boolean mOverFlowIconVisible = true;


    protected RootItemsAdaptor() {
        super(diffCallback);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }


    class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
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
        void onItemClick(View view , int position);

        void onItemLongClick(View view , int position);

        void onImageItemClick(View view , int position);

        void onCurrentListChanged(List<SerachResultItemModel> list);
    }

    public void setNotifier(Notifier notifier){mNotifier = notifier;}

    @Override
    public void onCurrentListChanged(@NonNull List<SerachResultItemModel> previousList, @NonNull List<SerachResultItemModel> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        mNotifier.onCurrentListChanged(currentList);
    }

    public void setOverflowIconVisible(boolean visible){
        if(visible == true)
            mOverFlowIconVisible = true;
        else
            mOverFlowIconVisible = false;
    }
}
