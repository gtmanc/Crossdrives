package com.crossdrives.cdfs;

import android.app.Activity;
import android.util.Log;

import com.crossdrives.driveclient.IDownloadCallBack;
import com.crossdrives.driveclient.IFileListCallBack;
import com.crossdrives.driveclient.IDriveClient;
import com.crossdrives.driveclient.IUploadCallBack;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CDFS {
    private static String TAG = "CDFS.CDFS";
    //List<IDriveClient> sClient = new ArrayList<>();
    HashMap<String, IDriveClient> mDrives = new HashMap<>();
    private final Executor sExecutor = Executors.newSingleThreadExecutor();
    private FileList mFileList;
    private OutputStream mStream;
    private Activity mActivity;
    static CDFS mCDFS = null;
    private final String NAME_ALLOCATION_FILE = "allocation.cdfs";

    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;

    CDFS(Activity activity) {
        mActivity = activity;
        String content;

        Log.d(TAG, "create allocation file");
        createTextFile(NAME_ALLOCATION_FILE, "Header of CDFS allocation");
        content = readFile(NAME_ALLOCATION_FILE);
        Log.d(TAG, content);
    }

    static public CDFS getCDFSService(Activity activity){
        if(mCDFS == null){
            Log.d(TAG, "Create instance CDFS");
            mCDFS = new CDFS(activity);
        }
        return mCDFS;
    }

    public void addClient(String brand, IDriveClient client){
        Log.d(TAG, "Add client!");
        //sClient.add(client);
        mDrives.put(brand, client);
        //return getClient(client);

        File metadata = new File();
        java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" +NAME_ALLOCATION_FILE);
        metadata.setName(NAME_ALLOCATION_FILE);
        client.upload().buildRequest(metadata, filePath).run(new IUploadCallBack() {
            @Override
            public void success(File file) {
                Log.d(TAG, "Upload file OK. ID: " + file.getId());
            }

            @Override
            public void failure(String ex) {
                Log.w(TAG, "Failed to upload file: " + ex.toString());
            }
        });
    }

    //public void removeClient(int i){
    public boolean removeClient(String brand, IDriveClient client){
        //sClient.remove(i);
        return mDrives.remove(brand, client);
    }
//    public IDriveClient getClient(int i){
//        return sClient.get(i);
//    }
    public IDriveClient getClient(String brand){
        //return sClient.indexOf(client);
        return mDrives.get(brand);
    }

    /*
    Operation: get file list
     */
    public Task<FileList> list(Object nextPage){
        Task task;
        Log.d(TAG, "Operation: list files. nextPage: " + nextPage);
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public FileList call() throws Exception {
                msTaskfinished = false;
                /*
                    Drive client test only. Always use index 0 (i.e first one added)
                 */
                mDrives.values().iterator().next().list().buildRequest()
                //sClient.get(0).list().buildRequest()
                        .setNextPage(nextPage)
                        .setPageSize(0) //0 means no page size is applied
                        //.filter("mimeType = application/vnd.google-apps.folder and name contains 'cdfs'")
                        //.filter("mimeType = application/vnd.google-apps.folder")
                        .filter(null)   //null means no filter will be applied
                        .run(new IFileListCallBack<FileList, Object>() {
                    @Override
                    public void success(FileList fileList, Object o) {
                        exitWait();
                        mFileList = fileList;
                        Log.d(TAG, "list finished");
                    }

                    @Override
                    public void failure(String ex) {
                        exitWait();
                        Log.w(TAG, "list finished with failure!");
                    }
                });
                waitUntilFinished();
                return mFileList;
            }
        });

        return task;
    }

    /*
        Operation download content of a file
     */
    public Task<OutputStream> download(String id) {
        Task task;
        Log.d(TAG, "Operation: download " + id);
        task = Tasks.call(sExecutor, new Callable<Object>() {
            @Override
            public OutputStream call() throws Exception {
                msTaskfinished = false;
                mDrives.values().iterator().next().download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                //sClient.get(0).download().buildRequest(id).run(new IDownloadCallBack<OutputStream>() {
                    @Override
                    //public void success(InputStream inputStream){
                    public void success(OutputStream os){
                        exitWait();
                        mStream = os;

                        Log.d(TAG, "download finished.");
//                        try {
//                            os.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }

                    @Override
                    public void failure(String ex) {
                        exitWait();
                        Log.w(TAG, "download failed" + ex.toString());
                    }
                });
                waitUntilFinished();
                if(mStream == null){ Log.w(TAG, "stream is null" );}
                return mStream;
            }
        });

        return task;
    }

    private void exitWait(){
        msTaskfinished = true;
    }

    private void waitUntilFinished() throws Exception{
        while(msTaskfinished == false) {
            Thread.sleep(100);
        }
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
    private String readFile(String path) {
        /* We have to use the openFileInput()-method
         * the ActivityContext provides.
         * Again for security reasons with
         * openFileInput(...) */

        FileInputStream fIn = null;
        String readString="";
        String s;

        try {
            fIn = mActivity.openFileInput(path);
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