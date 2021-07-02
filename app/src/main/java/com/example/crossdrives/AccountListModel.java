package com.example.crossdrives;

import java.net.URL;

public class AccountListModel {
    private String mBrand;
    private String mName;
    private String mMail;
    private URL mPhotourl;

    public AccountListModel(String brand, String name, String mail, URL purl) {
        mBrand = brand;
        mName = name;
        mMail = mail;
        mPhotourl = purl;
    }
    public String getBrand(){return mBrand;}
    public String getName(){return mName;}
    public String getMail(){return mMail;}
    public URL getPhotoUrl(){return mPhotourl;}
}
