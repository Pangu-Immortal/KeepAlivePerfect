package com.fanjun.keeplive.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.fanjun.keeplive.activity.OnePixelActivity

/**
 * 屏幕开关广播接收器
 * 屏幕关闭时延迟 1 秒启动一像素 Activity，屏幕点亮时通知停止无声音乐
 */
class OnepxReceiver : BroadcastReceiver() {

    private val handler = Handler(Looper.getMainLooper())
    private var screenOn = true

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> { // 屏幕关闭，延迟启动一像素 Activity
                screenOn = false
                handler.postDelayed({
                    if (!screenOn) {
                        val intent2 = Intent(context, OnePixelActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.FLAG_IMMUTABLE
                        } else {
                            0
                        }
                        val pendingIntent = PendingIntent.getActivity(context, 0, intent2, flag)
                        try {
                            pendingIntent.send()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, 1000)
                // 通知屏幕已关闭，开始播放无声音乐
                context.sendBroadcast(Intent("_ACTION_SCREEN_OFF"))
            }
            Intent.ACTION_SCREEN_ON -> { // 屏幕点亮，结束一像素 Activity
                screenOn = true
                // 通知屏幕已点亮，停止播放无声音乐
                context.sendBroadcast(Intent("_ACTION_SCREEN_ON"))
            }
        }
    }
}
