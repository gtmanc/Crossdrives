package com.crossdrives.cdfs.allocation;

public interface ICallBackAllocationFetch<Result> {
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
