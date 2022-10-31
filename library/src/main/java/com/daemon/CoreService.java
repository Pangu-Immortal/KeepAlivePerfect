package com.daemon;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.daemon.onepixel.OnePixelActivity;
import com.daemon.receiver.ScreenReceiver;
import com.daemon.utils.IntentUtils;
import com.daemon.utils.ROMUtils;

import java.util.concurrent.TimeUnit;

import static android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY;

public class CoreService extends BaseService implements Handler.Callback, ScreenReceiver.Observer {

    private static final String KEY_INTENT = "intent";
    private static final String START_ACTIVITY = "start_activity";
    private static final int STOP_MUSIC_MESSAGE = 1;
    private static final int STAT_MUSIC_MESSAGE = 2;
    private static final long STOP_MUSIC_DELAY = TimeUnit.MINUTES.toMillis(1);

    public MediaPlayer mMediaPlayer;
    public Handler mHandler;

    public class PlayErrorListener implements MediaPlayer.OnErrorListener {
        public PlayErrorListener() {
        }

        public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
            DaemonLog.d(CoreService.this.getName() + " player onError");
            return false;
        }
    }

    public class CompletionListener implements MediaPlayer.OnCompletionListener {
        public CompletionListener() {
        }

        public void onCompletion(MediaPlayer mediaPlayer) {
            DaemonLog.d(getName()+" restart music....");
            mediaPlayer.start();
        }
    }

    public class StopRunnable implements Runnable {
        public StopRunnable() {
        }

        public void run() {
            CoreService.this.stopSelf();
        }
    }

    private void startPlayMusic() {
        DaemonLog.d(getName() + " startPlay called");
        stopPlayMusic();
        try{
            mMediaPlayer = MediaPlayer.create(this,R.raw.no_notice);
            mMediaPlayer.setWakeMode(getApplicationContext(), 1);
            mMediaPlayer.setOnErrorListener(new PlayErrorListener());
            mMediaPlayer.setOnCompletionListener(new CompletionListener());
            if (ROMUtils.isHuawei() && Build.VERSION.SDK_INT >= 21) {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(USAGE_ASSISTANCE_ACCESSIBILITY).build());
            }
            //mMediaPlayer.prepare();
            mMediaPlayer.start();
        }catch (Throwable exp){
            exp.printStackTrace();
            return;
        }
    }

    private void stopPlayMusic() {
        if (mMediaPlayer != null){
            try{
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }catch (Exception exp){
                exp.printStackTrace();
            }
            this.mMediaPlayer = null;
        }
    }

    public static void start(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(new Intent(context, CoreService.class));
            }else{
                context.startService(new Intent(context, CoreService.class));
            }
        } catch (Exception unused) {
        }
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(new Intent(context, DaemonService.class));
            }else{
                context.startService(new Intent(context, DaemonService.class));
            }
        } catch (Exception unused) {
        }
    }

    public static void startForLockScreen(Context context, Intent intent) {
        IntentUtils.startActivitySafe(context, intent);
        Intent intent2 = new Intent(context, CoreService.class);
        if (intent != null) {
            intent2.putExtra(KEY_INTENT, intent);
            intent2.setAction(START_ACTIVITY);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(intent2);
            }else{
                context.startService(intent2);
            }
        } catch (Exception e2) {
            DaemonLog.d("startService onError", e2);
        }
    }

    @Override //
    public void onCreate() {
        super.onCreate();
        this.mHandler = new Handler(getMainLooper(), this);
        ScreenReceiver.addObserver(this);
        //Daemon.getCallback().onStart();
        if(!ROMUtils.isXiaomi()&&Daemon.getConfiguration().isMusicPlayEnabled()){
            startPlayMusic();
        }
    }

    @Override //
    public void onDestroy() {
        super.onDestroy();
        ScreenReceiver.removeObserver(this);
        MediaPlayer mediaPlayer = this.mMediaPlayer;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e2) {
                DaemonLog.d("mPlayer release error", e2);
            }
        }
        //Daemon.getCallback().onStop();
    }

    public boolean handleMessage(@NonNull Message message) {
        if (message.what == STOP_MUSIC_MESSAGE) {
            DaemonLog.d(getName() + " handleMessage stopPlay");
            stopPlayMusic();
        }
        if (message.what == STAT_MUSIC_MESSAGE){
            DaemonLog.d(getName() + " handleMessage startPlay");
            startPlayMusic();
        }
        return true;
    }


    @Override
    public void screenStatusChanged(Boolean screenON) {
        DaemonLog.d(getName() + " screenStatusChanged->" + screenON);
        if (screenON) {
            OnePixelActivity.finishIfExist();
            mHandler.sendEmptyMessageDelayed(STOP_MUSIC_MESSAGE, STOP_MUSIC_DELAY);
        } else {
            if (Daemon.getConfiguration().isOnePixelActivityEnabled() && !ROMUtils.isHuawei()) {
                DaemonLog.d(getName()+" start onepixelactivity");
                IntentUtils.startActivitySafe(Daemon.getApplication(), OnePixelActivity.class);
            }
            if (Daemon.getConfiguration().isMusicPlayEnabled() && !ROMUtils.isXiaomi()) {
                DaemonLog.d(getName()+" start playmusic");
                mHandler.sendEmptyMessage(STAT_MUSIC_MESSAGE);
            }
        }
    }

    @Override //
    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent,i,i2);
        Intent intent2;
        if (intent == null || !START_ACTIVITY.equals(intent.getAction()) || (intent2 = (Intent) intent.getParcelableExtra(KEY_INTENT)) == null) {
            return START_STICKY;
        }
        boolean isOppo = ROMUtils.isOppo();
        if (isOppo) {
            startPlayMusic();
        }
        IntentUtils.startActivitySafe(getApplicationContext(), intent2);
        DaemonLog.d(getName() + " startActivity,targetIntent=" + intent2);
        if (!isOppo) {
            return START_STICKY;
        }
        mHandler.postDelayed(new StopRunnable(), 5000);
        return START_STICKY;
    }
}