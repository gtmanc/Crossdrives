package com.crossdrives.cdfs.data;

import android.app.Activity;
import android.util.Log;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.driveclient.IDriveClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileLocal implements IFileCreation {
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
        name: the name of file to create. exclude separator "\".
        content: String to write

        return: absolute path of the created file. Could be null.
     */
    public java.io.File create(String name, String content){
        //String s = path + "/" + name;
        //Log.d(TAG, "Create local file. Path: " + s);
        java.io.File filePath = null;
        try {
            createTextFile(name, content);
            filePath = new java.io.File(mCDFS.getContext().getFilesDir() + name);
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }

        return filePath;
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
    private void createTextFile(String path, String content) throws IOException {
        // catches IOException below
        //final String TESTSTRING = new String("Hello Android");

        /* We have to use the openFileOutput()-method
         * the ActivityContext provides, to
         * protect your file from others and
         * This is done for security-reasons.
         * We chose MODE_WORLD_READABLE, because
         *  we have nothing to hide in our file */
        FileOutputStream fOut = null;

            fOut = mCDFS.getContext().openFileOutput(path, Activity.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            // Write the string to the file
            osw.write(content);
            /* ensure that everything is
             * really written out and close */
            osw.flush();
            osw.close();


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
}
