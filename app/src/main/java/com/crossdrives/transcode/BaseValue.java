package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseValue {
    List<String> mValues = new ArrayList<>();

    public void setValues(Collection<? extends String> ops){
        mValues.addAll(ops);
    }

    /*
        get operator presented 1st
     */
    public String getValue(String q){
        int i = -1;
        String operator = null;

        for (String s : mValues){
            i = q.indexOf(s);
            if(q.indexOf(s) != -1) {
                operator = s;
                break;
            }
        }

        return operator;
    }

    /*
        get index of operator presented 1st
     */
    public int getIndex(String q){
        int i = -1;

        for (String s : mValues){
            i = q.indexOf(s);
            if(q.indexOf(s) != -1) {
                break;
            }
        }

        return i;
    }

    public Collection<? extends String> getCandidates(){
        return mValues;
    }
}
