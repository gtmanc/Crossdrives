package com.crossdrives.msgraph;

import static com.example.crossdrives.R.string.section_me;

import android.util.Log;

public class SnippetCategory <T>
{
    private static String TAG = "CD.SnippetCategory";
    static final SnippetCategory<MSGraphMeService> meSnippetCategory
            = new SnippetCategory<>(section_me, create(MSGraphMeService.class));

    final String mSection;
    final T mService;

    SnippetCategory(int sectionId, T service) {
        mSection = SnippetApp.getApp().getString(sectionId);
        mService = service;
    }

    private static <T> T create(Class<T> clazz) {
        //Log.d(TAG, "create");
        return SnippetApp.getApp().getRetrofit().create(clazz);
    }
}
