package com.crossdrives.driveclient;

import android.util.Log;

import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class OneDriveUploadRequest extends BaseRequest implements IUploadRequest{
    final String TAG = "GDC.OneDriveUploadRequest";
    OneDriveClient mClient;
    java.io.File mPath;

    public OneDriveUploadRequest(OneDriveClient client, File metadata, java.io.File path) {
        mClient = client;
        mPath = path;
    }

    @Override
    public void meidaType(String type) {

    }

    @Override
    public void uploadType(String type) {

    }

    @Override
    public void run(IUploadCallBack callback) {
        // Get an input stream for the file
        //File file = new File(path);
        InputStream fileStream = null;
        try {
            fileStream = new FileInputStream(mPath);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Open FileInputStream failed!" + e.toString());
        }
        long streamSize = mPath.length();

// Create a callback used by the upload provider
        IProgressCallback Progress_callback = new IProgressCallback() {
            @Override
            // Called after each slice of the file is uploaded
            public void progress(final long current, final long max) {
//                System.out.println(
//                        String.format("Uploaded %d bytes of %d total bytes", current, max)
//                );
                Log.d(TAG, "Uploaded %" + current + "bytes of % " + max + " total bytes");
            }
        };

        DriveItemCreateUploadSessionParameterSet uploadParams =
                DriveItemCreateUploadSessionParameterSet.newBuilder()
                        .withItem(new DriveItemUploadableProperties()).build();

        // Create an upload session
        Log.d(TAG, "create upload session");
        UploadSession uploadSession = mClient.getGraphServiceClient()
                .me()
                .drive()
                .root()
                // itemPath like "/Folder/file.txt"
                // does not need to be a path to an existing item
                .itemWithPath("/")
                .createUploadSession(uploadParams)
                .buildRequest()
                .post();

        LargeFileUploadTask<DriveItem> largeFileUploadTask =
                new LargeFileUploadTask<DriveItem>
                        (uploadSession, mClient.getGraphServiceClient(), fileStream, streamSize, DriveItem.class);

// Do the upload
        try {
            Log.d(TAG, "do the upload");
            largeFileUploadTask.upload(0, null, Progress_callback);
        } catch (IOException e) {
            Log.w(TAG, "Upload task doens't work! " + e.toString());
        }
    }
}
