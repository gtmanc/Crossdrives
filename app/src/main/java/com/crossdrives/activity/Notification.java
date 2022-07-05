package com.crossdrives.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.R;

public class Notification {
    final static String NOTIFICATION_CH_ID = "0" ;
    static int mNotificationID = 0;
    static android.content.Context mContext = SnippetApp.getAppContext();
    static String mTitle;
    static String mContent;
    static int mSmallIcon;

    public Notification(int smallIcon) {
        this.mSmallIcon = smallIcon;
    }

    Notification setTitle(String title){
        this.mTitle = title;
        return this;
    }

    Notification setContent(String content){
        this.mContent = content;
        return this;
    }

    static Notification build(int smallIcon){
        return new Notification(smallIcon);
    }

    Notification createEntry(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(SnippetApp.getAppContext(), IDGenerator.getID())
                //.setSmallIcon(R.drawable.ic_baseline_cloud_circle_24)
                .setSmallIcon(mSmallIcon)
                .setContentTitle(mContext.getString(R.string.notification_title_default))
                .setContentText(mContext.getString(R.string.notification_content_default))
                //.setStyle(new NotificationCompat.BigTextStyle()
                //        .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);  //only for Android 7.1 and lower.

        if(mTitle != null){
            builder.setContentTitle(mTitle);
        }
        if(mContent != null){
            builder.setContentText(mContent);
        }

        createNotificationChannel();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(SnippetApp.getAppContext());
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());

        return this;
    }

    static void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel 0";//getString(R.string.channel_name);
            String description = "This is channel 0";//(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CH_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = SnippetApp.getAppContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    static class IDGenerator{
         static public String getID(){
             int id = mNotificationID;
             mNotificationID++;
             return Integer.toString(id);
        }
    }

}
