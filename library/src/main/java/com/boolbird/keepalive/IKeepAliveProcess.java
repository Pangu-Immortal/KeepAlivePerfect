package com.boolbird.keepalive;

import android.content.Context;

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
public interface IKeepAliveProcess {
    /**
     * Initialization some files or other when 1st time
     */
    boolean onInit(Context context, KeepAliveConfigs configs);

    /**
     * when Persistent processName create
     */
    void onPersistentCreate(Context context, KeepAliveConfigs configs);

    /**
     * when DaemonAssistant processName create
     */
    void onDaemonAssistantCreate(Context context, KeepAliveConfigs configs);

    /**
     * when watches the processName dead which it watched
     */
    void onDaemonDead();


    class Fetcher {

        private static volatile IKeepAliveProcess mDaemonStrategy;

        /**
         * fetch the strategy for this device
         *
         * @return the daemon strategy for this device
         */
        static IKeepAliveProcess fetchStrategy() {
            if (mDaemonStrategy != null) {
                return mDaemonStrategy;
            }
            mDaemonStrategy = new KeepAliveProcessImpl();
            return mDaemonStrategy;
        }
    }
}
