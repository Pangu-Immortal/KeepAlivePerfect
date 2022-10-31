package com.daemon;


import android.util.Log;

public class DaemonLog {
    public static final String TAG = "DEMO:";

    public static void d(String str) {
        Log.d(TAG,str);
    }

    public static void d(String str, Throwable th) {
        Log.d(TAG,str,th);
    }

    public static void e(String str) {
        Log.e(TAG,str);
    }

    public static void e(String str, Throwable th) {
        Log.e(TAG,str,th);
    }
}