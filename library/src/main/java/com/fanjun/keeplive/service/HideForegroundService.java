package com.fanjun.keeplive.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.fanjun.keeplive.KeepLive;
import com.fanjun.keeplive.config.NotificationUtils;
import com.fanjun.keeplive.receiver.NotificationClickReceiver;

/**
 * 隐藏前台服务通知
 */
public class HideForegroundService extends Service {
    private android.os.Handler handler;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        if (handler == null){
            handler = new Handler();
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopForeground(true);
                stopSelf();
            }
        }, 2000);
        return START_NOT_STICKY;
    }


    private void startForeground() {
        if (KeepLive.foregroundNotification != null) {
            Intent intent = new Intent(getApplicationContext(), NotificationClickReceiver.class);
            intent.setAction(NotificationClickReceiver.CLICK_NOTIFICATION);
            Notification notification = NotificationUtils.createNotification(this, KeepLive.foregroundNotification.getTitle(), KeepLive.foregroundNotification.getDescription(), KeepLive.foregroundNotification.getIconRes(), intent);
            startForeground(13691, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
