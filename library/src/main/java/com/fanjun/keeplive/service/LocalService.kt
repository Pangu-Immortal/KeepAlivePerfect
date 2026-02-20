package com.fanjun.keeplive.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.RemoteException
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.R
import com.fanjun.keeplive.config.NotificationUtils
import com.fanjun.keeplive.receiver.NotificationClickReceiver
import com.fanjun.keeplive.receiver.OnepxReceiver
import com.fanjun.keeplive.utils.ServiceUtils

/**
 * 本地服务（主进程）
 * 核心保活策略集合：前台通知、无声音乐循环、一像素 Activity、与 RemoteService 互相绑定守护
 */
class LocalService : Service() {

    private var onepxReceiver: OnepxReceiver? = null
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var isPause = true // 控制音乐暂停
    private var mediaPlayer: MediaPlayer? = null
    private val binder = MyBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var isBoundRemoteService = false

    override fun onCreate() {
        super.onCreate()
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        isPause = pm.isInteractive
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 播放无声音乐
        if (KeepLive.useSilenceMusice && mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.novioce)?.apply {
                setVolume(0f, 0f)
                setOnCompletionListener {
                    if (!isPause) {
                        if (KeepLive.runMode == KeepLive.RunMode.ROGUE) {
                            play()
                        } else {
                            handler.postDelayed({ play() }, 5000)
                        }
                    }
                }
                setOnErrorListener { _, _, _ -> false }
            }
            play()
        }

        // 注册一像素保活广播
        if (onepxReceiver == null) {
            onepxReceiver = OnepxReceiver()
        }
        registerReceiver(onepxReceiver, IntentFilter().apply {
            addAction("android.intent.action.SCREEN_OFF")
            addAction("android.intent.action.SCREEN_ON")
        })

        // 注册屏幕状态监听（控制音乐播放）
        if (screenStateReceiver == null) {
            screenStateReceiver = ScreenStateReceiver()
        }
        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction("_ACTION_SCREEN_OFF")
            addAction("_ACTION_SCREEN_ON")
        })

        // 启用前台服务通知，提升优先级
        KeepLive.foregroundNotification?.let { notification ->
            val intent2 = Intent(applicationContext, NotificationClickReceiver::class.java).apply {
                action = NotificationClickReceiver.CLICK_NOTIFICATION
            }
            val n = NotificationUtils.createNotification(
                this, notification.title, notification.description, notification.iconRes, intent2
            )
            startForeground(13691, n)
        }

        // 绑定守护进程
        try {
            isBoundRemoteService = bindService(
                Intent(this, RemoteService::class.java), connection, Context.BIND_ABOVE_CLIENT
            )
        } catch (_: Exception) {
        }

        // 隐藏服务通知（API < 25）
        try {
            if (Build.VERSION.SDK_INT < 25) {
                startService(Intent(this, HideForegroundService::class.java))
            }
        } catch (_: Exception) {
        }

        // 回调业务层
        KeepLive.keepLiveService?.onWorking()
        return START_STICKY
    }

    // 播放无声音乐
    private fun play() {
        if (KeepLive.useSilenceMusice) {
            mediaPlayer?.takeIf { !it.isPlaying }?.start()
        }
    }

    // 暂停无声音乐
    private fun pause() {
        if (KeepLive.useSilenceMusice) {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
    }

    // 屏幕状态广播接收器（控制音乐播放/暂停）
    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "_ACTION_SCREEN_OFF" -> {
                    isPause = false
                    play()
                }
                "_ACTION_SCREEN_ON" -> {
                    isPause = true
                    pause()
                }
            }
        }
    }

    // AIDL Binder 实现
    private inner class MyBinder : GuardAidl.Stub() {
        @Throws(RemoteException::class)
        override fun wakeUp(title: String?, discription: String?, iconRes: Int) {
            // 由 RemoteService 调用，无需额外逻辑
        }
    }

    // 与 RemoteService 的连接
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            // RemoteService 断开后重新启动并绑定
            if (ServiceUtils.isServiceRunning(applicationContext, "com.fanjun.keeplive.service.LocalService")) {
                startService(Intent(this@LocalService, RemoteService::class.java))
                isBoundRemoteService = bindService(
                    Intent(this@LocalService, RemoteService::class.java), this, Context.BIND_ABOVE_CLIENT
                )
            }
            // 同步屏幕状态
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                sendBroadcast(Intent("_ACTION_SCREEN_ON"))
            } else {
                sendBroadcast(Intent("_ACTION_SCREEN_OFF"))
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                KeepLive.foregroundNotification?.let { notification ->
                    val guardAidl = GuardAidl.Stub.asInterface(service)
                    guardAidl.wakeUp(notification.title, notification.description, notification.iconRes)
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (isBoundRemoteService) unbindService(connection)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(onepxReceiver)
            unregisterReceiver(screenStateReceiver)
        } catch (_: Exception) {
        }
        KeepLive.keepLiveService?.onStop()
    }
}
