package com.crossdrives.cdfs.model;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.File;

public class UpdateFile {
    String ID;
    File metadata;
    FileContent mediaContent;

    public String getID() {
        return ID;
    }

    public File getMetadata() {
        return metadata;
    }

    public FileContent getMediaContent() {
        return mediaContent;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setMetadata(File metadata) {
        this.metadata = metadata;
    }

    public void setMediaContent(FileContent mediaContent) {
        this.mediaContent = mediaContent;
    }
}
