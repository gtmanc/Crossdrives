package com.crossdrives.cdfs.allocation;

import com.crossdrives.cdfs.common.ResultCodes;

public class AllocResultCodes extends ResultCodes {
    int err;
    String reason;

    /*
        Code for single check
     */
    static public final int ERR_SEQ_OVER_SEG = 1;
    static public final int ERR_SIZE_OVER_MAX = 2;

    /*
        Code for check items crossly
     */
    static public final int ERR_CDFSSIZE_NOT_IDENTICAL = 3;
    static public final int ERR_TOTALSEG_NOT_IDENTICAL = 4;
    //final int ERR_MAXSEQ_NOTEQUAL_TOTALSEG = 105;
    static public final int ERR_MISSING_ITEM = 5;

    public AllocResultCodes(int err, String reason) {
        this.err = err;
        this.reason = reason;
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

    String getResultCodeString(){
        return "Unknown allocation result code";
    }
}
