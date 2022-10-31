package com.daemon;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.daemon.process.Configs;
import com.daemon.receiver.ScreenReceiver;
import com.daemon.utils.ActivityUtils;
import com.daemon.utils.ServiceUtils;

public  class Daemon {

    public interface DaemonCallback {
        void onStart();
        void onStop();
    }

    public  interface DaemonConfiguration{
        NotificationCompat.Builder getIntentNotificationBuilder(Context context);
        Notification getForegroundNotification();
        Boolean isMusicPlayEnabled();
        Boolean isOnePixelActivityEnabled();
    }

    private  static DaemonConfiguration configuration =null;
    private  static DaemonCallback callback=null;
    private  static Application application=null;

    public static DaemonCallback getCallback(){return callback;}

    public static Application getApplication() {
        return application;
    }

    public static DaemonConfiguration getConfiguration(){return configuration;}

    public static boolean isMainProcess(Application app){
        return ServiceUtils.isMainProcess(app);
    }

    public static void startActivity(Context context,Intent intent){
        if (ActivityUtils.isAppRunningForeground(context)){
            context.startActivity(intent);
            return;
        }
        ActivityUtils.hookJumpActivity(context,intent);
    }

    public  static void startWork(Application application, DaemonConfiguration configuration, DaemonCallback callback){
        if (Daemon.application != null){
            return;
        }
        Daemon.callback= callback;
        Daemon.configuration = configuration;
        Daemon.application = application;
        ScreenReceiver.register(application);
        com.daemon.process.Daemon.init(application.getBaseContext(),new com.daemon.process.Configs(new Configs.Config(
                "android.process.media","com.daemon.DaemonService","",""
        ),new Configs.Config(
                application.getPackageName()+":service","com.daemon.CoreService","",""
        )));
        CoreService.start(application);
        DaemonJobService.scheduleService(application);
    }
}
