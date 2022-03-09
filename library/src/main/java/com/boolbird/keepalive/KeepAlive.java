package com.boolbird.keepalive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Doc说明 (此类核心功能):
 * +---------------------------+
 * | @author qihao             |
 * | @date on 2021/5/10 15:29 |
 * +---------------------------+
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐│
 *  ││Esc│!1 │@2 │#3 │$4 │%5 │^6 │&7 │*8 │(9 │)0 │_- │+= │|\ │`~ ││
 *  │├───┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴───┤│
 *  ││ Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{[ │}] │ BS  ││
 *  │├─────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤│
 *  ││ Ctrl │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  ││
 *  │├──────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────┬───┤│
 *  ││ Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│Shift │Fn ││
 *  │└─────┬──┴┬──┴──┬┴───┴───┴───┴───┴───┴──┬┴───┴┬──┴┬─────┴───┘│
 *  │      │Fn │ Alt │         Space         │ Alt │Win│   qihao  │
 *  │      └───┴─────┴───────────────────────┴─────┴───┘          │
 *  └─────────────────────────────────────────────────────────────┘
 *
 */
public class KeepAlive {

    private static final String TAG = "KeepAlive";

    KeepAliveConfigs mConfigurations;

    static KeepAlive client;

    private KeepAlive(KeepAliveConfigs configurations) {
        this.mConfigurations = configurations;
    }

    public static void init(Context base, KeepAliveConfigs configurations) {
        Reflection.unseal(base);
        client = new KeepAlive(configurations);
        try {
            client.initDaemon(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    private static final String DAEMON_PERMITTING_SP_FILENAME = "d_permit";
    private static final String DAEMON_PERMITTING_SP_KEY = "permitted";

    private void initDaemon(Context base) {
        if (mConfigurations == null) {
            return;
        }

        String processName = getProcessName();
        String packageName = base.getPackageName();

        if (processName == null) {
            Log.e(TAG, "process name is empty");
        } else if (processName.startsWith(mConfigurations.PERSISTENT_CONFIG.processName)) {
            if (mConfigurations.limitReboot) {
                checkServiceProcessContinuousBootOverTimes(base, mConfigurations);
            } else {
                setDaemonPermitting(base, true);
            }
            if (isDaemonPermitting(base)) {
                IKeepAliveProcess.Fetcher.fetchStrategy().onPersistentCreate(base, mConfigurations);
            }
        } else if (processName.startsWith(mConfigurations.DAEMON_ASSISTANT_CONFIG.processName)) {
            // checkDaemonProcessContinuousBootOverTimes(base, mConfigurations);
            if (isDaemonPermitting(base)) {
                IKeepAliveProcess.Fetcher.fetchStrategy().onDaemonAssistantCreate(base, mConfigurations);
            }
        } else if (processName.startsWith(packageName)) {
            // checkMainProcessContinuousBootOverTimes(base, mConfigurations);
            if (isDaemonPermitting(base)) {
                IKeepAliveProcess.Fetcher.fetchStrategy().onInit(base, mConfigurations);
            }
            if (KeepAliveConfigs.bootReceivedListener != null) {
                KeepAliveConfigs.bootReceivedListener.onReceive(base, new Intent(Intent.ACTION_RUN));
            }
            KeepAlive.launchAlarm(base);
        }
    }

    static void launchAlarm(Context context) {
        // alarm唤醒
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        long INTERVAL_WAKEUP_MS = 60 * 1000; // 60 seconds
        long triggerAtTime = SystemClock.elapsedRealtime() + INTERVAL_WAKEUP_MS;
        Intent i = new Intent(context, AutoBootReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        } else {
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        }
    }

    static String getProcessName() {
        BufferedReader mBufferedReader = null;
        try {
            File file = new File("/proc/self/cmdline");
            mBufferedReader = new BufferedReader(new FileReader(file));
            return mBufferedReader.readLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (mBufferedReader != null) {
                try {
                    mBufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isDaemonPermitting(Context context) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        return sp.getBoolean(DAEMON_PERMITTING_SP_KEY, true);
    }

    protected boolean setDaemonPermitting(Context context, boolean isPermitting) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(DAEMON_PERMITTING_SP_KEY, isPermitting);
        return editor.commit();
    }

    private static boolean checkMainProcessContinuousBootOverTimes(Context context, KeepAliveConfigs configurations) {
        return checkProcessContinuousBootOverTimes(context, configurations, "main_process_boot_times", "main_process_boot_time");
    }

    private static boolean checkServiceProcessContinuousBootOverTimes(Context context, KeepAliveConfigs configurations) {
        return checkProcessContinuousBootOverTimes(context, configurations, "service_process_boot_times", "service_process_boot_time");
    }

    private static boolean checkDaemonProcessContinuousBootOverTimes(Context context, KeepAliveConfigs configurations) {
        return checkProcessContinuousBootOverTimes(context, configurations, "daemon_process_boot_times", "daemon_process_boot_time");
    }

    private static boolean checkProcessContinuousBootOverTimes(Context context, KeepAliveConfigs configurations, String timesKey, String rebootTimeKey) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        int times = sp.getInt(timesKey, 0);
        long lastBootTime = sp.getLong(rebootTimeKey, 0);
        long now = System.currentTimeMillis();
        Log.e(TAG, "checkCC " + times + " lastTime=" + lastBootTime + " diff=" + (now - lastBootTime) + " max=" + configurations.rebootIntervalMs + " times=" + configurations.rebootMaxTimes);
        if (lastBootTime > 0) {
            if (now - lastBootTime < configurations.rebootIntervalMs) {
                if (times >= configurations.rebootMaxTimes) {
                    markProcessBoot(context, 1, timesKey, rebootTimeKey);
                    if (client != null) {
                        client.setDaemonPermitting(context, false);
                    }
                    Log.e(TAG, "daemon is not permitted!");
                    return true;
                } else {
                    times++;
                    markProcessBoot(context, times, timesKey, rebootTimeKey);
                }
            } else {
                markProcessBoot(context, 1, timesKey, rebootTimeKey);
            }
        } else {
            markProcessBoot(context, 1, timesKey, rebootTimeKey);
        }
        if (client != null) {
            client.setDaemonPermitting(context, true);
        }
        return false;
    }

    private static void markProcessBoot(Context context, int times, String timesKey, String rebootTimeKey) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putInt(timesKey, times);
        editor.putLong(rebootTimeKey, System.currentTimeMillis());
        editor.apply();
    }
}
