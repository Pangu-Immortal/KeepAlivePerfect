package com.fanjun.keeplive;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.fanjun.keeplive.config.ForegroundNotification;
import com.fanjun.keeplive.config.KeepLiveService;
import com.fanjun.keeplive.service.JobHandlerService;
import com.fanjun.keeplive.service.LocalService;
import com.fanjun.keeplive.service.RemoteService;

import java.util.List;

/**
 * 保活工具
 */
public final class KeepLive {
    /**
     * 运行模式
     */
    public static enum RunMode {
        /**
         * 省电模式
         * 省电一些，但保活效果会差一点
         */
        ENERGY,
        /**
         * 流氓模式
         * 相对耗电，但可造就不死之身
         */
        ROGUE
    }

    public static ForegroundNotification foregroundNotification = null;
    public static KeepLiveService keepLiveService = null;
    public static RunMode runMode = null;
    public static boolean useSilenceMusice = true;

    /**
     * 启动保活
     *
     * @param application            your application
     * @param foregroundNotification 前台服务 必须要，安卓8.0后必须有前台通知才能正常启动Service
     * @param keepLiveService        保活业务
     */
    public static void startWork(@NonNull Application application, @NonNull RunMode runMode, @NonNull ForegroundNotification foregroundNotification, @NonNull KeepLiveService keepLiveService) {
        if (isMain(application)) {
            KeepLive.foregroundNotification = foregroundNotification;
            KeepLive.keepLiveService = keepLiveService;
            KeepLive.runMode = runMode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //启动定时器，在定时器中启动本地服务和守护进程
                Intent intent = new Intent(application, JobHandlerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    application.startForegroundService(intent);
                } else {
                    application.startService(intent);
                }
            } else {
                //启动本地服务
                Intent localIntent = new Intent(application, LocalService.class);
                //启动守护进程
                Intent guardIntent = new Intent(application, RemoteService.class);
                application.startService(localIntent);
                application.startService(guardIntent);
            }
        }
    }

    /**
     * 是否启用无声音乐
     * 如不设置，则默认启用
     * @param enable
     */
    public static void useSilenceMusice(boolean enable){
        KeepLive.useSilenceMusice = enable;
    }

    private static boolean isMain(Application application) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager mActivityManager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = mActivityManager.getRunningAppProcesses();
        if (runningAppProcessInfos != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }
            String packageName = application.getPackageName();
            if (processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
