package com.crossdrives.cdfs;

import android.app.Activity;
import android.util.Log;

import com.crossdrives.driveclient.IDriveClient;
import com.google.api.services.drive.model.File;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

public class CDFS extends BaseCDFS{
    private static String TAG = "CDFS.CDFS";
    //List<IDriveClient> sClient = new ArrayList<>();
    //HashMap<String, IDriveClient> mDrives = new HashMap<>();
    ConcurrentHashMap<String, Drive> mDrives = new ConcurrentHashMap<>();
    private Activity mActivity;
    static CDFS mCDFS = null;
    static Service mService;


    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;

    CDFS(Activity activity) {
        mActivity = activity;
        String content;
    }

    static public CDFS getCDFSService(Activity activity){
        if(mCDFS == null){
            Log.d(TAG, "Create instance CDFS");
            mCDFS = new CDFS(activity);
            mService = new Service(mCDFS);
        }
        return mCDFS;
    }


    public Service getService(){return mService;};

    public void addClient(String brand, IDriveClient client){
        Log.d(TAG, "Add client. Client: " + client.toString());
        //sClient.add(client);
        Drive drive = new Drive(client);
        mDrives.put(brand, drive);
        //return getClient(client);
        createBaseFolder(client);
        //createAllocationFile();
        //deleteObseleteFile();
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
        return mDrives.get(brand).getClient();
    }

    public ConcurrentHashMap<String, Drive> getDrives(){
        return mDrives;
    }

    private void createBaseFolder(IDriveClient client){
        File fileMetadata = new File();
//        fileMetadata.setName("CDFS2");
//        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        Infrastructure verify = new Infrastructure(client);
        FileLocal fc = new FileLocal(mActivity);

        //Check CDFS existing folder. We will do the creation in the callback
        verify.checkAndBuild();

        //Upload test only. For Google, folder cdfs is used. For MS, AAA is used.
        //fileMetadata.setParents(Collections.singletonList("16IhpPc0_nrrDplc73YIevRI8C27ir1JG")); //cdfs
        //fileMetadata.setParents(Collections.singletonList("CD26537079F955DF!5758"));  //AAA

//        access.create(fileMetadata).addOnSuccessListener(new OnSuccessListener<String>() {
//            @Override
//            public void onSuccess(String s) {
//                Log.d(TAG, "create OK. ID: " + s);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.w(TAG, "Failed to create item: " + e.getMessage());
//            }
//        });
    }

    /*
        Create necessary files including folder
        It's observed that the behavior of uploading file with the same name of existing file
        varies cross drives. e.g. Onedrive always overwrite the existing one. Google drive create a
        new one instead
    */
    private void createAllocationFile(){
        File metadata = new File();
        java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" + NAME_ALLOCATION_FILE);
        //java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" + "TBM_SK601.BIN");    //test big size file over 10 MB
        metadata.setName(NAME_ALLOCATION_FILE);
        //Upload test only. For Google, folder cdfs is used. For MS, AAA is used.
        //metadata.setParents(Collections.singletonList("16IhpPc0_nrrDplc73YIevRI8C27ir1JG")); //cdfs
        //metadata.setParents(Collections.singletonList("CD26537079F955DF!5758"));  //AAA
//        upload(metadata, filePath).addOnSuccessListener(new OnSuccessListener<String>() {
//            @Override
//            public void onSuccess(String id) {
//                Log.d(TAG, "Upload OK. ID: " + id);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.w(TAG, "Upload failed. " + e.toString());
//            }
//        });
    }

    private void deleteObseleteFile(){
        File fileMetadata = new File();

        //test only. For Google, folder cdfs is used. For MS, AAA is used.
        //fileMetadata.setId("16IhpPc0_nrrDplc73YIevRI8C27ir1JG"); //cdfs
        fileMetadata.setId("CD26537079F955DF!5755");
//        delete(fileMetadata).addOnSuccessListener(new OnSuccessListener<String>() {
//            @Override
//            public void onSuccess(String s) {
//                Log.d(TAG, "delete OK. ID: " + s);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.w(TAG, "Failed to delete item: " + e.getMessage());
//            }
//        });
    }



}