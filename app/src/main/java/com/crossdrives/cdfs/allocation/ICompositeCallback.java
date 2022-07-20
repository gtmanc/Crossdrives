package com.crossdrives.cdfs.allocation;

import com.crossdrives.driveclient.model.MediaData;

public interface ICompositeCallback {

    public void onSliceRequested(String driveName, String id, int seq);

    public void onComplete(String CompositedfFile);

    public void OnExceptionally(Throwable throwable);
}
