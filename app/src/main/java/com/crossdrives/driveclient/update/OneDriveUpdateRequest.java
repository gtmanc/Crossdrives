package com.crossdrives.driveclient.update;

import android.util.Log;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.model.File;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.DriveItemRequest;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OneDriveUpdateRequest extends BaseRequest implements IUpdateRequest {
    final String TAG = "CD.OneDriveUpdateRequest";
    OneDriveClient mClient;
    String mfileID;
    MetaData mMetaData;
    AbstractInputStreamContent mMediaContent;

    public OneDriveUpdateRequest(OneDriveClient client, String id, MetaData metaData) {
        this.mClient = client;
        this.mfileID = id;
        this.mMetaData = metaData;
    }

    public OneDriveUpdateRequest(OneDriveClient client, String id, MetaData metaData, AbstractInputStreamContent mediaContent) {
        this.mClient = client;
        this.mfileID = id;
        this.mMetaData = metaData;
        this.mMediaContent = mediaContent;
    }

    @Override
    public void run(IUpdateCallBack callback){

        /*
            Use completable future to wrap the blocking call doUploadBlocked.
            We don't care the result of the completable future, The upload result is propagated
            to the initiator through callback.
        */
        CompletableFuture<File> workingFuture = CompletableFuture.supplyAsync(()-> {
            File file = null;

            if(mMediaContent == null){
                Log.d(TAG, "Update meta data only.");
            }
            if(mMetaData == null){
                Log.d(TAG, "Update content only.");
            }

            if (mMediaContent != null) {    //update content of an existing file with known ID
                try {
                    file = doUploadBlocked(mClient.getGraphServiceClient(), mfileID, mMediaContent);
                    callback.success(file);
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                    callback.failure(e.getMessage());
                }
            } else {
                //update meta data (properties of item)
                //Microsoft sdk reference: https://learn.microsoft.com/en-us/graph/api/driveitem-move?view=graph-rest-1.0&tabs=java
                DriveItemRequest request;
                DriveItem item = new DriveItem();
                ItemReference parentReference = new ItemReference();
                parentReference.id = mMetaData.getParents().toAdd.get(0);
                Log.d(TAG, "new parent id: " + parentReference.id);
                item.parentReference = parentReference;
                //driveItem.name = "new-item-name.txt";

                request = mClient.getGraphServiceClient().me().drive().items(mfileID).buildRequest();
                request.patch(item);
            }
            return file;
        });

        workingFuture.exceptionally(e->{
            Log.w(TAG, e.toString());
            callback.failure(e.getMessage());
            return null;
        });

    }

    File doUploadBlocked(GraphServiceClient client, String fileID, AbstractInputStreamContent mediaContent) throws IOException {
        InputStream fileStream = mediaContent.getInputStream();
        UploadSession uploadSession = null;
        DriveItemRequestBuilder requestBuilder;
        LargeFileUploadResult<DriveItem> result = null;
        File f = new File();

        long streamSize = fileStream.available();

        requestBuilder = client.me().drive().items(fileID);

        DriveItemCreateUploadSessionParameterSet uploadParams =
                DriveItemCreateUploadSessionParameterSet.newBuilder()
                        .withItem(new DriveItemUploadableProperties()).build();

        // Create an upload session
        Log.d(TAG, "create upload session");
        uploadSession = requestBuilder
//            uploadSession = mClient.getGraphServiceClient()
//                    .me()
//                    .drive()
//                    .items("CD26537079F955DF!5758")
                //.root()
                // itemPath like "/Folder/file.txt"
                // does not need to be a path to an existing item
                //.itemWithPath(mfileID)
                .createUploadSession(uploadParams)
                .buildRequest()
                .post();

        Log.d(TAG, "create upload task");
        /*
            upload will get blocked util upload is completed.
        */
        LargeFileUploadTask<DriveItem> largeFileUploadTask =
                new LargeFileUploadTask<DriveItem>
                        (uploadSession, client, fileStream, streamSize, DriveItem.class);

        // Do the upload
        Log.d(TAG, "do the upload");
        result = largeFileUploadTask.upload(0, null, Progress_callback);

        //Log.d(TAG, "returned result: " + result.responseBody.name + " ID: " + result.responseBody.id);
        f.setName(result.responseBody.name);
        f.setId(result.responseBody.id);
        return f;
    }

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
}
