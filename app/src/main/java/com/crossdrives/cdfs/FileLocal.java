package com.crossdrives.cdfs;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.create.ICreateCallBack;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileLocal implements IFileCreation{
    private String TAG = "CD.CreationLocal";
    IDriveClient mClient;
    String mFileId;
    CDFS mCDFS;

    private final ExecutorService sExecutor = Executors.newCachedThreadPool();
    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;


    public FileLocal(CDFS cdfs) { mCDFS = cdfs;    }

    /*
        Create file in app's directory "/"
        name: the file to create
        content: String to write
     */
    public void create(String name, String content){
        //String s = path + "/" + name;
        //Log.d(TAG, "Create local file. Path: " + s);
        //java.io.File filePath = new java.io.File(s);
        createTextFile(name, content);
        //return filePath;
    }

    /*
        Read file in the app's directory "/"
        name: the file to read
     */
    public String read(String name){
        String s;
        //Log.d(TAG, "Read local file. Path: " + s);
        s = readFile(name);
        return s;
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
            fOut = mCDFS.getContext().openFileOutput(path, Activity.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            // Write the string to the file
            osw.write(content);
            /* ensure that everything is
             * really written out and close */
            osw.flush();
            osw.close();

        } catch (FileNotFoundException e) {
            Log.w(TAG, "File not found! " + e.getMessage());
        }catch (IOException e){
            Log.w(TAG, "IOException! " + e.getMessage());
        }
    }

    private String readFile(String path) {
        /* We have to use the openFileInput()-method
         * the ActivityContext provides.
         * Again for security reasons with
         * openFileInput(...) */

        FileInputStream fIn = null;
        String readString="";
        String s;

        try {
            fIn = mCDFS.getContext().openFileInput(path);
            InputStreamReader isr = new InputStreamReader(fIn);
            BufferedReader inputReader = new BufferedReader(isr);
            /* Prepare a char-Array that will
             * hold the chars we read back in. */
            //char[] inputBuffer = new char[TESTSTRING.length()];

            // Fill the Buffer with data from the file
            //isr.read(inputBuffer);
            while((s = inputReader.readLine()) != null){
                readString = readString.concat(s);
            }

            // Transform the chars to a String
            //String readString = new String(inputBuffer);

            // Check if we read back the same chars that we had written out
            //boolean isTheSame = TESTSTRING.equals(readString);

            //Log.i("File Reading stuff", "success = " + isTheSame);

        } catch (FileNotFoundException e) {
            readString = null;
        } catch (IOException ioe){
            readString = null;
        }
        return readString;
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
