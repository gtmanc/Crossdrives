package com.crossdrives.cdfs.allocation;

public class Result{
    int err;
    String reason;

    public Result(int err, String reason) {
        this.err = err;
        this.reason = reason;
    }

    public Result() {
    }

    public void setErr(int err) {
        this.err = err;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getErr() {
        return err;
    }

    public String getReason() {
        return reason;
    }

}
