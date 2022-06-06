package com.crossdrives.cdfs.allocation;

public interface ICallBackMapFetch<Result> {
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
