package com.example.crossdrives;

import android.content.Context;
import android.util.Log;

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;

public class DefaultCallback <T> implements ICallback<T> {
    private String TAG = "CD.DefaultCallback";

    public DefaultCallback(final Context context) {

    }

    @Override
    public void success(T t) {
        Log.w(TAG, "Must be implemented");
    }

    @Override
    public void failure(ClientException ex) {

    }
}
