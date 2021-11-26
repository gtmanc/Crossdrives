package com.example.crossdrives;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Some good reference available in official site for SAF:
 * //https://developer.android.com/guide/topics/providers/document-provider
 * For google drive API:
 * //https://www.programcreek.com/java-api-examples/index.php?api=com.google.api.services.drive.model.FileList
 */

public class DriveServiceHelper {
    static private String TAG = "CD.DriveServiceHelper";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    private static DriveServiceHelper mInstance;
    String mPageToken = null;
    Boolean mIsEnd = false; //A flag indicates if we reach to the end of file in a query

    synchronized public static DriveServiceHelper Create(Drive driveService){
        if(mInstance == null) {
            mInstance = new DriveServiceHelper(driveService);
            Log.d(TAG, "mInstance:" + mInstance);
        }

        //Log.d(TAG, "return:" + mInstance);
        return mInstance;
    }

    public static DriveServiceHelper getInstance(){
        return mInstance;
    }

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile() {
        return Tasks.call(mExecutor, new Callable<String>() {
            @Override
            public String call() throws Exception {
                File metadata = new File()
                        .setParents(Collections.singletonList("root"))
                        .setMimeType("text/plain")
                        .setName("Untitled file");

                File googleFile = mDriveService.files().create(metadata).execute();
                if (googleFile == null) {
                    throw new IOException("Null result when requesting file creation.");
                }

                return googleFile.getId();
            }
        });
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(final String fileId) {
        return Tasks.call(mExecutor, new Callable<Pair<String, String>>() {
            @Override
            public Pair<String, String> call() throws Exception {
                // Retrieve the metadata as a File object.
                File metadata = mDriveService.files().get(fileId).execute();
                String name = metadata.getName();

                // Stream the file contents to a String.
                InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                Pair<String, String> p = new Pair(name, contents);
                return p;

                //return Pair.create(name, contents);
//            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
//                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
//                StringBuilder stringBuilder = new StringBuilder();
//                String line;
//
//                while ((line = reader.readLine()) != null) {
//                    stringBuilder.append(line);
//                }
//                String contents = stringBuilder.toString();
//
//                return Pair.create(name, contents);
//            }
            }
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public Task<Void> saveFile(final String fileId, final String name, final String content) {
        return Tasks.call(mExecutor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Create a File containing any metadata changes.
                File metadata = new File().setName(name);

                // Convert content to an AbstractInputStreamContent instance.
                ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

                // Update the metadata and contents.
                mDriveService.files().update(fileId, metadata, contentStream).execute();
                return null;
            }
        });
    }

    public Task<Void> deleteFile(final String fileId) {
        return Tasks.call(mExecutor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mDriveService.files().delete(fileId).execute();
                return null;
            }
        });
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     *
     * Google reference for query: https://developers.google.com/drive/api/v3/search-files
     *
     * A empty List<File> is set to FileList and returned if there is no more result for a query
     * To reset query, call method resetQuery()
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                FileList files = null;
                //return mDriveService.files().list().setSpaces("drive").execute();
                //There could be more result. Use page token to get.
                if(mDriveService == null){
                    Log.w(TAG, "mDriveService is null!");
                }
                if(mIsEnd != true) {
                    Log.d(TAG, "mPageToken:" + mPageToken);
                    files = mDriveService.files().list()
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name)")
                            .setPageToken(mPageToken)
                            //set to a small number can be used for test of loading more data in UI handling
                            //.setPageSize(10)
                            .execute();

                    mPageToken = files.getNextPageToken();
                    if(mPageToken == null){
                        Log.d(TAG, "There will be no more result for this query");
                        mIsEnd = true;
                    }
                }
                else
                {
                    /*
                    We are at the end of list. Here simply set an empty List<File> to FileList so that
                    the caller will get a List with size zero
                     */
                    List<File> ffiles = new ArrayList<>();
                    files = new FileList();
                    files.setFiles(ffiles);
                    Log.d(TAG, "We are at the end of list");
                }
                return files;
            }
        });
    }

    public Task<FileList> queryFiles(String nextPageToken, int pageSize) {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                FileList files = null;
                //return mDriveService.files().list().setSpaces("drive").execute();
                //There could be more result. Use page token to get.
                if(mDriveService == null){
                    Log.w(TAG, "mDriveService is null!");
                }
                if(mIsEnd != true) {
                    Log.d(TAG, "mPageToken:" + mPageToken);
                    files = mDriveService.files().list()
                            .setSpaces("drive")
                            .setFields("nextPageToken, files(id, name)")
                            .setPageToken(nextPageToken)
                            //set to a small number can be used for test of loading more data in UI handling
                            .setPageSize(pageSize)
                            .execute();
                }
                return files;
            }
        });
    }

    /*
    Reset a query
     */
    public void resetQuery()
    {
        mIsEnd = false;
        mPageToken = null;
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //type is required: https://stackoverflow.com/questions/21045091/no-activity-found-to-handle-intent-android-intent-action-open-document
        //Note: if text/plain is set. Any file except text will not be selectable in SAF.
        //intent.setType("text/plain");
        intent.setType("*/*");

        return intent;
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            final ContentResolver contentResolver, final Uri uri) {
        return Tasks.call(mExecutor, new Callable<Pair<String, String>>() {
            @Override
            public Pair<String, String> call() throws Exception {
                // Retrieve the document's display name from its metadata.
                String name;

                Cursor cursor = contentResolver.query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }

                // Read the document's contents as a String.
                String content;

                InputStream is = contentResolver.openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
//                try (InputStream is = contentResolver.openInputStream(uri);
//                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
//                    StringBuilder stringBuilder = new StringBuilder();
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        stringBuilder.append(line);
//                    }
//                    content = stringBuilder.toString();
//                }

                Pair<String, String> p = new Pair(name, content);
                return p;
                //return Pair.create(name, content);
            }
        });
    }
}

