package com.crossdrives.cdfs.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.crossdrives.base.BaseItem;
import com.google.api.client.util.DateTime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CdfsItem extends BaseItem implements Parcelable {

    //drive item IDs in each user's drive which are mapped to the CDFS item
    ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, List<String>> getMap() {
        return map;
    }

    public void setMap(ConcurrentHashMap<String, List<String>> map) {
        this.map = map;
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(getName());
        parcel.writeString(getId());
        parcel.writeString(getPath());
        parcel.writeInt(isFolder()==true?1:0);
        parcel.writeMap(map);
    }

    public static final Creator<CdfsItem> CREATOR = new Creator<CdfsItem>() {
        @Override
        public CdfsItem createFromParcel(Parcel source) {
            return new CdfsItem();
        }

        @Override
        public CdfsItem[] newArray(int size) {
            return new CdfsItem[size];
        }
    };
}
