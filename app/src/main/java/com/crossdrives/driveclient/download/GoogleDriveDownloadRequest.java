package com.crossdrives.driveclient.download;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
import com.crossdrives.driveclient.model.MediaData;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class GoogleDriveDownloadRequest extends BaseRequest implements IDownloadRequest {
    final String TAG = "GDC.GoogleDriveDownloadRequest";
    GoogleDriveClient mClient;
    String mID;
    int additionInt;
    public GoogleDriveDownloadRequest(GoogleDriveClient client, String id) { mClient = client; mID = id;
    }

    @Override
    public IDownloadRequest setAdditionInt(int i) {
        additionInt = i;
        return this;
    }

    @Override
    public void run(IDownloadCallBack<MediaData> callback) {
        Task<MediaData> task;


        task = Tasks.call(mClient.getExecutor(), new Callable<MediaData>() {
            @Override
            public MediaData call() throws Exception {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStream outputStream = bos;
                MediaData mediaData = new MediaData();

                //Log.d(TAG, "download: " + mID);
                String ex = null;
                mClient.getGoogleDriveService().files().get(mID)
                        .executeMediaAndDownloadTo(bos);
                mediaData.setOs(bos);
                mediaData.setAdditionInteger(additionInt);
                return mediaData;
            }
        });
        task.addOnSuccessListener(mClient.getExecutor(), new OnSuccessListener<MediaData>() {
            @Override
            public void onSuccess(MediaData mediaData) {
                callback.success(mediaData);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.failure(e.getMessage());
            }
        });
    }
}
