package com.crossdrives.cdfs.util;

import android.util.Log;

public class Wait {
    final String TAG = "CD.Wait";
    int expected;
    int delay;  //ms
    int timeOut; //sec
    int count = 0;

    public Wait(int expected){
        this.expected = expected;
        this.delay = 1000;
        this.timeOut = 60;
    }
    public Wait(int expected, int delay, int timeOut) {
        this.expected = expected;
        this.delay = delay;
        this.timeOut = timeOut;
    }

    public boolean isCompleted(int succeed, int failed){
        boolean result = true;

        Log.d(TAG, "Wait... expected: " + expected + ". succeed: " + succeed + " failed: " + failed);
        if((succeed + failed) < expected){
            result = false;
        }else if((count*1) > timeOut){
            result = false;
        }
        count++;

        Delay.delay(delay);
        return result;
    }
}
