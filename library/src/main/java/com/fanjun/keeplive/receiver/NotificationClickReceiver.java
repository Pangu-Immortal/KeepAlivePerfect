package com.fanjun.keeplive.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fanjun.keeplive.KeepLive;

public final class NotificationClickReceiver extends BroadcastReceiver {
    public final static String CLICK_NOTIFICATION = "CLICK_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(NotificationClickReceiver.CLICK_NOTIFICATION)) {
            if (KeepLive.foregroundNotification != null) {
                if (KeepLive.foregroundNotification.getForegroundNotificationClickListener() != null) {
                    KeepLive.foregroundNotification.getForegroundNotificationClickListener().foregroundNotificationClick(context, intent);
                }
            }
        }
    }
}
