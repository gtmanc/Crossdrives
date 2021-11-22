package com.crossdrives.driveclient;

import com.google.api.services.drive.model.FileList;


public interface IFileListRequest {
    /**
     * Sets the select clause for the request
     *
     * @param value the select clause
     * @return the updated request
     */
    public IFileListRequest select(final String value);

    /**
     * Sets next page for the request
     *
     * @param page the page for the request. Set to null to get 1st page.
     * @return the updated request
     */
    public IFileListRequest setNextPage(final Object page);

    /**
     * Sets page size
     *
     * @param size the page size for the request.
     * @return the updated request
     */
    public IFileListRequest setPageSize(final int size);

    /*
        Execute the request

     * @param callback gets called when response got from remote
     */
    public void run(ICallBack<FileList, Object> callback);
}
