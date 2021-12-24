package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.List;

public class GoogleQueryTerm extends BaseQueryTerm{
    public static final String MIMETYPE = "mimeType";
    public static final String NAME = "name";
    public static final String FULLTEXT = "fullText";

    int index;
    String operator;
    final List<String> mTerms = new ArrayList<>();

    public GoogleQueryTerm() {
        super();
        //terms present in front of operator
        mTerms.add(MIMETYPE);
        mTerms.add(NAME);
        mTerms.add(FULLTEXT);

        setQueryTerms(mTerms);
     }

}
