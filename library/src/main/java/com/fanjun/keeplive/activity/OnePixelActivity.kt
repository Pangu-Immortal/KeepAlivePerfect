package com.fanjun.keeplive.activity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager

/**
 * 一像素保活 Activity
 * 屏幕关闭时拉起 1x1 透明窗口提升进程优先级，屏幕点亮后自动关闭
 */
class OnePixelActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设定一像素的 Activity 窗口
        window.apply {
            setGravity(Gravity.START or Gravity.TOP)
            attributes = attributes.apply {
                x = 0
                y = 0
                height = 1
                width = 1
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkScreenOn()
    }

    // 如果屏幕已点亮则直接关闭自己
    private fun checkScreenOn() {
        try {
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) { // isScreenOn 已废弃，使用 isInteractive
                finish()
            }
        } catch (_: Exception) {
        }
    }
}
