package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

class ConditionalOperator extends BaseOperator {
    int index;
    String operator;
    final List<String> mC_OPs = new ArrayList<>();

    public ConditionalOperator() {
        super();
        mC_OPs.add("or");
        mC_OPs.add("and");
        setOperators(mC_OPs);
    }
}
