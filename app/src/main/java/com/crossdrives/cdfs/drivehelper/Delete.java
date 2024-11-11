package com.crossdrives.cdfs.drivehelper;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.delete.IDeleteCallBack;
import com.crossdrives.driveclient.model.File;

import java.util.concurrent.CompletableFuture;

public class Delete {
    private IDriveClient mClient;
    public Delete(IDriveClient client) {
        mClient = client;
    }

    public class Deleted{
        public File file;
    }

    public CompletableFuture<Deleted> runSync(File file){
        //Log.d(TAG, "Submit request to drive client. drive:" + driveName + ". Seq:" + item.getSequence() + ".");
        CompletableFuture<Deleted> future = new CompletableFuture<>();
        //com.google.api.services.drive.model.File file = new File();
        //com.crossdrives.driveclient.model.File file;

        //file = setAiToFile(item);
        mClient.delete().buildRequest(file).run(new IDeleteCallBack<File>() {
            @Override
            public void success(com.crossdrives.driveclient.model.File file) {
                Deleted deleted = new Deleted();
                deleted.file = file;
                future.complete(deleted);
            }

            @Override
            public void failure(String ex) {
                future.completeExceptionally(new Throwable(ex));
            }
        });
        return future;
    }

    private com.crossdrives.driveclient.model.File setAiToFile(AllocationItem ai){
        com.crossdrives.driveclient.model.File file = new com.crossdrives.driveclient.model.File();
        com.google.api.services.drive.model.File gApiFile = new com.google.api.services.drive.model.File();

        gApiFile.setId(ai.getItemId());

        //file.setDriveName(ai.getDrive());
        file.setFile(gApiFile);
        //file.setInteger(ai.getSequence());
        //file.setString(ai.getCdfsId());

        return file;
    }
}
