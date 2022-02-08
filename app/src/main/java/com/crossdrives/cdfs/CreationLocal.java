package com.crossdrives.cdfs;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.create.ICreateCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreationLocal extends BaseCDFS implements IFileCreation{
    private String TAG = "CD.CreationLocal";
    IDriveClient mClient;
    String mFileId;
    Activity mActivity;

    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;


    public CreationLocal(Activity activity) {
        mActivity = activity;
    }


    public java.io.File create(String name){
        java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" + name);
        createTextFile(name, "Json strings");
        return filePath;
    }

    /*
     * Files with Activity Output/input: https://stackoverflow.com/questions/1239026/how-to-create-a-file-in-android
     * */
    private void createTextFile(String path, String content){
        // catches IOException below
        //final String TESTSTRING = new String("Hello Android");

        /* We have to use the openFileOutput()-method
         * the ActivityContext provides, to
         * protect your file from others and
         * This is done for security-reasons.
         * We chose MODE_WORLD_READABLE, because
         *  we have nothing to hide in our file */
        FileOutputStream fOut = null;
        try {
            fOut = mActivity.openFileOutput(path, Activity.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            // Write the string to the file
            osw.write(content);
            /* ensure that everything is
             * really written out and close */
            osw.flush();
            osw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){

        }
    }

    public void folder(){
        File fileMetadata = new File();
        fileMetadata.setName("cdfs");
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        create(fileMetadata).addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.d(TAG, "Folder created. ID: " + s);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to create folder : " + e.getMessage());
            }
        });
    }

    public Task<String> create(File metadata) {

        Task task;
        Log.d(TAG, "CDFS create file: " + metadata.getName());
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public String call() throws Exception {

                msTaskfinished = false;
                /*
                    Drive client test only. Always use index 0 (i.e first one added)
                 */
        mClient.create().buildRequest(metadata).run(new ICreateCallBack<File>() {
                    @Override
                    public void success(File file) {
                        //exitWait();
                        mFileId = file.getId();
                        Log.d(TAG, "Create file OK. ID: " + file.getId());
                    }

                    @Override
                    public void failure(String ex) {
                        //exitWait();
                        Log.w(TAG, "Failed to create file: " + ex.toString());
                    }
                });
                //waitUntilFinished();
                return mFileId;
            }
        });
        return task;
    }
}
