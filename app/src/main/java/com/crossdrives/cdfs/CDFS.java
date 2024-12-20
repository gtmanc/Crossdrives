package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.cdfs.allocation.Infrastructure;
import com.crossdrives.cdfs.data.Drive;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.util.Mapper;
import com.crossdrives.driveclient.IDriveClient;
import com.google.api.services.drive.model.File;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CDFS extends BaseCDFS{
    private static String TAG = "CDFS.CDFS";
    //List<IDriveClient> sClient = new ArrayList<>();
    //HashMap<String, IDriveClient> mDrives = new HashMap<>();

    private static CDFS mCDFS = null;
    private static Service mService;


    /*
    A flag used to synchronize the drive client callback. Always set to false each time an operation
    is performed.
    May use the thread synchronize object (e.g. condition variable) instead of the flag
     */
    private boolean msTaskfinished = false;

    static public CDFS getCDFSService(){
        if(mCDFS == null){
            Log.d(TAG, "Create instance CDFS");
            mCDFS = new CDFS();
            mService = new Service(mCDFS);
        }

        return mCDFS;
    }


    public Service getService(){return mService;};

    public CompletableFuture<CdfsItem> addClient(String name, IDriveClient client){
        Log.d(TAG, "Add client. Client: " + client.toString());
        HashMap<String, IDriveClient> clients = getClientsFromDrive();
        clients.put(name, client);

        //We have to ensure the infrastructure has been finished before the client is added.
        //Reason is that mDrives is a shared resource and a cdfs operation might be performed before
        //the basement has not yet built.
        Infrastructure builder = Infrastructure.getInstance();
        CompletableFuture<CdfsItem> InfBuildFuture = builder.buildAsync(clients);
        //CompletableFuture<Map.Entry<String, String>> future = builder.checkAndBuild(name, client);
        return InfBuildFuture.thenCompose((cdfsItem)->{
            CompletableFuture<CdfsItem> future = new CompletableFuture<>();
            Drive drive = new Drive(client);
            mDrives.put(name, drive);
            future.complete(cdfsItem);
            return future;
        });

        //createAllocationFile();
        //deleteObseleteFile();
    }

    public CompletableFuture<CdfsItem> addClients(HashMap<String, IDriveClient> clientsToAdd){

        Log.d(TAG, "Add clients. Clients: " + clientsToAdd);
        HashMap<String, IDriveClient> clients = getClientsFromDrive();
        clients.putAll(clientsToAdd);

        //We have to ensure the infrastructure has been finished before the client is added.
        //Reason is that mDrives is a shared resource and a cdfs operation might be performed before
        //the basement has not yet built.
        Infrastructure builder = Infrastructure.getInstance();
        CompletableFuture<CdfsItem> InfBuildFuture = builder.buildAsync(clients);
        //CompletableFuture<Map.Entry<String, String>> future = builder.checkAndBuild(name, client);
        return InfBuildFuture.thenCompose((cdfsItem)->{
            CompletableFuture<CdfsItem> future = new CompletableFuture<>();
            HashMap<String, Drive> drives = Mapper.reValue(clientsToAdd, client->{
                return new Drive(client);
            });
            mDrives.putAll(drives);
            future.complete(cdfsItem);
            return future;
        });

        //createAllocationFile();
        //deleteObseleteFile();
    }
    private HashMap<String, IDriveClient> getClientsFromDrive(){
        return Mapper.reValue(mDrives, drive->{
            return drive.getClient();
        });
    }

    //public void removeClient(int i){
    public boolean removeClient(String name){
        //sClient.remove(i);
        boolean result = true;
        Drive drive;
        drive = mDrives.remove(name);
        if(drive == null){
            Log.d(TAG, "Remove client failed!");
            mDrives.forEach((k, v)-> Log.d(TAG, "Key:" + k + " Value: " + v));
            result = false;
        }
        return result;
    }
//    public IDriveClient getClient(int i){
//        return sClient.get(i);
//    }
    public IDriveClient getClient(String name){
        //return sClient.indexOf(client);
        return mDrives.get(name).getClient();
    }

    public HashMap<String, Drive> getDrives(){
        return mDrives;
    }

    /*
        Create necessary files including folder
        It's observed that the behavior of uploading file with the same name of existing file
        varies cross drives. e.g. Onedrive always overwrite the existing one. Google drive create a
        new one instead
    */
//    private void createAllocationFile(){
//        File metadata = new File();
//        java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" + NAME_ALLOCATION_FILE);
//        //java.io.File filePath = new java.io.File(mActivity.getFilesDir() + "/" + "TBM_SK601.BIN");    //test big size file over 10 MB
//        metadata.setName(NAME_ALLOCATION_FILE);
//        //Upload test only. For Google, folder cdfs is used. For MS, AAA is used.
//        //metadata.setParents(Collections.singletonList("16IhpPc0_nrrDplc73YIevRI8C27ir1JG")); //cdfs
//        //metadata.setParents(Collections.singletonList("CD26537079F955DF!5758"));  //AAA
////        upload(metadata, filePath).addOnSuccessListener(new OnSuccessListener<String>() {
////            @Override
////            public void onSuccess(String id) {
////                Log.d(TAG, "Upload OK. ID: " + id);
////            }
////        }).addOnFailureListener(new OnFailureListener() {
////            @Override
////            public void onFailure(@NonNull Exception e) {
////                Log.w(TAG, "Upload failed. " + e.toString());
////            }
////        });
//    }

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