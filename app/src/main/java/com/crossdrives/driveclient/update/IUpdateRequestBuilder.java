package com.crossdrives.driveclient.update;

public interface IUpdateRequestBuilder {
    public String OP_LOCK = "lock";
    public String OP_UNLOCK = "lock";

    public IUpdateRequest buildRequest(String fileID, String op);
}
