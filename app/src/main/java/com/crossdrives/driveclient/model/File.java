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

    String driveName;
    /*
        Additional user data
     */
    Integer integer;
    String string;

//    public String getDriveName() {
//        return driveName;
//    }
//
//    public void setDriveName(String driveName) {
//        this.driveName = driveName;
//    }

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

    public Integer getInteger() {
        return integer;
    }

    public String getString() {
        return string;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public void setString(String string) {
        this.string = string;
    }
}

