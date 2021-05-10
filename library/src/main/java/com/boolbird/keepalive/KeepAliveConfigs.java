package com.boolbird.keepalive;

import android.content.Context;
import android.content.Intent;

/**
 * Doc说明 (此类核心功能):
 * +---------------------------+
 * | @author qihao             |
 * | @date on 2021/5/10 16:13 |
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
public class KeepAliveConfigs {

    final Config PERSISTENT_CONFIG;
    final Config DAEMON_ASSISTANT_CONFIG;
    boolean ignoreOptimization = false;
    int rebootIntervalMs = 5000; // 10s
    int rebootMaxTimes = 3;
    boolean limitReboot = false;

    static OnBootReceivedListener bootReceivedListener;

    public KeepAliveConfigs(Config persistentConfig, Config daemonAssistantConfig) {
        this.PERSISTENT_CONFIG = persistentConfig;
        this.DAEMON_ASSISTANT_CONFIG = daemonAssistantConfig;
    }

    public KeepAliveConfigs(Config persistentConfig) {
        this.PERSISTENT_CONFIG = persistentConfig;
        this.DAEMON_ASSISTANT_CONFIG = new Config("android.process.daemon", KeepAliveService.class.getCanonicalName());
    }

    public KeepAliveConfigs ignoreBatteryOptimization() {
        ignoreOptimization = true;
        return this;
    }

    public KeepAliveConfigs rebootThreshold(int rebootIntervalMs, int rebootMaxTimes) {
        limitReboot = true;
        this.rebootIntervalMs = rebootIntervalMs;
        this.rebootMaxTimes = rebootMaxTimes;
        return this;
    }

    public KeepAliveConfigs setOnBootReceivedListener(OnBootReceivedListener listener) {
        bootReceivedListener = listener;
        return this;
    }

    public static class Config {

        final String processName;
        final String serviceName;

        public Config(String processName, String serviceName) {
            this.processName = processName;
            this.serviceName = serviceName;
        }
    }

    public interface OnBootReceivedListener {
        void onReceive(Context context, Intent intent);
    }
}
