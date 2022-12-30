package com.crossdrives.cdfs.util.collection;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Optional;

public class Files {
    static String TAG = "CD.CDFS.Collection.Files";


    /*
        Search specified item in the given item list if the name matches.
        Input:
        fileList: item list
        name: name of drive item
     */
    public static File getFolder(@NonNull FileList fileList, String name){
        getFromFiles(fileList, name);
    }

    /*
        Search specified item in the given item list if the name matches.
        Input:
        fileList: item list
        name: name of drive item
     */
    public static File getFromFiles(@NonNull FileList fileList, String name){
        Optional<File> files;
        File result = null;
        if(fileList.getFiles().size() > 0) {
            files = fileList.getFiles().stream().filter((file) -> {
                return file.getName().compareToIgnoreCase(name) == 0 ? true : false;
            }).findAny();

            if (files.isPresent()) {
                result = files.get();

            } else {
                Log.w(TAG, "Can not find the specified item: " + name);
            }
        }else{
            Log.w(TAG, "No files found in the specified folder");
        }

        return result;
    }

}
