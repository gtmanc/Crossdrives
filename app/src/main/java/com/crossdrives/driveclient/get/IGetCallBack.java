package com.crossdrives.driveclient.get;

public interface IGetCallBack <T> {
    /**
     * How successful results are handled
     *
     * @param result
     */

    void success(T result);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex);
}
