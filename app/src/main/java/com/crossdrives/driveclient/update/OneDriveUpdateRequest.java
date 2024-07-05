package com.crossdrives.driveclient.update;

import android.util.Log;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.model.File;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
import com.microsoft.graph.requests.DriveItemContentStreamRequestBuilder;
import com.microsoft.graph.requests.DriveItemRequest;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
                    file = doSmallUploadBlocked(mClient.getGraphServiceClient(), mfileID, mMediaContent);
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
                item.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));

                request = mClient.getGraphServiceClient().me().drive().items(mfileID).buildRequest();
                DriveItem updatedItem = new DriveItem();
                updatedItem = request.patch(item);
                if(updatedItem == null){
                    callback.failure("Patch request returned a NULL item!");
                }else{
                    Log.d(TAG, "PATCH completed. Item Name: " + updatedItem.name);
                    file = new File();
                    file.setName(updatedItem.name);
                    file.setId(updatedItem.id);
                    callback.success(file);
                }
            }
            return file;
        });

        workingFuture.exceptionally(e->{
            Log.w(TAG, e.toString());
            callback.failure(e.getMessage());
            return null;
        });

    }
    File doSmallUploadBlocked(GraphServiceClient client, String fileID, AbstractInputStreamContent mediaContent) throws IOException {
        DriveItemContentStreamRequestBuilder contentRequestBuilder;
        DriveItem item;
        File f = new File();

        //Not clear how to specify the conflict behavior (@microsoft.graph.conflictBehavior) so far
        // because no example is found in internet.
        // The default conflict behavior seems to be replacing existing according to what I test.

        //Java InputStream to Byte Array and ByteBuffer: https://www.baeldung.com/convert-input-stream-to-array-of-bytes
        InputStream is = mediaContent.getInputStream();
        byte[] stream = new byte[is.available()];
        is.read(stream);

        // How do I read / convert an InputStream into a String in Java?
        // https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
//        StringWriter writer = new StringWriter();
//        IOUtils.copy(mediaContent.getInputStream(), writer, StandardCharsets.US_ASCII);
//        byte[] stream = writer.toString().getBytes();

//        = Base64.getDecoder().decode("aaabbbccc");
//        Log.d(TAG, "decode done");
//        Log.d(TAG, "decoded text: " + stream.toString());
        //byte[] stream = Base64.getDecoder().decode(theString.toString());
        //stream = new String("hello").getBytes();
        item = client.me().drive().items(fileID).content().buildRequest().put(stream);

        Log.d(TAG, "uploaded item: " + item.name + " ID: " + item.id);
        f.setName(item.name);
        f.setId(item.id);
        return f;
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
        if (null == uploadSession) {
            fileStream.close();
            Log.d(TAG, "Could not create upload session");
            //throw new Exception("Could not create upload session");
        }
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
