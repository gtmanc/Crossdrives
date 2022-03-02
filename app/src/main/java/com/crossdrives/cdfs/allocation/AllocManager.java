package com.crossdrives.cdfs.allocation;

import android.database.Cursor;
import android.util.Log;

import com.crossdrives.cdfs.Drive;
import com.crossdrives.cdfs.IAllocManager;
import com.crossdrives.cdfs.data.DBHelper;
import com.crossdrives.cdfs.model.AllocContainer;
import com.crossdrives.cdfs.model.AllocationItem;
import com.crossdrives.data.DBConstants;
import com.crossdrives.msgraph.SnippetApp;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;

public class AllocManager implements IAllocManager {
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

    static public int checkCompatibility(AllocContainer container){
        int result = ERR_COMPATIBILITY_SUCCESS;

        /*
            Expect more checks need to be added in the future.
         */
        if(container.getVersion() != mVersion){
            result = ERR_COMPATIBILITY_VER_NOT_COMPATIBLE;
            Log.w(TAG, "Expected version: " + mVersion + " version in container: " + container.getVersion());
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
        //addTestContentGoogle(container);
        addTestContentMicrosoft(container);
        json = gson.toJson(container);
        return json;
    }

    static private void addTestContentGoogle(AllocContainer container){
        java.util.List<AllocationItem> items = new ArrayList<>();
        AllocationItem item = new AllocationItem();
        item.setDrive("Google");
        item.setName("Test1.txt");
        item.setPath("Root");
        item.setSequence(1);
        item.setTotalSeg(2);
        item.setSize(32);
        item.setCDFSItemSize(64);
        item.setAttrFolder(false);
        container.setAllocItem(item);
    }
    static private void addTestContentMicrosoft(AllocContainer container){
        java.util.List<AllocationItem> items = new ArrayList<>();
        AllocationItem item = new AllocationItem();
        item.setDrive("Microsoft");
        item.setName("Test1.txt");
        item.setPath("Root");
        item.setSequence(2);
        item.setTotalSeg(2);
        item.setSize(32);
        item.setCDFSItemSize(64);
        item.setAttrFolder(false);
        container.setAllocItem(item);
    }
    static public void saveNewAllocation(AllocContainer container, String drive)
    {
        DBHelper dh = new DBHelper(SnippetApp.getAppContext());
        AllocationItem item = container.getAllocItem().get(0);
        int deleted;

        Log.d(TAG, "Save new allocation. Drive: " + drive);

        dh.setName(item.getName());
        dh.setDrive(item.getDrive());
        dh.setPath(item.getPath());
        dh.setSequence(item.getSequence());
        dh.setTotalSegment(item.getTotalSeg());
        dh.setSize(item.getSize());
        dh.setCDFSItemSize(item.getCDFSItemSize());

        /*
            Delete the old rows and then insert the new items
         */
        deleted = dh.delete(DBConstants.ALLOCITEMS_LIST_COL_DRIVENAME, "\"" + drive + "\"");
        Log.d(TAG, "Drive " + drive + " items have been deleted: " + deleted);
        dh.insert();
    }



    public OutputStream upload(File file){
        OutputStream stream = null;

        return stream;
    }

}