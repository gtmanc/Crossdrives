package com.crossdrives.ui.document;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Service;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.exception.PermissionException;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.ui.listener.ProgressUpdater;
import com.crossdrives.ui.listener.ResultUpdater;
import com.crossdrives.ui.notification.Notification;
import com.example.crossdrives.R;
import com.crossdrives.ui.model.Item;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class Open{
    static final String TAG = "CD.Open";

    static public boolean download(Activity activity, Item item, CdfsItem parent){
        boolean result;
        Context context = activity.getApplicationContext();
        Log.d(TAG, "Start to download file: " + item.getCdfsItem().getName());
        Toast.makeText(context, context.getString(R.string.toast_action_taken_download_start), Toast.LENGTH_LONG).show();
        //Log.d(TAG, "File ID: " + item.mId);
        //TODO: open detail of file
        Notification notification
                = new Notification(Notification.Category.NOTIFY_DOWNLOAD, R.drawable.ic_baseline_cloud_circle_24);
        notification.setContentTitle(context.getString(R.string.notification_title_downloading));
        notification.setContentText(context.getString(R.string.notification_content_default));
        notification.build();
        ResultUpdater resultUpdater = new ResultUpdater();
        OnSuccessListener<String> successListener = resultUpdater.createDownloadSuccessListener(notification);//createDownloadSuccessListener();
        OnFailureListener failureListener = resultUpdater.createDownloadFailureListener(notification);
        IDownloadProgressListener downloadProgressListener = new ProgressUpdater().createDownloadListener(notification);
        Service service = CDFS.getCDFSService().getService();
        result = true;
        if (service != null) {
            service.setDownloadProgressListener(downloadProgressListener);
        }
        try {
            service.download(item.getCdfsItem().getId(), parent).addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        } catch (MissingDriveClientException | PermissionException e) {
            Toast.makeText(context, "file download failed! " + e.getMessage(), Toast.LENGTH_LONG).show();
            result = false;
        }

        return result;
    }


}
