package com.daemon.process;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Daemon {

    private static final String TAG = "Daemon";

    private Configs mConfigurations;

    public  static Context context;

    private Daemon(Configs configurations) {
        this.mConfigurations = configurations;
    }

    public static void init(Context base, Configs configurations) {
        //Reflection.unseal(base);
        Daemon.context = base;
        Daemon client = new Daemon(configurations);
        client.initDaemon(base);
    }


    private final String DAEMON_PERMITTING_SP_FILENAME = "D16EA7BB-AC54-4CDD-8C69-50D0838F4DE4";
    private final String DAEMON_PERMITTING_SP_KEY = "permitted";


    private BufferedReader mBufferedReader;

    private void initDaemon(Context base) {
        if (mConfigurations == null) {
            return;
        }

        String processName = getProcessName();
        String packageName = base.getPackageName();

        if (processName.startsWith(mConfigurations.PERSISTENT_CONFIG.processName)) {
            Log.d("DEMO","onPersistentCreate");
            IProcess.Fetcher.fetchStrategy().onPersistentCreate(base, mConfigurations);
        } else if (processName.startsWith(mConfigurations.DAEMON_ASSISTANT_CONFIG.processName)) {
            Log.d("DEMO","onDaemonAssistantCreate");
            IProcess.Fetcher.fetchStrategy().onDaemonAssistantCreate(base, mConfigurations);
        } else if (processName.startsWith(packageName)) {
            Log.d("DEMO","onInit");
            IProcess.Fetcher.fetchStrategy().onInit(base);
        }

        releaseIO();
    }


    private String getProcessName() {
        try {
            File file = new File("/proc/self/cmdline");
            mBufferedReader = new BufferedReader(new FileReader(file));
            return mBufferedReader.readLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void releaseIO() {
        if (mBufferedReader != null) {
            try {
                mBufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBufferedReader = null;
        }
    }

    private boolean isDaemonPermitting(Context context) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        return sp.getBoolean(DAEMON_PERMITTING_SP_KEY, true);
    }

    protected boolean setDaemonPermitting(Context context, boolean isPermitting) {
        SharedPreferences sp = context.getSharedPreferences(DAEMON_PERMITTING_SP_FILENAME, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putBoolean(DAEMON_PERMITTING_SP_KEY, isPermitting);
        return editor.commit();
    }

}
