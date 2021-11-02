package com.example.crossdrives;

import java.util.ArrayList;
import java.util.List;

public class GlobalConstants {
    /*
     * Currently, this app supports two brand: Google and Microsoft.
     */
    static final int MAX_BRAND_SUPPORT = 2; //Google, Microsoft
    static final String BRAND_GOOGLE = SignInManager.BRAND_GOOGLE;
    static final String BRAND_MS = SignInManager.BRAND_MS;

    static List<String> BrandList= new ArrayList<>();

    static{
        BrandList.add(BRAND_GOOGLE);
        BrandList.add(BRAND_MS);
    }

}
