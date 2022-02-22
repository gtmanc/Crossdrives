package com.crossdrives.cdfs;

import android.util.Log;

import com.crossdrives.cdfs.model.AllocContainer;
import com.google.gson.Gson;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AllocManager implements IAllocManager{
    static private final String TAG = "CD.AllocManager";
    static private final int mVersion = 1;
    //List<AllocContainer> mAllocations = new ArrayList<>();

    static public AllocContainer toContainer(OutputStream stream){
        AllocContainer container;
        Gson gson = new Gson();
        Drive drive;

        container = gson.fromJson(stream.toString(), AllocContainer.class);
        return container;
    }

    static int checkCompatibility(AllocContainer container){
        int result = ERR_COMPATIBILITY_SUCCESS;

        /*
            Expect more checks need to be added in the future.
         */
        if(container.getVersion() != mVersion){
            result = ERR_COMPATIBILITY_VER_NOT_COMPATIBLE;
        }
        return result;
    }

    /*
        Create a new allocation file
     */
    static public String newAllocation(){
        AllocContainer container = new AllocContainer();
        Gson gson = new Gson();
        String json;
        container.setVersion(mVersion);
        json = gson.toJson(container);
        return json;
    }

    static public void saveNewAllocation(AllocContainer container)
    {
        Log.d(TAG, "Allocation file version:" +Integer.toString(container.getVersion()));
    }

    public OutputStream upload(File file){
        OutputStream stream = null;

        return stream;
    }

}
