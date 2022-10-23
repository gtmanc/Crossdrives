package com.crossdrives.ui.notification;

import android.content.Context;
import android.util.Log;

import com.crossdrives.cdfs.delete.Delete;
import com.crossdrives.cdfs.delete.IDeleteProgressListener;
import com.crossdrives.cdfs.download.Download;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.cdfs.upload.Upload;
import com.crossdrives.msgraph.SnippetApp;
import com.crossdrives.ui.notification.Notification;
import com.example.crossdrives.R;

import java.util.HashMap;

public class ProgressUpdater {
    final String TAG = "ProgressUpdater";

    HashMap<IUploadProgressListener, Notification> mNotificationsByUploadListener = new HashMap<>();
    HashMap<IDownloadProgressListener, Notification> mNotificationsByDownloadListener = new HashMap<>();
    Context context = SnippetApp.getAppContext();

    IUploadProgressListener createUploadListener(){
        Notification notification
                = new Notification(Notification.Category.NOTIFY_UPLOAD, R.drawable.ic_baseline_cloud_circle_24);
        notification.setContentTitle(context.getString(R.string.notification_title_uploading));
        notification.setContentText(context.getString(R.string.notification_content_default));
        notification.build();

        IUploadProgressListener uploadListener = new IUploadProgressListener() {
            @Override
            public void progressChanged(Upload uploader) {
                Notification notification;
                Upload.State state = uploader.getState();
                notification = mNotificationsByUploadListener.get(this);
                if (state == Upload.State.GET_REMOTE_QUOTA_STARTED) {
                    Log.d(TAG, "[Notification]:fetching remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_upload_start_get_quota));
                }
                else if(state == Upload.State.PREPARE_LOCAL_FILES_STARTED){
                    Log.d(TAG, "[Notification]:split file...");
                    notification.updateContentText(context.getString(R.string.notification_content_upload_start_prepare_data));
                }
                else if(state == Upload.State.MEDIA_IN_PROGRESS) {
                    int current = uploader.getProgressCurrent();
                    int max = uploader.getProgressMax();
                    Log.d(TAG, "[Notification]:update progress. Current " + current + " Max: " + max);
                    notification.updateContentText(context.getString(R.string.notification_content_upload_uploading_file));
                    notification.updateProgress(current, max);
                }
                else if(state == Upload.State.MAP_UPDATE_STARTED){
                    Log.d(TAG, "update remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_upload_start_update_maps));
                }
            }

        };

        mNotificationsByUploadListener.put(uploadListener, notification);
        return uploadListener;
    }


    IDownloadProgressListener createDownloadListener(){
        Notification notification;
        notification = new Notification(Notification.Category.NOTIFY_DOWNLOAD, R.drawable.ic_baseline_cloud_circle_24);
        notification.setContentTitle(context.getString(R.string.notification_title_downloading));
        notification.setContentText(context.getString(R.string.notification_content_default));
        notification.build();

        IDownloadProgressListener listener = new IDownloadProgressListener() {
            @Override
            public void progressChanged(Download downloader) {
                Log.d(TAG, "progressChanged!");
                Notification notification;
                Download.State state = downloader.getState();
                notification = mNotificationsByDownloadListener.get(this);
                if (state == Download.State.GET_REMOTE_MAP_STARTED) {
                    Log.d(TAG, "[Notification]:fetching remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_download_start_fetch_maps));
                }
                else if(state == Download.State.MEDIA_IN_PROGRESS){
                    int current = downloader.getProgressCurrent();
                    int max = downloader.getProgressMax();
                    Log.d(TAG, "[Notification]:download progress. Current " + current + " Max: " + max);
                    notification.updateContentText(context.getString(R.string.notification_content_download_uploading_file));
                    notification.updateProgress(current, max);
                }
            }
        };
        mNotificationsByDownloadListener.put(listener, notification);

        return listener;
    }

    IDeleteProgressListener createDeleteListener(){
        Notification notification;
        notification = new Notification(Notification.Category.NOTIFY_DOWNLOAD, R.drawable.ic_baseline_cloud_circle_24);

        IDeleteProgressListener lisener = new IDeleteProgressListener() {
            @Override
            public void progressChanged(Delete deleter) {

            }
        };
        return lisener;
    }
}
