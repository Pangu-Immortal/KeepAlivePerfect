package com.daemon;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.daemon.utils.IntentUtils;

@TargetApi(21)
public class IntentJobService extends JobService {

    /* renamed from: a  reason: collision with root package name */
    public static final String IS_ACTIVITY = "is_activity";

    /* renamed from: b  reason: collision with root package name */
    public static final int JOB_ID = 1001;

    public static void scheduleService(Context context, Intent intent, boolean z) {
        DaemonLog.d("IntentJobService scheduleService");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder persisted = new JobInfo.Builder(JOB_ID, new ComponentName(context, IntentJobService.class)).setPersisted(false);
            if (Build.VERSION.SDK_INT >= 28) {
                persisted.setImportantWhileForeground(true);
            }
            persisted.setRequiresDeviceIdle(false);
            persisted.setOverrideDeadline(3000);
            if (Build.VERSION.SDK_INT >= 26 && intent != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("android.intent.extra.INTENT", intent);
                bundle.putBoolean(IS_ACTIVITY, z);
                persisted.setTransientExtras(bundle);
            }
            if (jobScheduler != null) {
                try {
                    jobScheduler.schedule(persisted.build());
                } catch (Throwable th) {
                    DaemonLog.d("IntentJobService schedule error", th);
                }
            }
        }
    }

    public static void stopScheduleService(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(JOB_ID);
        }
    }

    public void onCreate() {
        super.onCreate();
        DaemonLog.d("IntentJobService onCreate");
    }

    public void onDestroy() {
        super.onDestroy();
        DaemonLog.d("IntentJobService onDestroy");
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        DaemonLog.d("IntentJobService onStartCommand");
        return START_STICKY;
    }

    public boolean onStartJob(JobParameters jobParameters) {
        DaemonLog.d("IntentJobService onStartJob");
        if (Build.VERSION.SDK_INT >= 28) {
            Bundle transientExtras = jobParameters.getTransientExtras();
            Intent intent = (Intent) transientExtras.getParcelable("android.intent.extra.INTENT");
            DaemonLog.d("IntentJobService onStartJob intent=" + intent);
            if (intent != null) {
                if (transientExtras.getBoolean(IS_ACTIVITY)) {
                    IntentUtils.startActivitySafe((Context) this, intent, false);
                } else {
                    try {
                        startService(intent);
                    } catch (Exception e) {
                        DaemonLog.d("IntentJobService start service error", e);
                    }
                }
            }
        }
        return false;
    }

    public boolean onStopJob(JobParameters jobParameters) {
        DaemonLog.d("IntentJobService onStopJob");
        return false;
    }
}