package com.crossdrives.transcode;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GraphTranscoder extends BaseTranscoder{
    static private final String TAG = "CD.GraphTranscoder";
    List<IConvert> mGraphConvertions = new ArrayList<>();
    static GoogleQueryValues mGoogleQueryValues;
    static GoogleEqualityOperator mGoogleEqualityOperator;
    static GoogleFunctionOperator mGoogleFunctionOperator;
    /*
        The maps are used to convert query term, operator and value
     */
    static HashMap<String, String > mQueryFunctionMap = new HashMap<>();
    static HashMap<String, String > mQueryValueMap = new HashMap<>();
    static HashMap<String, String > mQueryEqualityReverseMap = new HashMap<>();
    static HashMap<String, String > mQueryEqualityMap = new HashMap<>();
    static List<IConvert> mConvertions = new ArrayList<>();

    /**
     * Operator maps
     */
    static{
        mQueryFunctionMap.put(GoogleFunctionOperator.CONTAINS, "contains()");
    }

    static{
        mQueryEqualityReverseMap.put(GoogleEqualityOperator.EQUAL,"ne");
        mQueryEqualityReverseMap.put(GoogleEqualityOperator.NOT_EQUAL,"eq");
    }

    static{
        mQueryEqualityMap.put(GoogleEqualityOperator.EQUAL,"eq");
        mQueryEqualityMap.put(GoogleEqualityOperator.NOT_EQUAL,"ne");
    }

    static{
        mQueryValueMap.put(GoogleQueryValues.FOLDER,"folder");
    }

    static{
        mConvertions.add(new ConvertMimeType());
        mConvertions.add(new ConvertName());
    }
    public GraphTranscoder() {
        super(mConvertions);
        mGoogleQueryValues = new GoogleQueryValues();
        mGoogleEqualityOperator = new GoogleEqualityOperator();
        mGoogleFunctionOperator = new GoogleFunctionOperator();
    }

    /*
        Query term: mimeType
        e.g. To query item which is a folder
        Google: mimeType = 'application/vnd.google-apps.folder'
        Graph: folder != null
    */
    static class ConvertMimeType implements IConvert{

        public String convert(final String google_qs) {
            String graph_qs = new String(google_qs.toString());
            String equality, value;

            Log.d(TAG, "Converting mimeType. Given string: " + google_qs);
            //Exit if query term 'mineType' doesn't present
            if(!google_qs.contains(GoogleQueryTerm.MIMETYPE)) {
                Log.d(TAG, "Query term mimetype not found:" + google_qs);
                return null;
            }

            value = mGoogleQueryValues.getValue(google_qs);
            //Log.d(TAG, "Query value folder presents?");
            if(value != null){
                Log.d(TAG, "Query value found: " + value);
                //replace "query term"
                graph_qs = graph_qs.replace(GoogleQueryTerm.MIMETYPE, mQueryValueMap.get(value));
                //replace "query value"
                graph_qs = graph_qs.replace(value, "null");
            }else{
                Log.w(TAG, "Unrecognized value: " + value);
            }
            //replace Equality operators
            equality = mGoogleEqualityOperator.getOperator(google_qs);
            if(equality != null){
                graph_qs = graph_qs.replace(equality, mQueryEqualityReverseMap.get(equality));
            }

            Log.d(TAG, "mimeType converted string: " + graph_qs);

            return graph_qs;
        }
    }


    static class ConvertName implements IConvert{
        /*
        Query term: name
     */
        public String convert(final String google_qs){
            StringBuffer graph_qs = new StringBuffer("");
            String operator, value;
            int i;


            Log.d(TAG, "Converting name. Given string: " + google_qs);
            //Exit if query term 'name' doesn't present
            if(!google_qs.contains(GoogleQueryTerm.NAME)) {
                Log.d(TAG, "Query term 'name' not found:" + google_qs);
                return null;
            }

            /*
            * Two types of operator are processed: Functional and Equality
            * The business logic differs from each other.
            */
            operator = mGoogleFunctionOperator.getOperator(google_qs);
            if(operator != null) {
                Log.d(TAG, "Functional operator: " + operator);
                i = google_qs.lastIndexOf(operator);    //also fine if IndexOf() is used
                i += operator.length() + " ".length();
                value = google_qs.substring(i);

                graph_qs = graph_qs.append(mQueryFunctionMap.get(operator));
                i = graph_qs.indexOf("(");
                i++;
                graph_qs = graph_qs.insert(i, "name");
                i += "name".length();
                graph_qs = graph_qs.insert(i, ",");
                i += ",".length();
                graph_qs = graph_qs.insert(i, value);
            }
            operator = mGoogleEqualityOperator.getOperator(google_qs);
            if(operator != null){
                String s = new String(google_qs);
                Log.d(TAG, "Equality operator: " + operator);
                s = s.replace(operator, mQueryEqualityMap.get(operator));
                graph_qs = graph_qs.append(s);
            }

            Log.d(TAG, "converted graph_qs:" + graph_qs);

            return graph_qs.toString();
        }
    }
}
