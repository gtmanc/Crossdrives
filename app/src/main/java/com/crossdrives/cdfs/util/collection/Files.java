package com.crossdrives.cdfs.util.collection;

import static com.crossdrives.cdfs.common.IConstant.MIMETYPE_FOLDER;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Optional;

public class Files {
    static String TAG = "CD.CDFS.Collection.Files";

    static String FILTER_FOLDER = "is_folder";
    static String FILTER_FILE = "is_file";
    static String filter;


    /*
        Search specified item in the given item list if the name matches and it's a folder.
        Input:
        fileList: item list
        name: name of drive item
     */
    public static File getFolder(@NonNull FileList fileList, String name){
        return get(fileList, name, FILTER_FOLDER);
    }

    /*
        Search specified item in the given item list if the name matches.
        Input:
        fileList: item list
        name: name of drive item
     */
    public static File getFromFiles(@NonNull FileList fileList, String name){
//        Optional<File> files;
//        File result = null;
//        if(fileList.getFiles().size() > 0) {
//            files = fileList.getFiles().stream().filter((file) -> {
//                return file.getName().compareToIgnoreCase(name) == 0 ? true : false;
//            }).findAny();
//
//            if (files.isPresent()) {
//                result = files.get();
//
//            } else {
//                Log.w(TAG, "Can not find the specified item: " + name);
//            }
//        }else{
//            Log.w(TAG, "No files found in the specified folder");
//        }

        return get(fileList, name, null);
    }

    static File get(@NonNull FileList fileList, String name, String filter){
        Optional<File> files;
        File result = null;

        if(fileList.getFiles().size() > 0) {
            files = fileList.getFiles().stream().filter((file) -> {

                //First of all, if name doesn't match, report false.
                //Log.d(TAG, "name matches?");
                if(file.getName().compareToIgnoreCase(name) != 0){
                    return false;
                }

                //if no additional filter required, report ok right here.
                if(filter == null){
                    return true;
                }

                //Log.d(TAG, "Apply additional filter?");
                //Additional filters
                if(filter.compareToIgnoreCase(FILTER_FILE) == 0){   // mimeType is file
                    //https://developers.google.com/drive/api/guides/folder
                    //a folder is a file with the MIME type application/vnd.google-apps.folder and with no extension.

                    //Log.d(TAG, "mine type: " +file.getMimeType());
                    if(file.getMimeType().compareToIgnoreCase(MIMETYPE_FOLDER) == 0){
                        return false;
                    }
                }else if(filter.compareToIgnoreCase(FILTER_FOLDER) == 0){ // mimeType is folder
                    //Log.d(TAG, "mine type: " +file.getMimeType());
                    if(file.getMimeType().compareToIgnoreCase(MIMETYPE_FOLDER) != 0){
                        return false;
                    }
                }

                return true;
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
