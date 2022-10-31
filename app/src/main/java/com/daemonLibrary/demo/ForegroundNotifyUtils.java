package com.daemonLibrary.demo;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

public class ForegroundNotifyUtils {
    private static final String CHANNEL_ID = "BCD41FB3-2EE7-4781-8917-7B8D8DB355DC";

    public static Notification getForegroundNotification(Context context, String title, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel Channel = new NotificationChannel(CHANNEL_ID, "可关闭通知", NotificationManager.IMPORTANCE_DEFAULT);
            Channel.enableLights(true);
            Channel.setLightColor(Color.GREEN);
            Channel.setShowBadge(true);
            Channel.setDescription("");
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.enableVibration(false);
            Channel.setSound(null, null);
            manager.createNotificationChannel(Channel);

            Notification notification = new Notification.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground))
                    .build();
            return notification;
        } else {
            Notification notification = new Notification.Builder(context)
                    .setContentTitle(title)//设置标题
                    .setContentText(text)//设置内容
                    .setWhen(System.currentTimeMillis())//设置创建时间
                    .build();
            return notification;
        }
    }
}
