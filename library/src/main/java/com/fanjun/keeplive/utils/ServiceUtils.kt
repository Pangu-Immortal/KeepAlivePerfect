package com.fanjun.keeplive.utils

import android.app.ActivityManager
import android.content.Context

/**
 * 服务状态检测工具类
 * - isServiceRunning: 检查指定 Service 是否正在运行
 * - isRunningTaskExist: 检查指定进程是否存在
 */
@Suppress("DEPRECATION")
object ServiceUtils {

    /**
     * 检查指定类名的 Service 是否正在运行
     */
    @JvmStatic
    fun isServiceRunning(ctx: Context, className: String): Boolean {
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val servicesList = activityManager.getRunningServices(Int.MAX_VALUE) ?: return false
        return servicesList.any { it.service.className == className }
    }

    /**
     * 检查指定进程名是否存在
     */
    @JvmStatic
    fun isRunningTaskExist(context: Context, processName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processList = am.runningAppProcesses ?: return false
        return processList.any { it.processName == processName }
    }
}
