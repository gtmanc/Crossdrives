package com.crossdrives.driveclient.model;

public class File {
    /*
        File with associated meta data in remote drive
     */
    com.google.api.services.drive.model.File file;
    /*
        The local file that is uploaded to the remote initially.
     */
    java.io.File originalLocalFile;

    public com.google.api.services.drive.model.File getFile() {
        return file;
    }

    public java.io.File getOriginalLocalFile() {
        return originalLocalFile;
    }

    public void setFile(com.google.api.services.drive.model.File file) {
        this.file = file;
    }

    public void setOriginalLocalFile(java.io.File originalLocalFile) {
        this.originalLocalFile = originalLocalFile;
    }
}

