package com.crossdrives.cdfs.allocation;

public interface ICallBackFetch<Result> {
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
