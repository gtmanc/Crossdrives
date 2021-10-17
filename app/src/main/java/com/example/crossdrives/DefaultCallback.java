package com.example.crossdrives;

import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;

public class DefaultCallback <T> implements ICallback<T> {
    @Override
    public void success(T t) {

    }

    @Override
    public void failure(ClientException ex) {

    }
}
