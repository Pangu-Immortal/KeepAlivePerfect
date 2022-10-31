package com.daemonLibrary.demo

import android.annotation.TargetApi
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daemon.Daemon

class DaemonConfigurationImplement(val context: Context) : Daemon.DaemonConfiguration {
    @TargetApi(26)
    fun getIntentChannelId(context: Context): String {
        val from: NotificationManagerCompat = NotificationManagerCompat.from(context)
        val notificationChannel = NotificationChannel(
            "com.daemon.lockscreen.brandnew",
            "com.daemon.lockscreen.brandnew",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.enableLights(false)
        notificationChannel.setShowBadge(false)
        notificationChannel.enableVibration(false)
        notificationChannel.setSound(null, null)
        from.createNotificationChannel(notificationChannel)
        return notificationChannel.getId()
    }

    override fun getIntentNotificationBuilder(context: Context): NotificationCompat.Builder? {
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            NotificationCompat.Builder(
                context,
                getIntentChannelId(context)!!
            )
        } else {
            NotificationCompat.Builder(context)
        }
        builder.setContentTitle("手机优化中")
        builder.setContentText("正在优化您的手机")
        builder.setSmallIcon(R.drawable.ic_homee)
        builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_homee))
        builder.setAutoCancel(true)
        builder.setDefaults(4)
        builder.priority = -1
        return builder
    }

    override fun getForegroundNotification(): Notification? {
        return null;
        //return ForegroundNotifyUtils.getForegroundNotification(context, "", "Service is running")
    }

    override fun isMusicPlayEnabled(): Boolean {
        return true
    }

    override fun isOnePixelActivityEnabled(): Boolean {
        return true
    }

}

class DaemonCallbackImplement : Daemon.DaemonCallback {
    override fun onStart() {
        Log.d("DEOM", "onStart")
    }

    override fun onStop() {
        Log.d("DEOM", "onStop")
    }
}

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Daemon.isMainProcess(this)) {
            var flt = IntentFilter()
            flt.addAction(Intent.ACTION_PACKAGE_REMOVED)
            flt.addAction(Intent.ACTION_PACKAGE_ADDED)
            flt.addAction(Intent.ACTION_PACKAGE_CHANGED)
            flt.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            flt.addDataScheme("package")
            registerReceiver(SystemBroadcastReceiver(), flt)
            //DaemonLog.d("register Package notify")
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        //DaemonLog.d("Application onCrearte")
        Daemon.startWork(this, DaemonConfigurationImplement(this), DaemonCallbackImplement())
    }
}