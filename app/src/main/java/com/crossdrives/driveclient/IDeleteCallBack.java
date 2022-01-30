package com.crossdrives.driveclient;

public interface IDeleteCallBack <Result>{
    /**
     * How successful results are handled
     *
     * @param result the result
     */
    void success(final Result result);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex);
}
