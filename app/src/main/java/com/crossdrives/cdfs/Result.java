package com.crossdrives.cdfs;

import com.google.api.services.drive.model.FileList;

import java.util.List;

public class Result {
    FileList fileList;

    java.util.List<com.crossdrives.cdfs.allocation.Result> results;

    public void setResults(java.util.List<com.crossdrives.cdfs.allocation.Result> results)
    {
        this.results = results;
    }

    public void setFileList(FileList fileList){
        this.fileList = fileList;

    }

    public FileList getFileList() {
        return fileList;
    }

    public List<com.crossdrives.cdfs.allocation.Result> getResults() {
        return results;
    }
}
