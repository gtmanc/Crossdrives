package com.crossdrives.cdfs.remote;

public interface ICallBackLocker <Result>{

    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
