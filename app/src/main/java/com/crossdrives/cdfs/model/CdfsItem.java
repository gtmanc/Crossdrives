package com.crossdrives.cdfs.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.api.client.util.DateTime;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CdfsItem implements Parcelable {
    //CDFS display name
    String name;

    //CDFS ID
    String id;

    //CDFS display path which contains the item. Or the parents. e.g. AAA\BBB\CCC
    String path;

    //Indicator whether the item is a folder(parent) or not
    boolean folder;

    //modified time
    DateTime dateTime;

    //drive item IDs in each user's drive which are mapped to the CDFS item
    ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public ConcurrentHashMap<String, List<String>> getMap() {
        return map;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMap(ConcurrentHashMap<String, List<String>> map) {
        this.map = map;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(id);
        parcel.writeString(path);
        parcel.writeInt(folder==true?1:0);
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
