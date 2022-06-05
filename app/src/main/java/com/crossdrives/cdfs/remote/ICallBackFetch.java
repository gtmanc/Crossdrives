package com.crossdrives.cdfs.remote;

public interface ICallBackFetch<Result> {
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
