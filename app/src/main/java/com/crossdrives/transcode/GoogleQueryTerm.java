package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleQueryTerm extends BaseQueryTerm{
    public static final String MIME_TYPE = "mimeType";
    int index;
    String operator;
    final List<String> mTerms = new ArrayList<>();

    public GoogleQueryTerm() {
        super();
        //terms present in front of operator
        mTerms.add("mimeType");
        mTerms.add("name");
        mTerms.add("fullText");
        //TODO: may need to implement logoc for terms present prior to operator

        setQueryTerms(mTerms);
     }

}
