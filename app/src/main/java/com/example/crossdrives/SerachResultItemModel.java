package com.example.crossdrives;

public class SerachResultItemModel {

    boolean isSelected;
    String mName;
    String mId;

    public SerachResultItemModel(boolean isSelected, String Name, String id) {
        this.isSelected = isSelected;
        this.mName = Name;
        mId = id;
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

    public String getID() {
        return mId;
    }

    public void setName(String Name) {
        this.mName = Name;
    }

}
