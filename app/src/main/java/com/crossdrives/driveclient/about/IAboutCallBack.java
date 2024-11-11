package com.crossdrives.driveclient.about;

import com.crossdrives.driveclient.upload.IUploadCallBack;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;

public interface IAboutCallBack {
    /**
     * How successful results are handled
     *
     * @param file the file uploaded
     */
    void success(About about);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex);
}
