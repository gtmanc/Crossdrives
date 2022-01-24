package com.crossdrives.driveclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

public class GoogleDriveUploadRequest extends BaseRequest implements IUploadRequest{
    final String TAG = "GDC.GoogleDriveUploadRequest";
    GoogleDriveClient mClient;
    File mMetadata;
    java.io.File mPath;
    String mMediaType = "text/plain";
    String mUploadType = "uploadType=resumable";

    public GoogleDriveUploadRequest(GoogleDriveClient client, File metadata, java.io.File path) {
        mClient = client;
        mMetadata = metadata;
        mPath = path;
    }

    @Override
    public void meidaType(String type) {
        //check valid? "image/jpeg"
        mMediaType = type;
    }

    @Override
    public void uploadType(String type) {

    }

    @Override
    public void run(IUploadCallBack callback) {
        HttpResponse response = null;
        File file = new File();


        try {
            response = submitResumableRequest();
        } catch (MalformedURLException e) {
            Log.w(TAG, "MalformedURLException: " + e.toString());
            callback.failure(e.toString());
        } catch (IOException e) {
            Log.w(TAG, "IO exception: " + e.toString());
            callback.failure(e.toString());
        }

        if(response != null) {
            if (!response.isSuccessStatusCode()) {
                //throw GoogleJsonResponseException(jsonFactory, response);
                Log.w(TAG, "Upload failed: " + response.getStatusMessage());
                callback.failure(response.getStatusMessage());
            }
        }else{ callback.failure("Unknown failure: http response is null");}

        Log.d(TAG, "Get Request: " + response.getRequest());
        InputStream is;
        try {
            is = response.getContent();
            Log.d(TAG, "Get Content: " + is.toString());
        } catch (IOException e) {
            Log.w(TAG, "Upload failed: " + e.getMessage());
        }


        file.setName(mPath.getName());
        file.setId("");
        callback.success(file);
    }

    class CustomProgressListener implements MediaHttpUploaderProgressListener {
        @Override
        public void progressChanged(MediaHttpUploader uploader) throws IOException {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    //System.out.println("Initiation has started!");
                    Log.d(TAG, "Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    //System.out.println("Initiation is complete!");
                    Log.d(TAG, "Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    //System.out.println(uploader.getProgress());
                    Log.d(TAG, "Upload progress: " +uploader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    //System.out.println("Upload is complete!");
                    Log.d(TAG, "Upload is complete!");
            }
        }
    }

    //a reference how to use MediaHttpUploader:
    // https://stackoverflow.com/questions/39887303/resumable-upload-in-drive-rest-api-v3
    private HttpResponse submitResumableRequest() throws MalformedURLException, IOException {
        NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

        FileContent mediaContent = new FileContent(mMediaType, mPath);
        GenericUrl requestUrl = null;
        HttpResponse response = null;

        requestUrl = new GenericUrl(
                    new URL("https://www.googleapis.com/upload/drive/v3/files?name=" + mMetadata.getName()));

        //Log.w(TAG, "Access Token: " + mClient.getCredentail().getAccessToken());

        MediaHttpUploader uploader = new MediaHttpUploader(mediaContent, HTTP_TRANSPORT, mClient.getCredentail());
        uploader.setProgressListener(new CustomProgressListener());

        response = uploader.upload(requestUrl);

        return response;
    }

    public void submitDirectRequest(IUploadCallBack callback){
        Task<File> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<File>() {
            @Override
            public File call() throws Exception {
                File file;
                FileContent mediaContent = new FileContent(mMediaType, mPath);
                //Log.d(TAG, "Path: " + mPath);
                try {
                    file = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent)
                            .setFields("id")
                            .execute();
                } catch (IOException e) {
                    Log.w(TAG, "IO exception: " + e.toString());
                    file = null;
                }

                //System.out.println("File ID: " + file.getId());
                return file;
            }
        });

        task.addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                //call back
                if(file != null){
                    callback.success(file);
                }
                else{
                    callback.failure("Unknown failure. Could be IO exception in drive client request");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //call back
                callback.failure(e.toString());
            }
        });

    }
}
