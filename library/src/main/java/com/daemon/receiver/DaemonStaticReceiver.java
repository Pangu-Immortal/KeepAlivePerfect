package com.daemon.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DaemonStaticReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        //DaemonLog.d("DaemonStaticReceiver onReceive,action=" + action);
    }
}