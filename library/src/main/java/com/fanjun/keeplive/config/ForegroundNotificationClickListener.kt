package com.fanjun.keeplive.config

import android.content.Context
import android.content.Intent

/**
 * 前台服务通知点击事件回调接口
 */
fun interface ForegroundNotificationClickListener {
    fun foregroundNotificationClick(context: Context, intent: Intent)
}
