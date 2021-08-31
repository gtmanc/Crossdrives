package com.example.crossdrives;

import android.net.Uri;

import java.net.URL;

public class AccountListModel {
    private String mBrand;
    private String mName;
    private String mMail;
    private Uri mPhotourl;

    public AccountListModel(String brand, String name, String mail, Uri purl) {
        mBrand = brand;
        mName = name;
        mMail = mail;
        mPhotourl = purl;
    }
    public String getBrand(){return mBrand;}
    public String getName(){return mName;}
    public String getMail(){return mMail;}
    public Uri getPhotoUrl(){return mPhotourl;}
}
