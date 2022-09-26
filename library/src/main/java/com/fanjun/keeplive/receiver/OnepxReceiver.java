package com.fanjun.keeplive.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import com.fanjun.keeplive.activity.OnePixelActivity;

public final class OnepxReceiver extends BroadcastReceiver {
    android.os.Handler mHander;
    boolean screenOn = true;

    public OnepxReceiver() {
        mHander = new android.os.Handler(Looper.getMainLooper());
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {    //屏幕关闭的时候接受到广播
            screenOn = false;
            mHander.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!screenOn){
                        Intent intent2 = new Intent(context, OnePixelActivity.class);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent2, 0);
                        try {
                            pendingIntent.send();
                            /*} catch (PendingIntent.CanceledException e) {*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            },1000);
            //通知屏幕已关闭，开始播放无声音乐
            context.sendBroadcast(new Intent("_ACTION_SCREEN_OFF"));
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {   //屏幕打开的时候发送广播  结束一像素
            screenOn = true;
            //通知屏幕已点亮，停止播放无声音乐
            context.sendBroadcast(new Intent("_ACTION_SCREEN_ON"));
        }
    }
}
