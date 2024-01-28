package com.crossdrives.driveclient.upload;

import android.util.Log;

import com.crossdrives.driveclient.BaseRequest;
import com.crossdrives.driveclient.OneDriveClient;
import com.google.api.services.drive.model.File;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.models.Drive;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.DriveItemRequestBuilder;
import com.microsoft.graph.requests.DriveRequestBuilder;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OneDriveUploadRequest extends BaseRequest implements IUploadRequest {
    final String TAG = "CD.OneDriveUploadRequest";
    OneDriveClient mClient;
    java.io.File mPath;
    File mMetaData;

    public OneDriveUploadRequest(OneDriveClient client, File metadata, java.io.File path) {
        mClient = client;
        mPath = path;
        mMetaData = metadata;
    }

    @Override
    public void mediaType(String type) {

    }

    @Override
    public void uploadType(String type) {

    }

    @Override
    public void run (IUploadCallBack callback) {

        com.crossdrives.driveclient.model.File fileToClient = new com.crossdrives.driveclient.model.File();

        /*
            Use completable future to wrap the blocking call doUploadBlocked.
            We don't care the result of the completable future, The upload result is propagated
            to the initiator through callback.
        */
        CompletableFuture<File> workingFuture = CompletableFuture.supplyAsync(()->{
            File f = null;
            try {
                f = doSmallUploadBlocked();
                fileToClient.setFile(f);
                fileToClient.setOriginalLocalFile(mPath);
                callback.success(fileToClient);
            } catch (Exception e){
                Log.w(TAG, e.toString());
                callback.failure(e.getMessage(), mPath);
            }
            return f;
        });

        workingFuture.exceptionally(e->{
            Log.w(TAG, e.toString());
            callback.failure(e.getMessage(), mPath);
            return null;
        });

    }

    private File doSmallUploadBlocked() throws IOException, FileNotFoundException {
        List<String> parents;
        DriveRequestBuilder rb;
        DriveItemRequestBuilder irb;
        DriveItem uploadedItem;
        InputStream fileStream = null;
        File f = new File();

        //Not clear how to specify the conflict behavior (@microsoft.graph.conflictBehavior) so far
        // because no example is found in internet.
        // The default conflict behavior seems to be replacing existing according to what I test.
        //https://github.com/microsoftgraph/msgraph-sdk-java/blob/dev/src/test/java/com/microsoft/graph/functional/OneDriveTests.java#L92
        // https://github.com/microsoftgraph/msgraph-sdk-java/issues/393
        DriveItemUploadableProperties property = new DriveItemUploadableProperties();
        property.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));
        DriveItemCreateUploadSessionParameterSet uploadParams =
                DriveItemCreateUploadSessionParameterSet.newBuilder()
                        .withItem(property).build();

        // How do I read / convert an InputStream into a String in Java?
        // https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
        StringWriter writer = new StringWriter();
        IOUtils.copy(new FileInputStream(mPath), writer, StandardCharsets.US_ASCII);
        byte[] stream = writer.toString().getBytes();

        //build ItemRequestBuilder according to the given parent
        rb = mClient.getGraphServiceClient().me().drive();
        parents = mMetaData.getParents();

        irb = buildItemRequest(rb, parents);

        uploadedItem = irb
//            uploadSession = mClient.getGraphServiceClient()
//                    .me()
//                    .drive()
//                    .items("CD26537079F955DF!5758")
                //.root()
                // itemPath like "/Folder/file.txt"
                // does not need to be a path to an existing item
                .itemWithPath("/" + mMetaData.getName())
                //.createUploadSession(uploadParams)
                .content()
                //.createUploadSession(uploadParams)
                .buildRequest()
                .put(stream);
//        }catch (ClientException e){
//            Log.w(TAG, "create upload session: " + e.toString());
//        }
        Log.d(TAG, "uploaded item: " + uploadedItem.name + " ID: " + uploadedItem.id);
        f.setName(uploadedItem.name);
        f.setId(uploadedItem.id);
        return f;
    }

    private File doUploadBlocked() throws IOException, FileNotFoundException {
        // Get an input stream for the file
        //File file = new File(path);
        InputStream fileStream = null;
        UploadSession uploadSession = null;
        LargeFileUploadResult<DriveItem> result = null;
        File f = new File();
        DriveRequestBuilder rb;
        DriveItemRequestBuilder irb;
        List<String> parents;

        fileStream = new FileInputStream(mPath);

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
        //https://github.com/microsoftgraph/msgraph-sdk-java/blob/dev/src/test/java/com/microsoft/graph/functional/OneDriveTests.java#L92
        // https://github.com/microsoftgraph/msgraph-sdk-java/issues/393
        DriveItemUploadableProperties property = new DriveItemUploadableProperties();
        property.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));
        DriveItemCreateUploadSessionParameterSet uploadParams =
                DriveItemCreateUploadSessionParameterSet.newBuilder()
                        .withItem(property).build();

        //build ItemRequestBuilder according to the given parent
        rb = mClient.getGraphServiceClient().me().drive();
        parents = mMetaData.getParents();

        irb = buildItemRequest(rb, parents);

        // Create an upload session
        Log.d(TAG, "create upload session");
        uploadSession = irb
//            uploadSession = mClient.getGraphServiceClient()
//                    .me()
//                    .drive()
//                    .items("CD26537079F955DF!5758")
                    //.root()
                    // itemPath like "/Folder/file.txt"
                    // does not need to be a path to an existing item
                    .itemWithPath("/" + mMetaData.getName())
                    .createUploadSession(uploadParams)
                    .buildRequest()
                    .post();
//        }catch (ClientException e){
//            Log.w(TAG, "create upload session: " + e.toString());
//        }

        Log.d(TAG, "create upload task");
        LargeFileUploadTask<DriveItem> largeFileUploadTask =
                new LargeFileUploadTask<DriveItem>
                        (uploadSession, mClient.getGraphServiceClient(), fileStream, streamSize, DriveItem.class);

        // Do the upload
        Log.d(TAG, "do the upload");
        /*
            upload will get blocked util upload is completed.
        */
        result = largeFileUploadTask.upload(0, null, Progress_callback);
        //Log.d(TAG, "returned result: " + result.responseBody.name + " ID: " + result.responseBody.id);
        f.setName(result.responseBody.name);
        f.setId(result.responseBody.id);
        return f;
    }

    DriveItemRequestBuilder buildItemRequest(DriveRequestBuilder rb, List<String> parents){
        DriveItemRequestBuilder irb = null;

        irb = rb.root();
        if(parents == null){
            Log.d(TAG, "PID is null, upload file to root");
        }
        else if(parents.size() == 0) {
            Log.d(TAG, "No PID is given, upload file to root");
        }
        else{
            irb = rb.items(parents.get(0));
        }

        return irb;
    }

}
