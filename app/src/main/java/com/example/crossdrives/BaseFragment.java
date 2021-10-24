package com.example.crossdrives;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseFragment extends Fragment {
    final String BRAND_GOOGLE = "GDrive";
    final String BRAND_MS = "OneDrive";
    final int MAX_BRAND_SUPPORT = 2; //Google, Microsoft

    List<String> BrandList= new ArrayList<>();

    public BaseFragment() {
        BrandList.add(BRAND_GOOGLE);
        BrandList.add(BRAND_MS);
    }
}
