package com.boolbird.keepalive;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

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
public class KeepAliveService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindDaemonService();
    }

    private IBinder binder;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = iBinder;
            try {
                iBinder.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        public void binderDied() {
            if (binder != null) {
                binder.unlinkToDeath(this, 0);
                binder = null;
            }
            bindDaemonService();
        }
    };

    protected void bindDaemonService() {
        KeepAliveConfigs configs;
        if (KeepAlive.client != null && KeepAlive.client.mConfigurations != null) {
            String processName = KeepAlive.getProcessName();
            configs = KeepAlive.client.mConfigurations;

            if (processName == null) {
                return;
            }
            if (processName.startsWith(configs.PERSISTENT_CONFIG.processName)) {
                Intent intent = new Intent();
                ComponentName component = new ComponentName(getPackageName(), configs.DAEMON_ASSISTANT_CONFIG.serviceName);
                intent.setComponent(component);
                bindService(intent, conn, BIND_AUTO_CREATE);
            } else if (processName.startsWith(configs.DAEMON_ASSISTANT_CONFIG.processName)) {
                Intent intent = new Intent();
                ComponentName component = new ComponentName(getPackageName(), configs.PERSISTENT_CONFIG.serviceName);
                intent.setComponent(component);
                bindService(intent, conn, BIND_AUTO_CREATE);
            }
        }
    }
}
