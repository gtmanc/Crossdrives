package com.crossdrives.driveclient;

/**
 * A callback that describes how to deal with success and failure
 *
 * @param <Result> the result type of the successful action
 */
public interface ICallBack<Result> {
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