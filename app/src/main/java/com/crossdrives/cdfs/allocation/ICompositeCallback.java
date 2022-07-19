package com.crossdrives.cdfs.allocation;

public interface ICompositeCallback {

    public void onSliceRequested(int seq);

    public void onComplete();

    public void OnExceptionally(Throwable throwable);
}
