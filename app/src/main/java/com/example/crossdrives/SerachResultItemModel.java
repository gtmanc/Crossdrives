package com.example.crossdrives;

import com.google.api.client.util.DateTime;

public class SerachResultItemModel {

    boolean isSelected;
    String mName;
    String mId;
    DateTime mDateTime;
    boolean folder; //attributes indicates whether the item is a folder or not.

    public SerachResultItemModel(boolean isSelected, String Name, String id, DateTime dt, boolean folder) {
        this.isSelected = isSelected;
        this.mName = Name;
        mId = id;
        mDateTime = dt;
        this.folder = folder;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getName() {
        return mName;
    }

    public DateTime getDateTime() {
        return mDateTime;
    }

    public String getID() {
        return mId;
    }

    public void setName(String Name) {
        this.mName = Name;
    }

    public void setDateTime(DateTime dt) { this.mDateTime = dt; }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }
}