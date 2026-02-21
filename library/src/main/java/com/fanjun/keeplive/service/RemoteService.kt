package com.fanjun.keeplive.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteException
import com.fanjun.keeplive.config.NotificationUtils
import com.fanjun.keeplive.receiver.NotificationClickReceiver
import com.fanjun.keeplive.utils.ServiceUtils

/**
 * 守护服务（:remote 进程）
 * 运行在独立进程，与 LocalService 通过 AIDL 互相绑定，实现双进程守护
 */
@Suppress("DEPRECATION")
class RemoteService : Service() {

    private val binder = MyBinder()
    private var isBoundLocalService = false

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            isBoundLocalService = bindService(
                Intent(this, LocalService::class.java), connection, Context.BIND_ABOVE_CLIENT
            )
        } catch (_: Exception) {
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isBoundLocalService) unbindService(connection)
        } catch (_: Exception) {
        }
    }

    // AIDL Binder 实现：收到 wakeUp 调用后启动前台通知（API < 25）
    private inner class MyBinder : GuardAidl.Stub() {
        @Throws(RemoteException::class)
        override fun wakeUp(title: String?, discription: String?, iconRes: Int) {
            if (Build.VERSION.SDK_INT < 25) {
                val intent = Intent(applicationContext, NotificationClickReceiver::class.java).apply {
                    action = NotificationClickReceiver.CLICK_NOTIFICATION
                }
                val notification = NotificationUtils.createNotification(
                    this@RemoteService, title ?: "", discription ?: "", iconRes, intent
                )
                startForeground(13691, notification)
            }
        }
    }

    // 与 LocalService 的连接
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            // LocalService 断开后重新启动并绑定
            if (ServiceUtils.isRunningTaskExist(applicationContext, "$packageName:remote")) {
                startService(Intent(this@RemoteService, LocalService::class.java))
                isBoundLocalService = bindService(
                    Intent(this@RemoteService, LocalService::class.java), this, Context.BIND_ABOVE_CLIENT
                )
            }
            // 同步屏幕状态
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                sendBroadcast(Intent("_ACTION_SCREEN_ON"))
            } else {
                sendBroadcast(Intent("_ACTION_SCREEN_OFF"))
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // 连接成功，无需额外逻辑
        }
    }
}
