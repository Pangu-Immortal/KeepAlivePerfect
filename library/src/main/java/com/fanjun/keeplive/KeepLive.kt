package com.fanjun.keeplive

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fanjun.keeplive.config.ForegroundNotification
import com.fanjun.keeplive.config.KeepLiveService
import com.fanjun.keeplive.service.JobHandlerService
import com.fanjun.keeplive.service.LocalService
import com.fanjun.keeplive.service.RemoteService

/**
 * 保活工具入口
 * 提供 startWork() 一行代码启动保活，支持 ENERGY（省电）和 ROGUE（激进）两种模式
 */
object KeepLive {

    /**
     * 运行模式枚举
     */
    enum class RunMode {
        /** 省电模式：省电一些，但保活效果会差一点 */
        ENERGY,
        /** 流氓模式：相对耗电，但可造就不死之身 */
        ROGUE
    }

    @JvmField
    var foregroundNotification: ForegroundNotification? = null

    @JvmField
    var keepLiveService: KeepLiveService? = null

    @JvmField
    var runMode: RunMode? = null

    @JvmField
    var useSilenceMusice: Boolean = true

    /**
     * 启动保活
     * @param application 应用实例
     * @param runMode 运行模式
     * @param foregroundNotification 前台服务通知配置（Android 8.0+ 必须）
     * @param keepLiveService 保活业务回调
     */
    @JvmStatic
    fun startWork(
        application: Application,
        runMode: RunMode,
        foregroundNotification: ForegroundNotification,
        keepLiveService: KeepLiveService
    ) {
        if (!isMain(application)) return
        this.foregroundNotification = foregroundNotification
        this.keepLiveService = keepLiveService
        this.runMode = runMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 启动定时器，在定时器中启动本地服务和守护进程
            val intent = Intent(application, JobHandlerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(intent)
            } else {
                application.startService(intent)
            }
        } else {
            // 直接启动本地服务和守护进程
            application.startService(Intent(application, LocalService::class.java))
            application.startService(Intent(application, RemoteService::class.java))
        }
    }

    /**
     * 是否启用无声音乐（默认启用）
     */
    @JvmStatic
    fun useSilenceMusice(enable: Boolean) {
        useSilenceMusice = enable
    }

    // 判断当前进程是否为主进程
    private fun isMain(application: Application): Boolean {
        val pid = android.os.Process.myPid()
        val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses ?: return false
        val processName = processes.firstOrNull { it.pid == pid }?.processName ?: return false
        return processName == application.packageName
    }
}
