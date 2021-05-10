package com.boolbird.keepalive.demo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Service2 extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("Service2 started");
    }
}
