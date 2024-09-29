package com.crossdrives.cdfs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.crossdrives.cdfs.list.ListResult;
import com.crossdrives.cdfs.common.ResultCodes;
import com.crossdrives.cdfs.create.Create;
import com.crossdrives.cdfs.delete.Delete;
import com.crossdrives.cdfs.delete.IDeleteProgressListener;
import com.crossdrives.cdfs.download.Download;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.exception.CompletionException;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.InvalidArgumentException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.exception.PermissionException;
import com.crossdrives.cdfs.list.ICallbackList;
import com.crossdrives.cdfs.list.List;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.cdfs.model.CdfsItem;
import com.crossdrives.cdfs.move.IMoveItemProgressListener;
import com.crossdrives.cdfs.move.Move;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.cdfs.upload.Upload;
import com.crossdrives.msgraph.SnippetApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
    Interface for application.
    Google play service task api is adopted so that the application can decide the thread can be used
    to run.
 */
public class Service{
    private static String TAG = "CD.Service";
    CDFS mCDFS;

    public Service(CDFS cdfs) {
        mCDFS = cdfs;
    }

    //private final Executor sExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    IUploadProgressListener uploadProgressListener;
    IDownloadProgressListener downloadProgressListener;
    IDeleteProgressListener deleteProgressListener;

    public Task<ListResult> list(@Nullable java.util.List<CdfsItem> parents) throws MissingDriveClientException, GeneralServiceException {
        List list = new List(mCDFS, parents);
        final FileList[] fileList = {null};
        final Throwable[] throwables = {null};
        final java.util.List<ListResult>[] allocCheckResults = new java.util.List[]{null};
        ResultCodes result = new ResultCodes();
        Task task;


        Log.d(TAG, "Service: list files");

        mCDFS.requiresDriveClientNonNull();

        task = list.execute();

//        task = Tasks.call(mExecutor, new Callable<Object>() {
//            @Override
//            public ResultCodes call() throws Exception {
////                listLock.lock();
//                CompletableFuture<FileList> future = new CompletableFuture<>();
//                list.list(parent, new ICallbackList<FileList>() {
//                    @Override
//                    public void onSuccess(FileList files) {
//                        fileList[0] = files;
////                        listLock.lock();
////                        queryFinished.signal();
////                        listLock.unlock();
//                        future.complete(files);
//                    }
//
//                    @Override
//                    public void onCompleteExceptionally(FileList files, java.util.List<ListResult> results) {
//                        fileList[0] = files;
//                        allocCheckResults[0] = results;
////                        listLock.lock();
////                        queryFinished.signal();
////                        listLock.unlock();
//                        future.complete(files);
//                    }
//
//                    @Override
//                    public void onFailure(Throwable throwable) {
//                        throwables[0] = throwable;
////                        listLock.lock();
////                        queryFinished.signal();
////                        listLock.unlock();
//                        future.completeExceptionally(throwable);
//                    }
//                });
//
////                queryFinished.await();
////                listLock.unlock();
//                future.join();
//
//                if (throwables[0] != null) {
//                    throw new GeneralServiceException("", throwables[0]);
//                }
//
//                result.setFileList(fileList[0]);
//                result.setErrCodes(allocCheckResults[0]);
//                return result;
//            }
//        });
//
        return task;
    }
    /*
        ins: input stream for the content to upload
        name: the name in CDFS space
        parent: the target folder name of the upload
    */
    public Task upload(InputStream ins, String name, java.util.List<CdfsItem> parents) throws Exception {
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
                        upload.upload(ins, name, parents, listener);

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
    public Task<String> download(String fileID, CdfsItem parent) throws MissingDriveClientException, PermissionException {

        IDownloadProgressListener listener = defaultDownloadProgressListener;
        if (downloadProgressListener != null)
            listener = downloadProgressListener;
        int StatusChecked =
        ContextCompat.checkSelfPermission(SnippetApp.getAppContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(StatusChecked == PackageManager.PERMISSION_DENIED){
            Log.d(TAG, "Permission for accessing download folder has not yet granted!");
            throw new PermissionException("Permission for accessing download folder has not yet granted!", new Throwable(""));
        }

        Download download = new Download(mCDFS, fileID, parent, listener);
        final Throwable[] throwables = {null};

        Log.d(TAG, "CDFS Service: Download");

        mCDFS.requiresDriveClientNonNull();

        return download.execute();
    }

    public void setDownloadProgressListener(IDownloadProgressListener listener) {
        downloadProgressListener = listener;
    }

    IDownloadProgressListener defaultDownloadProgressListener = new IDownloadProgressListener() {
        @Override
        public void progressChanged(Download downloader) {
            Log.d(TAG, "Download progress " + downloader.getState());
        }

    };

    public Task<com.crossdrives.driveclient.model.File> delete(String fileID, CdfsItem parent) throws MissingDriveClientException, PermissionException {

        IDeleteProgressListener listener = defaultDeleteProgressListener;
        if (deleteProgressListener != null)
            listener = deleteProgressListener;

        Delete deleter = new Delete(mCDFS, fileID, parent, listener);
        final Throwable[] throwables = {null};

        Log.d(TAG, "CDFS Service: Delete");

        mCDFS.requiresDriveClientNonNull();

        return deleter.execute();
    }

    IDeleteProgressListener defaultDeleteProgressListener = new IDeleteProgressListener() {
        @Override
        public void progressChanged(Delete deleter) {
            Log.d(TAG, "Delete in progress " + deleter.getState());
        }
    };

    public void setDeleteProgressListener(IDeleteProgressListener listener) {
        deleteProgressListener = listener;
    }

    public Task<com.crossdrives.driveclient.model.File> move(CdfsItem fileID, CdfsItem src, CdfsItem dest, IMoveItemProgressListener usrListener) throws MissingDriveClientException, PermissionException {

        IMoveItemProgressListener listener = defaultMoveItemProgressListener;
        if (usrListener != null)
            listener = usrListener;

        Move mover = new Move(mCDFS, fileID, src, dest, listener);
        final Throwable[] throwables = {null};

        Log.d(TAG, "CDFS Service: move");

        mCDFS.requiresDriveClientNonNull();

        return mover.execute();
    }

    IMoveItemProgressListener defaultMoveItemProgressListener = new IMoveItemProgressListener() {
        @Override
        public void progressChanged(Move mover) {
            Log.d(TAG, "Move in progress " + mover.getState());
        }
    };


    public Task<com.crossdrives.driveclient.model.File>  create(String name, java.util.List<CdfsItem> parents) throws MissingDriveClientException, PermissionException {

//        IDeleteProgressListener listener = defaultDeleteProgressListener;
//        if (deleteProgressListener != null)
//            listener = deleteProgressListener;

        Create creator = new Create(mCDFS, name, parents);
        final Throwable[] throwables = {null};

        Log.d(TAG, "CDFS Service: Create");

        mCDFS.requiresDriveClientNonNull();

        return creator.execute();
    }

}
