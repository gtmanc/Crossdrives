package com.crossdrives.transcode;

import android.text.TextUtils;
import android.util.Log;

/*
    A transcode to decode a Google style query string and then encode it to various of ones. i.e. Microsoft.
    A query string input by user is in term of style: "query term" "operator" "values".
    So far, the transcoder only supports OData query parameter used by Microsoft Graph API.
    Implementation:
    1. A original google query string is given when transcoder concrete is constructed
    2. Split the given query string to a string array. Split by conditional operator (i.e. "or", "and")
    3. Transcode the split string for each
    4. Concatenate the transcoded string to a new query string
 */
public class BaseTranscoder {
    private final String TAG = "CD.BaseTranscoder";
    String mGivenQueryString;
    ConditionalOperator mConditionalOperator;
    GoogleQueryTerm mGoogleQueryTerms;
    GoogleQueryValues mGoogleQueryValues;
    GoogleEqualityOperator mGoogleEqualityOperator;

    public BaseTranscoder(String qs) {
        mGivenQueryString = qs;
        mConditionalOperator = new ConditionalOperator();
        mGoogleQueryTerms = new GoogleQueryTerm();
        mGoogleQueryValues = new GoogleQueryValues();
        mGoogleEqualityOperator = new GoogleEqualityOperator();
    }

    /*
        e.g. To query item which is a folder
        Google: mimeType = 'application/vnd.google-apps.folder'
        Graph: folder != null
     */
    private String mimetype(final String google_qs) {
        int i = -1;
        String graph_qs = new String(google_qs.toString());

        Log.d(TAG, "Converting mimeType...");
        //Exit if query term 'mineType' doesn't present
        if(!google_qs.contains(GoogleQueryTerm.MIME_TYPE)) {
            Log.d(TAG, "Query term mimetype not found:" + google_qs);
            return null;
        }

        //Folder?
        //replace "query term"
        //Log.d(TAG, "Query value folder presents?");
        if(mGoogleQueryValues.getValue(google_qs).equals(GoogleQueryValues.FOLDER)){
            Log.d(TAG, "Query value 'folder' found!");
            graph_qs = graph_qs.replace(GoogleQueryTerm.MIME_TYPE, "folder");
            graph_qs = graph_qs.replace(GoogleQueryValues.FOLDER, "null");
        }
        //replace Equality operators
        if(mGoogleEqualityOperator.getOperator(google_qs).equals(GoogleEqualityOperator.EQUAL)){
            Log.d(TAG, "Equality operator '=' found!");
            graph_qs = graph_qs.replace(GoogleEqualityOperator.EQUAL, "ne");
        }

        Log.d(TAG, "mimeType converted string: " + graph_qs);

        return graph_qs;
    }

    /*
        Separate the whole query string to query strings.
        Reference example 6 in https://www.geeksforgeeks.org/split-string-java-examples/
    */
    private String[] split(){
        String regex = "";

        regex = TextUtils.join("|", mConditionalOperator.getCandidates());

        Log.d(TAG, "Regex for split:" + regex);

        return mGivenQueryString.split(regex);
    }

    public String execute(){
        String[] transcoded = new String[]{""};
        String[] separated;
        String s;

        Log.d(TAG, "Given gooogle query string:" + mGivenQueryString);

        separated = split();

        Log.d(TAG, "Length of split strings:" + separated.length);
        for(int i = 0 ; i < separated.length; i++){
            transcoded[i] = mimetype(separated[i]);
        }

        /*
            Good reference for converting striing arrat to string
            https://stackoverflow.com/questions/5283444/convert-array-of-strings-into-a-string-in-java/5283753
        */
        s = TextUtils.join(" ", transcoded);
        Log.d(TAG, "Converted Odataquery string:" + s);
        return s;
    }
}
