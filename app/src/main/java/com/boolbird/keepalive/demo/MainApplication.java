package com.boolbird.keepalive.demo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.boolbird.keepalive.KeepAlive;
import com.boolbird.keepalive.KeepAliveConfigs;
import com.boolbird.keepalive.Reflection;


/**
 * Doc说明 (此类核心功能):
 * +---------------------------+
 * | @author qihao             |
 * | @date on 2021/5/10 15:28 |
 * +---------------------------+
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐│
 *  ││Esc│!1 │@2 │#3 │$4 │%5 │^6 │&7 │*8 │(9 │)0 │_- │+= │|\ │`~ ││
 *  │├───┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴───┤│
 *  ││ Tab │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │{[ │}] │ BS  ││
 *  │├─────┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴┬──┴─────┤│
 *  ││ Ctrl │ A │ S │ D │ F │ G │ H │ J │ K │ L │: ;│" '│ Enter  ││
 *  │├──────┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────┬───┤│
 *  ││ Shift  │ Z │ X │ C │ V │ B │ N │ M │< ,│> .│? /│Shift │Fn ││
 *  │└─────┬──┴┬──┴──┬┴───┴───┴───┴───┴───┴──┬┴───┴┬──┴┬─────┴───┘│
 *  │      │Fn │ Alt │         Space         │ Alt │Win│   qihao  │
 *  │      └───┴─────┴───────────────────────┴─────┴───┘          │
 *  └─────────────────────────────────────────────────────────────┘
 *
 */
public class MainApplication extends Application {
    private static final String TAG = "MainApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "attachBaseContext");

        Reflection.unseal(base);

        KeepAliveConfigs configs = new KeepAliveConfigs(
                new KeepAliveConfigs.Config(getPackageName() + ":resident",
                        Service1.class.getCanonicalName()));
        configs.ignoreBatteryOptimization();
        // configs.rebootThreshold(10*1000, 3);
        configs.setOnBootReceivedListener(new KeepAliveConfigs.OnBootReceivedListener() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive boot");
                // 设置服务自启
                context.startService(new Intent(context, Service1.class));
            }
        });
        KeepAlive.init(base, configs);
    }
}
