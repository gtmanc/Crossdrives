package com.crossdrives.cdfs.upload;

public interface IUploadCallbck<Result> {
    void onSuccess(Result result);

    void onFailure(Throwable throwable);

}
