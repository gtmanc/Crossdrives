package com.crossdrives.driveclient.model;

import java.io.OutputStream;

public class MediaData {
    class Addition{
        int integer;
    }

    OutputStream os;
    Addition addition = new Addition();

    public OutputStream getOs() {
        return os;
    }

    public int getAdditionInt() {
        return addition.integer;
    }

    public void setOs(OutputStream os) {
        this.os = os;
    }

    public void setAdditionInteger(int integer) {
        this.addition.integer = integer;
    }
}
