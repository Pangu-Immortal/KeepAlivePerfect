package com.boolbird.keepalive.demo

import android.app.Application
import android.content.Context
import android.util.Log
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.config.ForegroundNotification
import com.fanjun.keeplive.config.KeepLiveService

/**
 * Doc说明 (此类核心功能):
 * +---------------------------+
 * | @author qihao             |
 * | @date on 2021/5/10 15:28 |
 * +---------------------------+
 * ┌─────────────────────────────────────────────────────────────┐
 * │┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐│
 * ││Esc│!1 │@2 │#3 │$4 │%5 │^6 │&7 │*8 │(9 │)0 │_- │+= │|\ │`~ ││
 * │├───┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴───┤│
 * ││ Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{[ │}] │ BS  ││
 * │├─────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤│
 * ││ Ctrl │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  ││
 * │├──────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────┬───┤│
 * ││ Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│Shift │Fn ││
 * │├──────┬─┴┬──┴──┬┴───┴───┴───┴───┴───┴──┬┴───┴┬──┴┬─────┴───┘│
 * ││option│Fn│ Alt │         Space         │ Alt │Win│   qihao  │
 * │└──────┴──┴─────┴───────────────────────┴─────┴───┘          │
 * └─────────────────────────────────────────────────────────────┘
 *
 */

private const val TAG = "MainApplication"

class MainApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        Log.d(TAG, "attachBaseContext")


        // 定义前台通知的默认样式
        val foregroundNotification = ForegroundNotification(
            "标题", "描述", R.drawable.ic_small_notification
        ) { context, intent ->
            // 定义前台服务的通知点击事件

        }
        //启动保活服务
        KeepLive.startWork(this,
            KeepLive.RunMode.ENERGY,
            foregroundNotification,  //你需要保活的服务，如socket连接、定时任务等，建议不用匿名内部类的方式在这里写
            object : KeepLiveService {
                /**
                 * 运行中
                 * 由于服务可能会多次自动启动，该方法可能重复调用
                 */
                override fun onWorking() {
                    //  do something
                }

                /**
                 * 服务终止
                 * 由于服务可能会被多次终止，该方法可能重复调用，需同onWorking配套使用，如注册和注销broadcast
                 */
                override fun onStop() {
                    //   do something
                }
            }
        )
    }
}