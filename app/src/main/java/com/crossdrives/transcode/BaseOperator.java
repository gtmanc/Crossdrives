package com.crossdrives.transcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BaseOperator {

    final List<String> mOPs = new ArrayList<>();

    public BaseOperator() {

    }

    public void setOperators(Collection<? extends String> ops){
        mOPs.addAll(ops);
    }

//    public int getIndex(){return index;}
//    public void setIndex(int index){this.index = index;}
    /*
        get operator presented 1st
     */
    public String getOperator(String q){
        int i = -1;
        String operator = null;

        for (String s : mOPs){
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

        for (String s : mOPs){
            i = q.indexOf(s);
            if(q.indexOf(s) != -1) {
                break;
            }
        }

        return i;
    }

    /*

     */
    public Collection<? extends String> getCandidates(){
        return mOPs;
    }
}
