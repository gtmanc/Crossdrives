package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleFunctionOperator extends BaseOperator{
<<<<<<< HEAD
    public static final String CONTAINS = "contains";
=======
    static final String CONTAINS = "contains";
    static final String EQUALS = "=";
>>>>>>> 983262f (#21 infrastructure build process)

    final List<String> mF_OPs = new ArrayList<>();


    public GoogleFunctionOperator() {
        super();
        mF_OPs.add(CONTAINS);
        setOperators(mF_OPs);
    }
}
