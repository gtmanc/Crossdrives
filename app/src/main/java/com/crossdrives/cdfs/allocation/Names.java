package com.crossdrives.cdfs.allocation;

import androidx.annotation.Nullable;

import com.crossdrives.cdfs.common.IConstant;
import com.crossdrives.cdfs.model.CdfsItem;

public class Names {

    static final String CDFS_FOLDER = "CDFS";
    static final String PREFIX_ALLOC_FILE = "Allocation";
    static final String SEPERATOR ="_";
    static final String EXT_NAME_ALLOC = ".cdfs";
    /*
        Return the allocation file name: Allocation_[folder_CdfsId].cdfs
        Input null to specify root of CDFS.
     */
    static public String allocFile(@Nullable String cdfsId){
        String name = PREFIX_ALLOC_FILE;

        if(cdfsId != null){
              name = name.concat(SEPERATOR + cdfsId);
        }
        name = name.concat(EXT_NAME_ALLOC);

        return name;
    }

    /*
        Get the name of base folder (not seen for CDFS).
    */
    static public String baseFolder(){
        return CDFS_FOLDER;
    }

    /*
        Get the complete path
*   */
    static public String CompletePath(CdfsItem item){
        String path = IConstant.CDFS_PATH_BASE;
        if(item != null){
            path = item.getPath().concat(item.getName());
        }

        return path;
    }

}
