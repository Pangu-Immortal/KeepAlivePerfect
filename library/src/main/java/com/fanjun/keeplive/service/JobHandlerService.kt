package com.fanjun.keeplive.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.config.NotificationUtils
import com.fanjun.keeplive.receiver.NotificationClickReceiver
import com.fanjun.keeplive.utils.ServiceUtils

/**
 * 定时检测服务（API 21+）
 * 通过 JobScheduler 每 30 秒检查 LocalService 和 RemoteService 是否存活，若死亡则重启
 */
@RequiresApi(api = 21)
class JobHandlerService : JobService() {

    private var jobScheduler: JobScheduler? = null
    private val jobId = 100

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServices(this)
        // 调度 JobScheduler 定时任务
        if (Build.VERSION.SDK_INT >= 21) {
            jobScheduler = getSystemService("jobscheduler") as JobScheduler
            jobScheduler?.cancel(jobId)
            val builder = JobInfo.Builder(jobId, ComponentName(packageName, JobHandlerService::class.java.name))
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setMinimumLatency(30000L)
                builder.setOverrideDeadline(30000L)
                builder.setBackoffCriteria(30000L, JobInfo.BACKOFF_POLICY_LINEAR)
            } else {
                builder.setPeriodic(30000L)
            }
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            builder.setPersisted(true)
            jobScheduler?.schedule(builder.build())
        }
        return START_STICKY
    }

    // 启动本地服务和守护进程
    private fun startServices(context: Context) {
        // API 26+ 需要前台通知
        if (Build.VERSION.SDK_INT >= 26) {
            KeepLive.foregroundNotification?.let { notification ->
                val intent = Intent(applicationContext, NotificationClickReceiver::class.java).apply {
                    action = NotificationClickReceiver.CLICK_NOTIFICATION
                }
                val n = NotificationUtils.createNotification(
                    this, notification.title, notification.description, notification.iconRes, intent
                )
                startForeground(13691, n)
            }
        }
        startService(Intent(context, LocalService::class.java))
        startService(Intent(context, RemoteService::class.java))
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        // 检查服务是否存活，若死亡则重启
        if (!ServiceUtils.isServiceRunning(applicationContext, "com.fanjun.keeplive.service.LocalService") ||
            !ServiceUtils.isRunningTaskExist(applicationContext, "$packageName:remote")
        ) {
            startServices(this)
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        if (!ServiceUtils.isServiceRunning(applicationContext, "com.fanjun.keeplive.service.LocalService") ||
            !ServiceUtils.isRunningTaskExist(applicationContext, "$packageName:remote")
        ) {
            startServices(this)
        }
        return false
    }
}
