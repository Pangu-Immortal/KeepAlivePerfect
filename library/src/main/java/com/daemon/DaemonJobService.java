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
import java.util.concurrent.TimeUnit;

@TargetApi(21)
public class DaemonJobService extends JobService {
    public static final int JOB_ID = 1000;
    public static final long JOB_INTERVAL = TimeUnit.SECONDS.toMillis(8);

    public static void scheduleService(Context context) {
        DaemonLog.d("DaemonJobService scheduleService");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder persisted = new JobInfo.Builder(JOB_ID, new ComponentName(context, DaemonJobService.class)).setPersisted(true);
            if (Build.VERSION.SDK_INT < 24) {
                persisted.setPeriodic(JOB_INTERVAL);
            } else {
                persisted.setMinimumLatency(JOB_INTERVAL);
            }
            if (jobScheduler != null) {
                try {
                    jobScheduler.schedule(persisted.build());
                } catch (Throwable unused) {
                }
            }
        }
    }

    public static void stopScheduleService(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(1000);
        }
    }

    public void onCreate() {
        super.onCreate();
        DaemonLog.d("DaemonJobService onCreate");
    }

    public void onDestroy() {
        super.onDestroy();
        DaemonLog.d("DaemonJobService onDestroy");
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        DaemonLog.d("DaemonJobService onStartCommand");
        return START_STICKY;
    }

    public boolean onStartJob(JobParameters jobParameters) {
        DaemonLog.d("DaemonJobService onStartJob");
        if (Build.VERSION.SDK_INT >= 24) {
            scheduleService(getApplicationContext());
        }
        CoreService.start(getApplicationContext());
        return false;
    }

    public boolean onStopJob(JobParameters jobParameters) {
        DaemonLog.d("DaemonJobService onStopJob");
        return false;
    }
}