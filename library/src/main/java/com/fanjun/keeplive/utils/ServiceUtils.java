package com.fanjun.keeplive.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.Iterator;
import java.util.List;

public class ServiceUtils {
    public static boolean isServiceRunning(Context ctx, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) ctx
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> servicesList = activityManager
                .getRunningServices(Integer.MAX_VALUE);
        if (servicesList != null) {
            Iterator<ActivityManager.RunningServiceInfo> l = servicesList.iterator();
            while (l.hasNext()) {
                ActivityManager.RunningServiceInfo si = l.next();
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
        if (processList != null){
            for (ActivityManager.RunningAppProcessInfo info : processList) {
                if (info.processName.equals(processName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
