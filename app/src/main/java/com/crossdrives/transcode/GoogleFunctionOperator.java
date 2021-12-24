package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleFunctionOperator extends BaseOperator{
    public static final String CONTAINS = "contains";

    final List<String> mF_OPs = new ArrayList<>();


    public GoogleFunctionOperator() {
        super();
        mF_OPs.add(CONTAINS);
        setOperators(mF_OPs);
    }
}
