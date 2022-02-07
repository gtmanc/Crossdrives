package com.crossdrives.transcode;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
    ConditionalOperator mConditionalOperator;
    GoogleQueryTerm mGoogleQueryTerms;



    List<IConvert> mConvertions = new ArrayList<>();


    public BaseTranscoder(List<IConvert> convertions) {
        mConditionalOperator = new ConditionalOperator();
        mGoogleQueryTerms = new GoogleQueryTerm();
        mConvertions = convertions;
    }

    interface IConvert{
        String convert(final String qs);
    }
//
//    class ConvertMimeType implements IConvert{
//        /*
//        Query term: mimeType
//        e.g. To query item which is a folder
//        Google: mimeType = 'application/vnd.google-apps.folder'
//        Graph: folder != null
//     */
//        public String convert(final String google_qs) {
//            String graph_qs = new String(google_qs.toString());
//            String equality, value;
//
//            Log.d(TAG, "Converting mimeType...");
//            //Exit if query term 'mineType' doesn't present
//            if(!google_qs.contains(GoogleQueryTerm.MIMETYPE)) {
//                Log.d(TAG, "Query term mimetype not found:" + google_qs);
//                return null;
//            }
//
//            value = mGoogleQueryValues.getValue(google_qs);
//            //Log.d(TAG, "Query value folder presents?");
//            if(value != null){
//                Log.d(TAG, "Query value found: " + value);
//                //replace "query term"
//                graph_qs = graph_qs.replace(GoogleQueryTerm.MIMETYPE, mQueryValueMap.get(value));
//                //replace "query value"
//                graph_qs = graph_qs.replace(value, "null");
//            }else{
//                Log.w(TAG, "Unrecognized value: " + value);
//            }
//            //replace Equality operators
//            equality = mGoogleEqualityOperator.getOperator(google_qs);
//            if(equality != null){
//                graph_qs = graph_qs.replace(equality, mQueryEqualityMap.get(equality));
//            }
//
//            Log.d(TAG, "mimeType converted string: " + graph_qs);
//
//            return graph_qs;
//        }
//    }
//
//
//    class ConvertName implements IConvert{
//        /*
//        Query term: name
//     */
//        public String convert(final String google_qs){
//            StringBuffer graph_qs = new StringBuffer("");
//            String operator, value;
//            int i;
//
//
//            Log.d(TAG, "Converting name...");
//            //Exit if query term 'name' doesn't present
//            if(!google_qs.contains(GoogleQueryTerm.NAME)) {
//                Log.d(TAG, "Query term 'name' not found:" + google_qs);
//                return null;
//            }
//
//            Log.d(TAG, "Given string: " + google_qs);
//
//            //get operator and value
//            operator = mGoogleFunctionOperator.getOperator(google_qs);
//            i = google_qs.lastIndexOf(operator);    //also fine if IndexOf() is used
//            i += operator.length() + " ".length();
//            value = google_qs.substring(i);
//
//            graph_qs = graph_qs.append(mQueryFunctionMap.get(operator));
//            i = graph_qs.indexOf("(");
//            i++;
//            graph_qs = graph_qs.insert(i, "name");
//            i += "name".length();
//            graph_qs = graph_qs.insert(i, ",");
//            i+=",".length();
//            graph_qs = graph_qs.insert(i, value);
//
//            Log.d(TAG, "converted graph_qs:" + graph_qs);
//
//            return graph_qs.toString();
//        }
//    }


    /*
        Separate the whole query string to query strings.
        Good reference:
        example 6 in https://www.geeksforgeeks.org/split-string-java-examples/
        https://stackoverflow.com/questions/3481828/how-to-split-a-string-in-java
    */
    private String[] split(String qs){
        String regex = "";
        /*
            Good reference for converting string array to a string
            https://stackoverflow.com/questions/5283444/convert-array-of-strings-into-a-string-in-java/5283753
        */
        regex = TextUtils.join("|", mConditionalOperator.getCandidates());

        Log.d(TAG, "Regex for split:" + regex);

        return qs.split(regex);
    }

    private List<String> getConditionOperators(String qs){
        String s, op;
        List<String> conditions = new ArrayList<>();
        int i;

        s = qs;
        do{
            i = mConditionalOperator.getIndex(s);
            if (i != -1) {
                op = mConditionalOperator.getOperator(s.substring(i));
                Log.d(TAG, "Sub string:" + op);
                conditions.add(op);
                i += op.length();
                s = s.substring(i);
            }
        }while(i != -1);

        return conditions;
    }

    public String execute(String qs){
        List<String> transcoded = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        String[] separated;
        String s;
        ListIterator<IConvert> conversions;

        Log.d(TAG, "Given gooogle query string:" + qs);

        separated = split(qs); //Split whole query string into substrings.
        conditions = getConditionOperators(qs); //extract condition operators

        Log.d(TAG, "Length of split strings:" + separated.length);
        /**
            convert the split substring by calling all of the added converters. If the conversion is
            not applied, the conversion return null instead of a converted string.
        */
        for(int i = 0 ; i < separated.length; i++){
            conversions = mConvertions.listIterator();
            while(conversions.hasNext()) {
                s = conversions.next().convert(separated[i]);
                if (s != null) {
                    transcoded.add(s);
                }
            }
        }
       /*
        Merge two arrays interleaved.
        Assume:
        1. length of function operations is shorter than the other.
        2. Length of function is the one of the other - 1
        */
        s = null;
        if(transcoded.size() != (conditions.size()+1)){
            Log.w(TAG, "transcode failed! Length of condition op: "
                    + conditions.size() + "length of query string: " + transcoded.size());
            return s;
        }
        Log.d(TAG, "Merge string: " + transcoded.get(0));
        s = transcoded.get(0);
        for (int i = 0; i < conditions.size(); i++) {
            Log.d(TAG, "Substring: " + transcoded.get(i));
            s = s.concat(conditions.get(i));
            s = s.concat(" ");
            s = s.concat(transcoded.get(i+1));
        }

        Log.d(TAG, "Converted string: " + s);
        return s;
    }
}
