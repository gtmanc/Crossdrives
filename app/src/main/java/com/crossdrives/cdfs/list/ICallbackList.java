package com.crossdrives.cdfs.list;

public interface ICallbackList <Result> {
    void onSuccess(Result result);

    void onFailure(Throwable throwable);

    void onCompleteExceptionally(Result result, java.util.List<com.crossdrives.cdfs.allocation.Result> casues);

}
