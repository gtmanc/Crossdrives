package com.crossdrives.cdfs.allocation;

import com.crossdrives.driveclient.model.MediaData;

public interface ICompositeCallback {

    /*
        gets called when compositor requests a slice to composite.
        driveName:  name of the user drive
        id:         item ID in a user drive
        seq:        CDFS sequence number. start with 1.
     */
    public void onSliceRequested(String driveName, String id, int seq);

    /*
        Gets called when a slice has been composited. Do not call sliceRemove
     */
    public void onSliceCompleted(String driveName, int seq);

    public void onCompleted(String CompositedfFile);

    public void OnExceptionally(Throwable throwable);
}
