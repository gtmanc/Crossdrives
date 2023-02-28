package com.crossdrives.ui.listener;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.model.File;
import com.crossdrives.msgraph.SnippetApp;
import com.crossdrives.test.TestFileIntegrityChecker;
import com.crossdrives.ui.notification.Notification;
import com.example.crossdrives.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResultUpdater {
    final String TAG = "CD.ResultUpdater";

    Context context = SnippetApp.getAppContext();

    interface IResultListener<T>{
        void onSuccess(T t);
    }

    public OnSuccessListener<File> createUploadSuccessListener(Notification notification){
        OnSuccessListener<File> listener = new OnSuccessListener<File>() {
                @Override
                public void onSuccess(File file) {
                    //Notification notification = mNotificationsByUploadSuccessListener.get(this);
                    notification.removeProgressBar();
                    notification.updateContentTitle(context.getString(R.string.notification_title_upload_completed));
                    notification.updateContentText(context.getString(R.string.notification_content_upload_complete));
                    //Toast.makeText(context, "file uploaded: " + file.getOriginalLocalFile().getName(), Toast.LENGTH_LONG).show();
                }
            };
        return listener;
    }

    public OnSuccessListener<String> createDownloadSuccessListener(Notification notification){
        OnSuccessListener<String> listener = new OnSuccessListener<String>() {

            @Override
            public void onSuccess(String file) {
                //Notification notification = mDownloadSuccessListener.get(this);
                notification.removeProgressBar();
                notification.updateContentTitle(context.getString(R.string.notification_title_download_completed));
                notification.updateContentText(context.getString(R.string.notification_content_download_complete));
                Log.d(TAG, "file downloaded: " + file);
                //Toast.makeText(context, "file downloaded: " + file, Toast.LENGTH_LONG).show();
                //downloadIntegrityCheck();
            }
        };
        return listener;
    }

    public OnSuccessListener<File> createDeleteSuccessListener(Notification notification){
        OnSuccessListener<File> listener = new OnSuccessListener<File>() {

            @Override
            public void onSuccess(File file) {
                //Notification notification = mDownloadSuccessListener.get(this);
                notification.removeProgressBar();
                notification.updateContentTitle(context.getString(R.string.notification_title_delete_completed));
                notification.updateContentText(context.getString(R.string.notification_content_delete_complete));
                Log.d(TAG, "file deleted: " + file);
            }
        };
        return listener;
    }

    public OnCompleteListener<Task> createUploadCompleteListener(InputStream finalIn){
        OnCompleteListener<Task> listener = new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                Log.d(TAG, "upload completed. Close stream");
                //https://stackoverflow.com/questions/16369462/why-is-inputstream-close-declared-to-throw-ioexception
                try {
                    finalIn.close();
                } catch (IOException e) {
                    Toast.makeText(SnippetApp.getAppContext(), "Upload Completed with error: "
                            + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        return listener;
    }

    public OnFailureListener createUploadFailureListener(Notification notification) {
        OnFailureListener listener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Notification notification = mNotificationsByUpFailedListener.get(this);
                Log.w(TAG, "upload failed: " + e.getMessage() + e.getCause());
                Toast.makeText(SnippetApp.getAppContext(), "Upload Failed: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show();
                notification.removeProgressBar();
                notification.updateContentTitle(context.getString(R.string.notification_title_upload_completed));
                notification.updateContentText(context.getString(R.string.notification_content_upload_complete_exceptionally));
                //Toast.makeText(context, "Uploaded failed! " + Toast.LENGTH_LONG).show();
            }
        };
        return listener;
    }

    public OnFailureListener createDownloadFailureListener(Notification notification) {
        OnFailureListener listener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Notification notification = mDownloadFailedListener.get(this);
                Log.w(TAG, "download failed: " + e.getMessage() + e.getCause());
                Toast.makeText(SnippetApp.getAppContext(), "download Failed: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show();
                notification.removeProgressBar();
                notification.updateContentTitle(context.getString(R.string.notification_title_download_completed));
                notification.updateContentText(context.getString(R.string.notification_content_download_complete_exceptionally));
            }
        };
        return listener;
    }

    public OnFailureListener createDeleteFailureListener(Notification notification) {
        OnFailureListener listener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Notification notification = mDownloadFailedListener.get(this);
                Log.w(TAG, "delete failed: " + e.getMessage() + e.getCause());
                Toast.makeText(SnippetApp.getAppContext(), "download Failed: "
                        + e.getMessage(), Toast.LENGTH_SHORT).show();
                notification.removeProgressBar();
                notification.updateContentTitle(context.getString(R.string.notification_title_delete_completed));
                notification.updateContentText(context.getString(R.string.notification_content_delete_complete_exceptionally));
            }
        };
        return listener;
    }

    public void downloadIntegrityCheck(String name){
        TestFileIntegrityChecker checker;
        FileInputStream fis = null;
        int result = 0;
        try {
            fis = SnippetApp.getAppContext().openFileInput(name);
            checker = new TestFileIntegrityChecker(fis);
            result = checker.execute(TestFileIntegrityChecker.Pattern.PATTERN_SERIAL_NUM);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error occurred in integrity check: "
                    + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if(fis != null){
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(result >= 0){
            Toast.makeText(context, "Integrity check failed. Position: "
                    + result, Toast.LENGTH_LONG).show();
        }
    }

    public OnSuccessListener<File> createCreateSuccessListener(Notification notification){
        OnSuccessListener<File> listener = new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {

                //Notification notification = mNotificationsByUploadSuccessListener.get(this);
                if(notification != null){
                    notification.removeProgressBar();
                    notification.updateContentTitle(context.getString(R.string.notification_title_upload_completed));
                    notification.updateContentText(context.getString(R.string.notification_content_upload_complete));
                }
                Toast.makeText(context, context.getString(R.string.toast_create_file_success), Toast.LENGTH_LONG).show();
            }
        };
        return listener;
    }

    public OnFailureListener createCreateFailureListener(Notification notification) {
        OnFailureListener listener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Notification notification = mDownloadFailedListener.get(this);
                //Notification notification = mNotificationsByUploadSuccessListener.get(this);
                if(notification != null){
                    notification.removeProgressBar();
                    notification.updateContentTitle(context.getString(R.string.notification_title_upload_completed));
                    notification.updateContentText(context.getString(R.string.notification_content_upload_complete));
                }
                Toast.makeText(context, context.getString(R.string.toast_create_file_failure), Toast.LENGTH_LONG).show();
            }
        };
        return listener;
    }
}
