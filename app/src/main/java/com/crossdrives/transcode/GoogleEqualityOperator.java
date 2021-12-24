package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleEqualityOperator extends BaseOperator{
    public static final String EQUAL = "=";
    public static final String NOT_EQUAL = "!=";

    final List<String> mE_OPs = new ArrayList<>();


    public GoogleEqualityOperator() {
        super();
        mE_OPs.add(EQUAL);
        mE_OPs.add(NOT_EQUAL);
        setOperators(mE_OPs);
    }
}
