package com.crossdrives.msgraph;


import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class msgraphapi <T, Result> implements Callback<Result> {
    private String TAG = "CD.msgraphapi";
    private AbstractSnippet<T, Result> mItem;
    private final int ITEM_ARG = 1;

    public msgraphapi() {
        //SnippetContent.ITEMS.get(getArguments().getInt(ARG_ITEM_ID));
        mItem = (AbstractSnippet<T, Result>)SnippetContent.ITEMS.get(ITEM_ARG);

        // actually make the request
        mItem.request(mItem.mService, this);

    }

    @Override
    public void onResponse(Call<Result> call, Response<Result> response) {
        Log.d(TAG, "[onResponse] Code: " + response.code());
    }

    @Override
    public void onFailure(Call<Result> call, Throwable t) {
        Log.d(TAG, "[onFailure]: ");
    }
}
