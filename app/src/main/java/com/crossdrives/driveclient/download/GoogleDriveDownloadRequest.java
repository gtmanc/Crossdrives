package com.crossdrives.driveclient.download;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
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
    public GoogleDriveDownloadRequest(GoogleDriveClient client, String id) { mClient = client; mID = id;
    }

    @Override
    //public void run(IDownloadCallBack<InputStream> callback) {
    public void run(IDownloadCallBack<OutputStream> callback) {
        Task<OutputStream> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<OutputStream>() {
            @Override
            public OutputStream call() throws Exception {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStream outputStream = bos;

                //Log.d(TAG, "download: " + mID);
                String ex = null;
                mClient.getGoogleDriveService().files().get(mID)
                            .executeMediaAndDownloadTo(bos);
                return bos;
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<OutputStream>() {
            @Override
            public void onSuccess(OutputStream outputStream) {
                callback.success(outputStream);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.failure(e.getMessage());
            }
        });
    }
}
