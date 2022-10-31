package com.daemon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public abstract class BaseService extends Service {
    class LoggerRunnable implements Runnable {
        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DaemonLog.d(getName()+" thread is running");
            }
        }
    }
    public String getName() {
        return getClass().getSimpleName();
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }


    private Notification defaultNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel Channel = new NotificationChannel("48098DDD-C1F8-4CA0-9BE9-A1466CF692B2", "可关闭通知", NotificationManager.IMPORTANCE_DEFAULT);
            Channel.enableLights(true);
            Channel.setLightColor(Color.GREEN);
            Channel.setShowBadge(true);
            Channel.setDescription("");
            Channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            Channel.enableVibration(false);
            Channel.setSound(null, null);
            manager.createNotificationChannel(Channel);

            Notification notification = new Notification.Builder(this, "48098DDD-C1F8-4CA0-9BE9-A1466CF692B2")
                    .setContentTitle("App")
                    .setContentText("App is running")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.drawable.ic_stat_name))
                    .build();
            return notification;
        } else {
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("App")//设置标题
                    .setContentText("App is running")//设置内容
                    .setWhen(System.currentTimeMillis())//设置创建时间
                    .build();
            return notification;
        }
    }

    private Notification getNotification(){
        Notification notification = Daemon.getConfiguration().getForegroundNotification();
        if(notification != null){
            return notification;
        }
        return defaultNotification();
    }

    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForeground(1,getNotification());
        }
        DaemonLog.d(getName() + " oncreate");
        //Thread thread = new Thread(new LoggerRunnable());
        //thread.start();
    }

    public void onDestroy() {
        super.onDestroy();
        DaemonLog.d(getName() + " onDestroy");
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        DaemonLog.d(getName() + " onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForeground(1,getNotification());
        }
        return START_STICKY;
    }

    public void onTaskRemoved(Intent intent) {
        DaemonLog.d("onTaskRemoved");
    }
}