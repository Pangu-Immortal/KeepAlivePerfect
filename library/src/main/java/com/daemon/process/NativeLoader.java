package com.daemon.process;

/* package */class NativeLoader {

    static {
        try {
            System.loadLibrary("daemon");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public native void doDaemon(String indicatorSelfPath, String indicatorDaemonPath, String observerSelfPath, String observerDaemonPath);

    public void onDaemonDead() {
        IProcess.Fetcher.fetchStrategy().onDaemonDead();
    }
}
