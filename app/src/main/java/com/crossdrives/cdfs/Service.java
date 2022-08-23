package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.cdfs.allocation.Result;
import com.crossdrives.cdfs.download.Download;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.exception.CompletionException;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.InvalidArgumentException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.list.ICallbackList;
import com.crossdrives.cdfs.list.List;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.cdfs.upload.Upload;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    Interface for application.
    Google play service task api is adopted so that the application can decide the thread can be used
    to run.
 */
public class Service implements IService {
    private static String TAG = "CD.Service";
    CDFS mCDFS;

    public Service(CDFS cdfs) {
        mCDFS = cdfs;
    }

    //private final Executor sExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    IUploadProgressListener uploadProgressListener;
    IDownloadProgressListener downloadProgressListener;

    @Override
    public Task<com.crossdrives.cdfs.Result> list(Object nextPage) throws MissingDriveClientException, GeneralServiceException {
        List list = new List(mCDFS);
        final FileList[] fileList = {null};
        final Throwable[] throwables = {null};
        final java.util.List<Result>[] allocCheckResults = new java.util.List[]{null};
        com.crossdrives.cdfs.Result result = new com.crossdrives.cdfs.Result();
        Task task;


        Log.d(TAG, "Service: list files. nextPage: " + nextPage);

        mCDFS.requiresDriveClientNonNull();

        task = Tasks.call(mExecutor, new Callable<Object>() {
            @Override
            public com.crossdrives.cdfs.Result call() throws Exception {
//                listLock.lock();
                CompletableFuture<FileList> future = new CompletableFuture<>();
                list.list(null, new ICallbackList<FileList>() {
                    @Override
                    public void onSuccess(FileList files) {
                        fileList[0] = files;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.complete(files);
                    }

                    @Override
                    public void onCompleteExceptionally(FileList files, java.util.List<Result> results) {
                        fileList[0] = files;
                        allocCheckResults[0] = results;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.complete(files);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwables[0] = throwable;
//                        listLock.lock();
//                        queryFinished.signal();
//                        listLock.unlock();
                        future.completeExceptionally(throwable);
                    }
                });

//                queryFinished.await();
//                listLock.unlock();
                future.join();

                if (throwables[0] != null) {
                    throw new GeneralServiceException("", throwables[0]);
                }

                result.setFileList(fileList[0]);
                result.setResults(allocCheckResults[0]);
                return result;
            }
        });

        return task;
    }
    /*
        ins: input stream for the content to upload
        name: the name in CDFS space
        parent: the target folder name of the upload
    */
    @Override
    public Task upload(InputStream ins, String name, String parent) throws Exception {
        Upload upload = new Upload(mCDFS);
        Task task;
        final Throwable[] throwables = {null};


        Log.d(TAG, "CDFS Service: Upload");

        mCDFS.requiresDriveClientNonNull();

        if (ins == null) {
            throw new InvalidArgumentException("input stream for the file to pload is null"
                    , new Throwable(""));
        }

        task = Tasks.call(mExecutor, new Callable<File>() {

            @Override
            public File call() throws CompletionException, GeneralServiceException {
                IUploadProgressListener listener = defaultUploadProgressListener;
                if (uploadProgressListener != null)
                    listener = uploadProgressListener;

                CompletableFuture<File> future =
                        upload.upload(ins, name, parent, listener);

                future.exceptionally((ex) -> {
                    Log.w(TAG, "Upload completed exceptionally. " + ex.getMessage());
                    ex.printStackTrace();
                    throwables[0] = new Throwable(ex);
                    return null;
                });

                /*
                    Here we use join() instead of get() because we don't need to take care interrupt
                    or timeout
                 */
                File f = future.join();

                if (throwables[0] != null) {
                    throw new GeneralServiceException("", throwables[0]);
                }
                return f;
            }
        });

        return task;
    }

    public void setUploadProgressLisetener(IUploadProgressListener listener) {
        uploadProgressListener = listener;
    }

    IUploadProgressListener defaultUploadProgressListener = new IUploadProgressListener() {
        @Override
        public void progressChanged(Upload uploader) {
            Log.d(TAG, "Upload progress " + uploader.getState());
        }
    };

    /*
        Download a CDFS item
        fileID:   CDFS item ID
        parent:   CDFS folder where the item exists in
     */
    public Task<String> download(String fileID, String parent) throws MissingDriveClientException {

        IDownloadProgressListener listener = defaultDownloadProgressListener;
        if (downloadProgressListener != null)
            listener = downloadProgressListener;

        Download download = new Download(mCDFS, fileID, parent, listener);
        final Throwable[] throwables = {null};

        Log.d(TAG, "CDFS Service: Download");

        mCDFS.requiresDriveClientNonNull();

        return download.execute();
    }

    public void setDownloadProgressLisetener(IDownloadProgressListener listener) {
        downloadProgressListener = listener;
    }

    IDownloadProgressListener defaultDownloadProgressListener = new IDownloadProgressListener() {
        @Override
        public void progressChanged(Download downloader) {
            Log.d(TAG, "Download progress " + downloader.getState());
        }

    };
}
