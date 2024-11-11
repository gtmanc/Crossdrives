package com.crossdrives.ui.listener;

import android.content.Context;
import android.util.Log;

import com.crossdrives.cdfs.delete.Delete;
import com.crossdrives.cdfs.delete.IDeleteProgressListener;
import com.crossdrives.cdfs.download.Download;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.move.IMoveItemProgressListener;
import com.crossdrives.cdfs.move.Move;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.cdfs.upload.Upload;
import com.crossdrives.msgraph.SnippetApp;
import com.crossdrives.ui.notification.Notification;
import com.example.crossdrives.R;

import java.util.HashMap;

public class ProgressUpdater {
    final String TAG = "CD.ProgressUpdater";

    HashMap<IUploadProgressListener, Notification> mNotificationsByUploadListener = new HashMap<>();
    HashMap<IDownloadProgressListener, Notification> mNotificationsByDownloadListener = new HashMap<>();
    Context context = SnippetApp.getAppContext();

    public IUploadProgressListener createUploadListener(Notification notification){

        IUploadProgressListener uploadListener = new IUploadProgressListener() {
            @Override
            public void progressChanged(Upload uploader) {
                //Notification notification;
                Upload.State state = uploader.getState();
                //notification = mNotificationsByUploadListener.get(this);
                //Log.d(TAG, "[Upload Notification]: called");
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

        //mNotificationsByUploadListener.put(uploadListener, notification);
        return uploadListener;
    }


    public IDownloadProgressListener createDownloadListener(Notification notification){

        IDownloadProgressListener listener = new IDownloadProgressListener() {
            @Override
            public void progressChanged(Download downloader) {
                Log.d(TAG, "Download progressChanged!");
                //Notification notification;
                Download.State state = downloader.getState();
                //notification = mNotificationsByDownloadListener.get(this);
                if (state == Download.State.GET_REMOTE_MAP_STARTED) {
                    Log.d(TAG, "[Notification]:fetching remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_download_start_fetch_maps));
                }
                else if(state == Download.State.MEDIA_IN_PROGRESS){
                    int current = downloader.getProgressCurrent();
                    int max = downloader.getProgressMax();
                    Log.d(TAG, "[Notification]:download progress. Current " + current + " Max: " + max);
                    notification.updateContentText(context.getString(R.string.notification_content_downloading_file));
                    notification.updateProgress(current, max);
                }
            }
        };
        //mNotificationsByDownloadListener.put(listener, notification);

        return listener;
    }

    public IDeleteProgressListener createDeleteListener(Notification notification){

        IDeleteProgressListener lisener = new IDeleteProgressListener() {
            @Override
            public void progressChanged(Delete deleter) {
                //Log.d(TAG, "delete progressChanged!");
                //Notification notification;
                Delete.State state = deleter.getState();
                //notification = mNotificationsByDownloadListener.get(this);
                if (state == Delete.State.GET_MAP_STARTED) {
                    Log.d(TAG, "[Notification]:fetching remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_delete_start_fetch_maps));
                }
                else if(state == Delete.State.DELETION_IN_PROGRESS){
                    int current = deleter.getProgressCurrent();
                    int max = deleter.getProgressMax();
                    Log.d(TAG, "[Notification]:delete in progress. Current " + current + " Max: " + max);
                    notification.updateContentText(context.getString(R.string.notification_content_deleting_file));
                    notification.updateProgress(current, max);
                }
            }
        };
        return lisener;
    }

    public IMoveItemProgressListener createMoveItemListener(Notification notification){

        IMoveItemProgressListener lisener = new IMoveItemProgressListener() {
            @Override
            public void progressChanged(Move mover) {
                //Log.d(TAG, "move progressChanged!");
                //Notification notification;
                Move.State state = mover.getState();
                //notification = mNotificationsByDownloadListener.get(this);
                if (state == Move.State.GET_MAP_STARTED) {
                    Log.d(TAG, "[Notification]:fetching remote maps...");
                    notification.updateContentText(context.getString(R.string.notification_content_move_item_start_fetch_maps));
                }
                else if(state == Move.State.SRC_MAP_UPDATE_STARTED){
                    Log.d(TAG, "Move: update source map");
                    notification.updateContentText(context.getString(R.string.notification_content_move_item_start_update_src_map));
                }
                else if(state == Move.State.MOVE_IN_PROGRESS){
                    int current = mover.getProgressCurrent();
                    int max = mover.getProgressMax();
                    Log.d(TAG, "[Notification]:move in progress. Current " + current + " Max: " + max);
                    notification.updateContentText(context.getString(R.string.notification_content_moving_file));
                    notification.updateProgress(current, max);
                }else if(state == Move.State.DEST_MAP_UPDATE_STARTED){
                    Log.d(TAG, "Move: update dest map");
                    notification.updateContentText(context.getString(R.string.notification_content_move_item_start_update_dest_map));
                }
            }
        };
        return lisener;
    }
}
