package com.example.crossdrives;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.crossdrives.base.BaseItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.google.api.client.util.DateTime;

public class SerachResultItemModel {
    boolean isSelected;
    CdfsItem cdfsItem;

//    public SerachResultItemModel(boolean isSelected, String Name, String id, DateTime dt, boolean folder) {
//        this.isSelected = isSelected;
//        setName(Name);
//        setId(id);
//        setDateTime(dt);
//        setFolder(folder);
//    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public CdfsItem getCdfsItem() {
        return cdfsItem;
    }

    public void setCdfsItem(CdfsItem cdfsItem) {
        this.cdfsItem = cdfsItem;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}