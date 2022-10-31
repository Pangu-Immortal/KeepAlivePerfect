package com.daemon;

public class DaemonService extends BaseService {
    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(){
            @Override
            public void run() {
                Daemon.getCallback().onStart();
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Daemon.getCallback().onStop();
    }
}
