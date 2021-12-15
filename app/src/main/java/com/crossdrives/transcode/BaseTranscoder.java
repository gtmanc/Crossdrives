package com.crossdrives.transcode;

import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

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
    /*
        The maps are used to convert query term mimeType
     */
    static HashMap<String, String > mQueryValueMap = new HashMap<>();
    static HashMap<String, String > mQueryEqualityMap = new HashMap<>();
    static{
        mQueryEqualityMap.put(GoogleEqualityOperator.EQUAL,"ne");
        mQueryEqualityMap.put(GoogleEqualityOperator.NOT_EQUAL,"eq");
    }
    static{
        mQueryValueMap.put(GoogleQueryValues.FOLDER,"null");
    }

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
        String equality, value;

        Log.d(TAG, "Converting mimeType...");
        //Exit if query term 'mineType' doesn't present
        if(!google_qs.contains(GoogleQueryTerm.MIME_TYPE)) {
            Log.d(TAG, "Query term mimetype not found:" + google_qs);
            return null;
        }

        value = mGoogleQueryValues.getValue(google_qs);
        //Log.d(TAG, "Query value folder presents?");
        if(value != null){
            Log.d(TAG, "Query value found: " + value);
            //replace "query term"
            graph_qs = graph_qs.replace(GoogleQueryTerm.MIME_TYPE, mQueryEqualityMap.get(value));
            //replace "query value"
            graph_qs = graph_qs.replace(value, "null");
        }else{
            Log.w(TAG, "Unrecognized value: " + value);
        }
        //replace Equality operators
        equality = mGoogleEqualityOperator.getOperator(google_qs);
        if(equality != null){
            Log.d(TAG, "Equality operator found: " + equality);
            graph_qs = graph_qs.replace(equality, mQueryEqualityMap.get(equality));
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
