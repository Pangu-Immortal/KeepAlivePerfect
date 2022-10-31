package com.daemonLibrary.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.daemon.Daemon

class SystemBroadcastReceiver : BroadcastReceiver() {
    private fun startActivity(context: Context, intent: Intent) {
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        if (Build.VERSION.SDK_INT < 21) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            context.startActivity(intent)
            return
        }
        intent.addCategory("android.intent.category.LAUNCHER")
        intent.action = "android.intent.action.MAIN"
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        Daemon.startActivity(context, intent)
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1 == null) {
            return
        }
        when (p1.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED -> {
                startActivity(
                    Daemon.getApplication(),
                    Intent(Daemon.getApplication(), OnRemoveActivity::class.java)
                )
            }
        }
    }
}