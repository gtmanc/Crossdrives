package com.crossdrives.ui.listener;

import com.crossdrives.ui.notification.Notification;
import com.google.android.gms.tasks.OnSuccessListener;

public class Result {
    public interface IResultListenerCallback{
        void updateUI();
    }

    <T >OnSuccessListener<T> createSuccessListener(Notification notification, IResultListenerCallback callback){

    }

    createFailueListener(){

    }
}
