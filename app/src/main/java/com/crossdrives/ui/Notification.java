package com.crossdrives.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.crossdrives.msgraph.SnippetApp;

public class Notification {

    static android.content.Context mContext = SnippetApp.getAppContext();
    static String mTitle;
    static String mText;
    static int mSmallIcon;
    Category mCategory;
    int mNotificationID;
    NotificationCompat.Builder mBuilder;
    NotificationManagerCompat mNotificationManager;

    public enum Category {
        NOTIFY_UPLOAD (0),
        NOTIFY_DOWNLOAD(1),
        NOTIFY_MESSAGE(2);

        int mChannelID;

        public int getChannelID() {
            return mChannelID;
        }

        Category(int i) {
            mChannelID = i;
        }
    }

    public Notification(Category category, int smallIcon) {
        mCategory = category;
        this.mSmallIcon = smallIcon;
    }


    public Notification setContentTitle(String title){
       mTitle = title;
       return this;
    }

    public Notification setContentText(String content){
        mText = content;
        return this;
    }

    public Notification build()
    {
        String title, text, CHID;

        title = "";
        if(mTitle != null){
            title = mTitle;
        }

        text = "";
        if(mText != null){
            text = mText;
        }

        CHID = Integer.toString(mCategory.getChannelID());
        createEntry(CHID, mSmallIcon, title, text);

        //It's safe to call this repeatedly because creating an existing notification channel performs no operation.
        createNotificationChannel(CHID);

        return this;
    }

    public void updateProgress(int current, int max){
        mBuilder.setProgress(max, current, false);
        mNotificationManager.notify(mNotificationID, mBuilder.build());
    }

    public void removeProgressBar(){
        mBuilder.setProgress(0, 0, false);
        mNotificationManager.notify(mNotificationID, mBuilder.build());
    }

    public void updateContentText(String contentText){
        mBuilder.setContentText(contentText);
        mNotificationManager.notify(mNotificationID, mBuilder.build());
    }

    public void updateContentTitle(String contentTitle){
        mBuilder.setContentTitle(contentTitle);
        mNotificationManager.notify(mNotificationID, mBuilder.build());
    }

    Notification createEntry(String chID, int smallIcon, String title, String text){
        // Create an explicit intent for an Activity in your app
//        Intent intent = new Intent(mContext, QueryResultActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(SnippetApp.getAppContext(), chID)
                //.setSmallIcon(R.drawable.ic_baseline_cloud_circle_24)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                //.setStyle(new NotificationCompat.BigTextStyle()
                //        .bigText("Much longer text that cannot fit one line..."))
                .setOnlyAlertOnce(true) // notification interupts the user (with sound, vibration, or visual clues) only the first time the notification appears and not for later updates
                .setAutoCancel(true)    // automatically removes the notification when the user taps it.
                // Set the intent that will fire when the user taps the notification
                //.setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);  //only for Android 7.1 and lower.

        mBuilder = builder;

        mNotificationManager = NotificationManagerCompat.from(SnippetApp.getAppContext());
        // notificationId is a unique int for each notification that you must define
        mNotificationID = IDAssigner.getID();
        mNotificationManager.notify(mNotificationID, builder.build());

        return this;
    }

    void createNotificationChannel(String ch) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel 0";//getString(R.string.channel_name);
            String description = "This is channel 0";//(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(ch, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = SnippetApp.getAppContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    static class IDAssigner{
        static int  mID = 0;
        static public int getID(){
           int id = mID;
           mID++;
           return id;
        }
    }

}
