package com.crossdrives.msgraph;

import static com.example.crossdrives.R.string.section_me;

public class SnippetCategory <T>
{
    static final SnippetCategory<MSGraphMeService> meSnippetCategory
            = new SnippetCategory<>(section_me, create(MSGraphMeService.class));

    final String mSection;
    final T mService;

    SnippetCategory(int sectionId, T service) {
        mSection = SnippetApp.getApp().getString(sectionId);
        mService = service;
    }

    private static <T> T create(Class<T> clazz) {
        return SnippetApp.getApp().getRetrofit().create(clazz);
    }
}
