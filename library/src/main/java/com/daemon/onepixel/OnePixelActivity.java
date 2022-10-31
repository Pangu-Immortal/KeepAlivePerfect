package com.daemon.onepixel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import com.daemon.CoreService;


public class OnePixelActivity extends Activity {

    private static final String TAG = "OnePixelActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window mWindow = getWindow();
        mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams attrParams = mWindow.getAttributes();
        attrParams.x = 0;
        attrParams.y = 0;
        attrParams.height = 1;
        attrParams.width = 1;
        mWindow.setAttributes(attrParams);
        OnePixelActivity.sActivity = this;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "OnDestroy---- start WatchDogService");
        Intent intentAlive = new Intent(this, CoreService.class);
        startService(intentAlive);
        super.onDestroy();
        OnePixelActivity.sActivity = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScreenOn()) {
            finishSelf();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        finishSelf();
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        finishSelf();
        return super.onTouchEvent(motionEvent);
    }

    public void finishSelf() {
        if (!isFinishing()) {
            finish();
        }
    }


    private boolean isScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }

    private static OnePixelActivity sActivity = null;

    public static  void finishIfExist(){
        if (sActivity != null){
            sActivity.finish();
        }
    }
}
