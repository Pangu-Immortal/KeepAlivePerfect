package com.fanjun.keeplive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fanjun.keeplive.KeepLive

/**
 * 前台通知点击事件接收器
 * 收到 CLICK_NOTIFICATION 广播后回调用户注册的点击监听
 */
class NotificationClickReceiver : BroadcastReceiver() {

    companion object {
        const val CLICK_NOTIFICATION = "CLICK_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CLICK_NOTIFICATION) {
            KeepLive.foregroundNotification
                ?.foregroundNotificationClickListener
                ?.foregroundNotificationClick(context, intent)
        }
    }
}
