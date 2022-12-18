package com.crossdrives.cdfs;

import java.util.List;

public class Result<T> {
    T result;

    java.util.List<com.crossdrives.cdfs.allocation.Result> errCodes;

    public void setErrCodes(java.util.List<com.crossdrives.cdfs.allocation.Result> errCodes)
    {
        this.errCodes = errCodes;
    }

    public void setFileList(T result){
        this.result = result;

    }

    public T getFileList() {
        return result;
    }

    public List<com.crossdrives.cdfs.allocation.Result> getErrCodes() {
        return errCodes;
    }
}
