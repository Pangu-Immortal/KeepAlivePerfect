package com.daemon.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

public final class ServiceUtils {

    private ServiceUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> servicesList = activityManager
                .getRunningServices(Integer.MAX_VALUE);
        if (servicesList != null) {
            for (ActivityManager.RunningServiceInfo si : servicesList) {
                if (className.equals(si.service.getClassName())) {
                    isRunning = true;
                }
            }
        }
        return isRunning;
    }

    public static boolean isRunningTaskExist(Context context, String processName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processList = am.getRunningAppProcesses();
        if (processList != null) {
            for (ActivityManager.RunningAppProcessInfo info : processList) {
                if (info.processName.equals(processName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMainProcess(@NonNull Application application) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = manager.getRunningAppProcesses();
        if (runningAppProcessInfos != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : manager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }
            return processName.equals(application.getPackageName());
        }
        return false;
    }
}
