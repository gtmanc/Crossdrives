package com.crossdrives.cdfs.util;

public class Delay
{
    static public void delay(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
