package com.crossdrives.cdfs.model;

import com.google.api.client.http.FileContent;

import java.io.File;

public class updateContent {
    String ID;
    java.io.File mediaContent;

    public String getID() {
        return ID;
    }

    public File getMediaContent() {
        return mediaContent;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setMediaContent(File mediaContent) {
        this.mediaContent = mediaContent;
    }
}
