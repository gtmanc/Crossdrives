package com.crossdrives.cdfs;

import com.crossdrives.driveclient.IDriveClient;

import java.util.ArrayList;
import java.util.List;

public class CDFS {

    List<IDriveClient> mClient = new ArrayList<>();

    public CDFS() {

    }

    public void addClient(IDriveClient client){
        mClient.add(client);
    }

    private void qruery(){
        IDriveClient client;

        client = mClient.get(0);
        client.query().
    }
}
