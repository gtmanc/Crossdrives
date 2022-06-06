package com.crossdrives.cdfs.remote;

public interface IFetcherCallBack <Result>{
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
