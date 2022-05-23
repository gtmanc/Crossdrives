package com.crossdrives.driveclient.upload;

import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.GoogleDriveClient;
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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

public class GoogleDriveUploadRequest extends BaseRequest implements IUploadRequest {
    final String TAG = "CD.GoogleDriveUploadRequest";
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

    public void run_NonServiceSpecific(IUploadCallBack callback){
        HttpResponse response = null;
        File file = new File();
        com.crossdrives.driveclient.model.File fileToClient = new com.crossdrives.driveclient.model.File();


        try {
            response = submitRequestNotServiceSpecific();
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

        try {
            response.disconnect();
        } catch (IOException e) {
            Log.w(TAG, "Response disconnect failed: " + e.getMessage());
        }

        file.setName(mPath.getName());
        file.setId("");
        fileToClient.setFile(file);
        fileToClient.setOriginalLocalFile(mPath);
        callback.success(fileToClient);
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
    //https://stackoverflow.com/questions/39887303/resumable-upload-in-drive-rest-api-v3
    private HttpResponse submitRequestNotServiceSpecific() throws MalformedURLException, IOException {
        NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

        FileContent mediaContent = new FileContent(mMediaType, mPath);
        GenericUrl requestUrl = null;
        HttpResponse response = null;
        //String url = "https://www.googleapis.com/upload/drive/v3/files?name=" + mMetadata.getName();
        String url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable";
        Log.d(TAG, "Request URL: " + url);

//        requestUrl = new GenericUrl(
//                    new URL("https://www.googleapis.com/upload/drive/v3/files?name=" + mMetadata.getName()));
        requestUrl = new GenericUrl(
                new URL(url));

        //Log.w(TAG, "Access Token: " + mClient.getCredentail().getAccessToken());

        MediaHttpUploader uploader = new MediaHttpUploader(mediaContent, HTTP_TRANSPORT, mClient.getCredentail());
        uploader.setProgressListener(new CustomProgressListener());
        uploader.setDirectUploadEnabled(true);
        Log.d(TAG, "Upload chunk size: " + uploader.getChunkSize());
        Log.d(TAG, "Upload InitiationRequestMethod: " + uploader.getInitiationRequestMethod());
        Log.d(TAG, "Upload InitiationHeaders: " + uploader.getInitiationHeaders());
        //Log.d(TAG, "Upload MediaContent: " + uploader.getMediaContent().toString());
        Log.d(TAG, "Upload MediaMetaData: " + uploader.getMetadata());

        response = uploader.upload(requestUrl);

        return response;
    }

    @Override
    public void run(IUploadCallBack callback) {
        Task<File> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<File>() {

            @Override
            public File call() throws Exception {
                File file = null;
                FileContent mediaContent = new FileContent(mMediaType, mPath);
                Drive.Files.Create create;
                Log.d(TAG, "path: " + mPath);
                Log.d(TAG, "parents: " + mMetadata.getParents().get(0));
                Log.d(TAG, "name: " + mMetadata.getName());
                    //file = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent)
                    create = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent);
                    MediaHttpUploader uploader = create.getMediaHttpUploader();
                    uploader.setProgressListener(new CustomProgressListener());
                    create.setFields("id");
                    file = create.execute();

                //Log.d(TAG, "Upload chunk size: " + uploader.getChunkSize());
                return file;
            }
        });
        task.addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                com.crossdrives.driveclient.model.File fileToClient = new com.crossdrives.driveclient.model.File();
                //call back
                if(file != null){
                    Log.d(TAG, "Upload OK: " + file.getId());
                    fileToClient.setFile(file);
                    fileToClient.setOriginalLocalFile(mPath);
                    callback.success(fileToClient);
                }
                else{
                    Log.w(TAG, "Upload Failed!");
                    callback.failure("Unknown failure!");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //call back
                Log.w(TAG, "Upload Failed: " + e.getMessage());
                callback.failure(e.getMessage());
            }
        });
    }

    public void run_(IUploadCallBack callback){
        Task<File> task;

        task = Tasks.call(mClient.getExecutor(), new Callable<File>() {
            @Override
            public File call() throws Exception {
                File file = null;
                FileContent mediaContent = new FileContent(mMediaType, mPath);
                Drive.Files.Create create;
                //Log.d(TAG, "Path: " + mPath);


                //file = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent)
                create = mClient.getGoogleDriveService().files().create(mMetadata, mediaContent);
                MediaHttpUploader uploader = create.getMediaHttpUploader();
                uploader.setProgressListener(new CustomProgressListener());
                create.setFields("id");
                file = create.execute();
                //Log.d(TAG, "Upload chunk size: " + uploader.getChunkSize());
                return file;
            }
        });

        task.addOnSuccessListener(new OnSuccessListener<File>() {
            @Override
            public void onSuccess(File file) {
                com.crossdrives.driveclient.model.File fileToClient= new com.crossdrives.driveclient.model.File();
                //call back
                if(file != null){
                    Log.d(TAG, "Upload OK: " + file.getId());
                    fileToClient.setFile(file);
                    fileToClient.setOriginalLocalFile(mPath);
                    callback.success(fileToClient);
                }
                else{
                    Log.w(TAG, "Upload Failed!");
                    callback.failure("Unknown failure. Could be IO exception in drive client request");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //call back
                Log.w(TAG, "Upload Faile: " + e.getMessage());
                callback.failure(e.getMessage());
            }
        });

    }
}
