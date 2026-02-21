package com.fanjun.keeplive.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.config.NotificationUtils
import com.fanjun.keeplive.receiver.NotificationClickReceiver

/**
 * 隐藏前台服务通知
 * API < 25 时使用：启动前台通知后延迟 2 秒停止自身，达到隐藏通知的效果
 */
class HideForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showForeground()
        handler.postDelayed({
            stopForeground(true)
            stopSelf()
        }, 2000)
        return START_NOT_STICKY
    }

    // 启动前台通知
    private fun showForeground() {
        val notification = KeepLive.foregroundNotification ?: return
        val intent = Intent(applicationContext, NotificationClickReceiver::class.java).apply {
            action = NotificationClickReceiver.CLICK_NOTIFICATION
        }
        val n = NotificationUtils.createNotification(
            this, notification.title, notification.description, notification.iconRes, intent
        )
        startForeground(13691, n)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
