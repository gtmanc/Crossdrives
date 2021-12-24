package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseQueryTerm {
    final List<String> mQueryTerms = new ArrayList<>();

    public BaseQueryTerm() {
    }

    public void setQueryTerms(Collection<? extends String> terms){
        mQueryTerms.addAll(terms);
    }

    //    public int getIndex(){return index;}
//    public void setIndex(int index){this.index = index;}
    /*
        get query term presented 1st
     */
    public String getTerm(String q){
        int i = -1;
        String term = null;

        for (String s : mQueryTerms){
            i = q.indexOf(s);
            if(q.indexOf(s) != -1) {
                term = s;
                break;
            }
        }

        return term;
    }

    /*
        get index of query term presented 1st
    */
    public int getIndex(String q){
        int i = -1;

        for (String s : mQueryTerms){
            i = q.indexOf(s);
            if(q.indexOf(s) != -1) {
                break;
            }
        }

        return i;
    }
}
