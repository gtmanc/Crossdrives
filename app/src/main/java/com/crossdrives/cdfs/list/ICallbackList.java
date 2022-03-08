package com.crossdrives.cdfs.list;

public interface ICallbackList <Result> {
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
