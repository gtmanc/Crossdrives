package com.crossdrives.msgraph;

public class msgraphapi <T, Result> {
    private AbstractSnippet<T, Result> mItem;
    private final int ITEM_ARG = 0;

    public msgraphapi() {
        //SnippetContent.ITEMS.get(getArguments().getInt(ARG_ITEM_ID));
        mItem = (AbstractSnippet<T, Result>)SnippetContent.ITEMS.get(ITEM_ARG);



    }
}
