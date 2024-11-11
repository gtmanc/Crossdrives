package com.crossdrives.cdfs.allocation;

public interface ResultCode {

    final int SUCCESS = 0;
    /*
        Code for single check
     */
    final int ERR_SEQ_OVER_SEG = 1;
    final int ERR_SIZE_OVER_MAX = 2;

    /*
        Code for check items crossly
     */
    final int ERR_CDFSSIZE_NOT_IDENTICAL = 3;
    final int ERR_TOTALSEG_NOT_IDENTICAL = 4;
    //final int ERR_MAXSEQ_NOTEQUAL_TOTALSEG = 5;
    final int ERR_MISSING_ITEM = 5;

}
