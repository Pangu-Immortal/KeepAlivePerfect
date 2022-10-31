package com.daemon;

import android.app.Instrumentation;
import android.os.Bundle;

public class DInstrumentation extends Instrumentation {
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        DaemonLog.d("DInstrumentation.OnCreate");
    }

    public void onDestroy() {
        super.onDestroy();
    }
}