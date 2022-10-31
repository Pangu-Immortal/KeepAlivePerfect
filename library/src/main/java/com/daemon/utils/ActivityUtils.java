package com.daemon.utils;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.daemon.CoreService;
import com.daemon.Daemon;
import com.daemon.DaemonLog;
import com.daemon.IntentJobService;

import java.util.List;

public class ActivityUtils {

    public static final class StartActivityRunnable implements Runnable {
        private final Context mContext;
        private final Intent mIntent;

        public StartActivityRunnable(Context context, Intent intent) {
            this.mContext = context;
            this.mIntent = intent;
        }

        public void run() {
            ActivityUtils.startActivity(this.mContext, this.mIntent);
        }
    }

    public static final class CancelNotificationRunnable implements Runnable {

        /* renamed from: a  reason: collision with root package name */
        public final /* synthetic */ NotificationManagerCompat notificationManagerCompat;

        public CancelNotificationRunnable(NotificationManagerCompat notificationManagerCompat) {
            this.notificationManagerCompat = notificationManagerCompat;
        }

        public void run() {
            this.notificationManagerCompat.cancel(99);
        }
    }

    public static void startActivity(Context context, Intent intent) {
        IntentUtils.startActivitySafe(context, intent);
        if (Build.VERSION.SDK_INT >= 26) {
            bringAppToForeground(context);
            IntentJobService.scheduleService(context, intent, true);
        }
        boolean isAppRunningForeground = isAppRunningForeground(context);
        DaemonLog.d("vivo,isAppRunningForeground=" + isAppRunningForeground);
        if (!isAppRunningForeground) {
            for (int i = 0; i < 10; i++) {
                try {
                    bringAppToForeground(context);
                    Thread.sleep((long) 50);
                    if (isAppRunningForeground(context)) {
                        IntentUtils.startActivitySafe(context, intent);
                        //nk.getInstance().getCallback().moveHomeBack();
                        return;
                    }
                } catch (Exception unused) {
                    return;
                }
            }
            return;
        }
        CoreService.startForLockScreen(context, intent);
    }

    public static void bringAppToForeground(Context context) {
        List<ActivityManager.RunningTaskInfo> runningTasks;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (!(activityManager == null || (runningTasks = activityManager.getRunningTasks(10)) == null)) {
            for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTasks) {
                ComponentName componentName = runningTaskInfo.topActivity;
                if (componentName != null && componentName.getPackageName().equals(context.getPackageName())) {
                    DaemonLog.d("bringAppToForeground,taskInfo.topActivity=" + runningTaskInfo.topActivity + ",baseActivity=" + runningTaskInfo.baseActivity);
                    activityManager.moveTaskToFront(runningTaskInfo.id, 0);
                    return;
                }
            }
        }
    }

    public static void hookJumpActivity(Context context, Intent intent) {
        if (ROMUtils.isOppo()) {
            IntentUtils.startActivitySafe(context, intent);
            IntentJobService.scheduleService(context, intent, true);
        } else if (ROMUtils.isVivo()) {
            IntentJobService.scheduleService(context, intent, true);
            new Thread(new StartActivityRunnable(context, intent)).start();
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                try{
                    IntentUtils.startActivitySafe(context, intent,false);
                }catch (Exception exp){
                    exp.printStackTrace();
                }
                return;
            }
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            NotificationCompat.Builder b2 = Daemon.getConfiguration().getIntentNotificationBuilder(context);
            b2.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);
            NotificationManagerCompat from = NotificationManagerCompat.from(context);
            try {
                from.cancel(99);
                IntentJobService.scheduleService(context, intent, true);
                IntentUtils.startActivitySafe(context, intent);
                from.notify(99, b2.build());
                new Handler(Looper.getMainLooper()).postDelayed(new CancelNotificationRunnable(from), 2000);
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        }
    }

    public static boolean isAppRunningForeground(Context context) {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null || (runningAppProcesses = activityManager.getRunningAppProcesses()) == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (TextUtils.equals(context.getApplicationInfo().processName, runningAppProcessInfo.processName) && runningAppProcessInfo.importance == 100) {
                return true;
            }
        }
        return false;
    }
}