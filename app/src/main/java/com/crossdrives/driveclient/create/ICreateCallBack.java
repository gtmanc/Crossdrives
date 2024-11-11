package com.crossdrives.driveclient.create;

public interface ICreateCallBack <ID>{
    /**
     * How successful results are handled
     *
     * @param id the file ID
     */

    void success(ID id);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex);
}
