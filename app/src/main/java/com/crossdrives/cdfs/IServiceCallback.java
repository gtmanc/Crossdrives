package com.crossdrives.cdfs;

public interface IServiceCallback <Result>{
    void onCompleted(Result result);

    void onCompletedExceptionally(Throwable throwable);
}
