package com.crossdrives.driveclient;

import java.io.IOException;

public interface IDownloadCallBack <Result>{
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
