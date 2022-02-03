package com.crossdrives.driveclient.list;

/**
 * A callback that describes how to deal with success and failure
 *
 * @param <Result> the result type of the successful action
 */
public interface IFileListCallBack<Result, NextPage> {
    /**
     * How successful results are handled
     *
     * @param result the result
     */
    /*
       design remark:
       The next page object implemented in Google drive is different from Onedrive.
       i.e. Google drive is String but Onedrive is Builder class. Here I leveraged Google FileList
       because I don't want to define a new class for propagate the result to caller layer.
       So, you see a little bit ugly interface. :)
     */
    void success(final Result result, final NextPage page);

    /**
     * How failures are handled
     *
     * @param ex the exception
     */
    void failure(final String ex);
}