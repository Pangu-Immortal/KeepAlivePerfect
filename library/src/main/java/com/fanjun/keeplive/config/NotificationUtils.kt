package com.fanjun.keeplive.config

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * 通知创建工具类
 * 封装 API 26+ 的 NotificationChannel 创建和低版本兼容
 */
class NotificationUtils private constructor(context: Context) : ContextWrapper(context) {

    private var manager: NotificationManager? = null
    private val id: String = context.packageName
    private val name: String = context.packageName
    private val ctx: Context = context
    private var channel: NotificationChannel? = null

    // 创建通知渠道（API 26+）
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createNotificationChannel() {
        if (channel == null) {
            channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(false)
                enableLights(false)
                vibrationPattern = longArrayOf(0)
                setSound(null, null)
            }
            getManager().createNotificationChannel(channel!!)
        }
    }

    // 获取 NotificationManager 单例
    private fun getManager(): NotificationManager {
        if (manager == null) {
            manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        return manager!!
    }

    // 构建 API 26+ 渠道通知
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getChannelNotification(title: String, content: String, icon: Int, intent: Intent): Notification.Builder {
        val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, getPendingIntentFlag())
        return Notification.Builder(ctx, id)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
    }

    // 根据 API 版本返回 PendingIntent FLAG
    private fun getPendingIntentFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    // 构建 API 25 及以下兼容通知
    fun getNotification25(title: String, content: String, icon: Int, intent: Intent): NotificationCompat.Builder {
        val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, getPendingIntentFlag())
        return NotificationCompat.Builder(ctx, id)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0))
            .setContentIntent(pendingIntent)
    }

    companion object {

        /**
         * 发送通知（随机 ID）
         */
        @JvmStatic
        fun sendNotification(context: Context, title: String, content: String, icon: Int, intent: Intent) {
            val utils = NotificationUtils(context)
            val notification = if (Build.VERSION.SDK_INT >= 26) {
                utils.createNotificationChannel()
                utils.getChannelNotification(title, content, icon, intent).build()
            } else {
                utils.getNotification25(title, content, icon, intent).build()
            }
            utils.getManager().notify(java.util.Random().nextInt(10000), notification)
        }

        /**
         * 创建通知（不发送，由调用方 startForeground 使用）
         */
        @JvmStatic
        fun createNotification(context: Context, title: String, content: String, icon: Int, intent: Intent): Notification {
            val utils = NotificationUtils(context)
            return if (Build.VERSION.SDK_INT >= 26) {
                utils.createNotificationChannel()
                utils.getChannelNotification(title, content, icon, intent).build()
            } else {
                utils.getNotification25(title, content, icon, intent).build()
            }
        }
    }
}
