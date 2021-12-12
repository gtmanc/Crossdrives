package com.crossdrives.transcode;

/*
    A transcode to decode a Google style query string and then ecoder it to various of ones. i.e. Microsoft.
    A query string input by user is in term of style: "query term" "operator" "values".
    So far, the transcoder only upport OData query parameter used by Microsoft Graph API.
 */
public class BaseTranscoder {
    String mQueryString;
    ConditionalOperator mConditionalOperator;
    GoogleQueryTerm mQueryTerms;
    GoogleQueryValues mValues;

    public BaseTranscoder(String qs) {
        mQueryString = qs;
        mConditionalOperator = new ConditionalOperator();
        mQueryTerms = new GoogleQueryTerm();
        mValues = new GoogleQueryValues();
    }

    private String mimetype(String qs) {
        int i = -1;
        String value;
        if(!qs.contains(GoogleQueryTerm.MIME_TYPE))
            return null;

        // Folder?
        //replace "query term"
        if(mValues.getValue(qs).equals(GoogleQueryValues.FOLDER)){
            qs.replace(GoogleQueryTerm.MIME_TYPE, "folder");
            qs.replace(GoogleQueryValues.FOLDER, "null");
        }
        //replace Equality operators
        if(mConditionalOperator.getOperator(qs).equals(GoogleEqualityOperator.EQUAL)){
            qs.replace(GoogleEqualityOperator.EQUAL, "ne");
        }

        return qs;
    }

    /*
        Separate the whole query string to query strings.
        Reference example 6 in https://www.geeksforgeeks.org/split-string-java-examples/
    */
    private String[] split(){
        String clause = new String("[");

        for(String s : mConditionalOperator.getCandidates()){
            clause.concat(s);
            //clause += s;
        }
        clause.concat("]+");

        return mQueryString.split(clause);
    }

    public String execute(){

    }
}
