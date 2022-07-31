package com.example.crossdrives;

import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.OneDriveClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GlobalConstants {
    /*
     * Currently, this app supports two brand: Google and Microsoft.
     */
    static final int MAX_BRAND_SUPPORT = 2; //Google, Microsoft
    static final String BRAND_GOOGLE = SignInManager.BRAND_GOOGLE;
    static final String BRAND_MS = SignInManager.BRAND_MS;

    static List<String> BrandList= new ArrayList<>();
    public static HashMap<String, SignInManager> supporttedSignin= new HashMap<>();
    public static HashMap<String, Object> supporttedDriveClient = new HashMap<>();
    static{
        BrandList.add(BRAND_GOOGLE);
        BrandList.add(BRAND_MS);

        supporttedSignin.put(BRAND_GOOGLE, SignInGoogle.getInstance());
        supporttedSignin.put(BRAND_MS, SignInMS.getInstance());

        supporttedDriveClient.put(BRAND_GOOGLE, GoogleDriveClient.class);
        supporttedDriveClient.put(BRAND_MS, OneDriveClient.class);
    }

}
