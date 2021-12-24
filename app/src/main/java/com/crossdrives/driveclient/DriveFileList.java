package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;

public class DriveFileList {
    Object mNextPage;
    String mName;
    String mID;

    void setNextPage(Object page){mNextPage = page;}

    void setName(String name){mName = name;}

    void setID(String id){mID = id;}

    Object getNextPage(){return mNextPage;}

    void getN(Object page ){mNextPage = page;}

    void getNextPage(Object page ){mNextPage = page;}

}
