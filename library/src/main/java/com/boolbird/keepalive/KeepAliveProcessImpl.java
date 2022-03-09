package com.boolbird.keepalive;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Doc说明 (此类核心功能):
 * +---------------------------+
 * | @author qihao             |
 * | @date on 2021/5/10 15:29 |
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
public class KeepAliveProcessImpl implements IKeepAliveProcess {

    private static final String TAG = "KeepAliveProcessImpl";

    private final static String INDICATOR_DIR_NAME = "indicators";
    private final static String INDICATOR_PERSISTENT_FILENAME = "indicator_p";
    private final static String INDICATOR_DAEMON_ASSISTANT_FILENAME = "indicator_d";
    private final static String OBSERVER_PERSISTENT_FILENAME = "observer_p";
    private final static String OBSERVER_DAEMON_ASSISTANT_FILENAME = "observer_d";

    private IBinder mRemote;
    private Parcel mServiceData;

    private int mPid = Process.myPid();

    private static int transactCode;

    static {
        switch (Build.VERSION.SDK_INT) {
            case 26:
            case 27:
                transactCode = 26;
                break;
            case 28:
                transactCode = 30;
                break;
            case 29:
                transactCode = 24;
                break;
            default:
                transactCode = 34;
                break;
        }
    }

    @Override
    public boolean onInit(Context context, KeepAliveConfigs configs) {
        if (configs.ignoreOptimization) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isIgnoringBatteryOptimizations(context)) {
                    requestIgnoreBatteryOptimizations(context);
                }
            }
        }

        return initIndicatorFiles(context);
    }

    @Override
    public void onPersistentCreate(final Context context, KeepAliveConfigs configs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        initAmsBinder();
        initServiceParcel(context, configs.DAEMON_ASSISTANT_CONFIG.serviceName);
        startServiceByAmsBinder();

        Thread t = new Thread() {
            public void run() {
                try {
                    File indicatorDir = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
                    new NativeKeepAlive().doDaemon(
                            new File(indicatorDir, INDICATOR_PERSISTENT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, INDICATOR_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, OBSERVER_PERSISTENT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, OBSERVER_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                            transactCode, getNativePtr(mServiceData));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    @Override
    public void onDaemonAssistantCreate(final Context context, KeepAliveConfigs configs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        initAmsBinder();
        initServiceParcel(context, configs.PERSISTENT_CONFIG.serviceName);
        startServiceByAmsBinder();

        Thread t = new Thread() {
            public void run() {
                try {
                    File indicatorDir = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
                    new NativeKeepAlive().doDaemon(
                            new File(indicatorDir, INDICATOR_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, INDICATOR_PERSISTENT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, OBSERVER_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                            new File(indicatorDir, OBSERVER_PERSISTENT_FILENAME).getAbsolutePath(),
                            transactCode, getNativePtr(mServiceData));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    @Override
    public void onDaemonDead() {
        Log.i(TAG, "on daemon dead!");
        if (startServiceByAmsBinder()) {
            int pid = Process.myPid();
            Log.i(TAG, "mPid: " + mPid + " current pid: " + pid);
            Process.killProcess(mPid);
        }
    }

    private void initAmsBinder() {
        Class<?> activityManagerNative;
        try {
            activityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Object amn = activityManagerNative.getMethod("getDefault").invoke(activityManagerNative);
            if (amn == null) {
                return;
            }
            Field mRemoteField = amn.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            mRemote = (IBinder) mRemoteField.get(amn);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // when processName dead, we should save time to restart and kill self, don`t take a waste of time to recycle
    private void initServiceParcel(Context context, String serviceName) {
        Intent intent = new Intent();
        ComponentName component = new ComponentName(context.getPackageName(), serviceName);
        intent.setComponent(component);

        Parcel parcel = Parcel.obtain();
        intent.writeToParcel(parcel, 0);

        mServiceData = Parcel.obtain();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /* Android 8.1 frameworks/base/core/java/android/app/IActivityManager.aidl
             * ComponentName startService(in IApplicationThread caller, in Intent service,
             *    in String resolvedType, boolean requireForeground, in String callingPackage, int userId);
             *
             * frameworks/base/services/core/java/com/android/server/am/ActiveServices.java
             * if (fgRequired) {
             *     final int mode = mAm.mAppOpsService.checkOperation(
             *             AppOpsManager.OP_START_FOREGROUND, r.appInfo.uid, r.packageName);
             *     switch (mode) {
             *         case AppOpsManager.MODE_ALLOWED:
             *         case AppOpsManager.MODE_DEFAULT: // All okay.
             *             break;
             *         case AppOpsManager.MODE_IGNORED:
             *             // Not allowed, fall back to normal start service, failing siliently if background check restricts that.
             *             fgRequired = false;
             *             forceSilentAbort = true;
             *             break;
             *         default:
             *             return new ComponentName("!!", "foreground not allowed as per app op");
             *     }
             * }
             * requireForeground 要求启动service之后，调用service.startForeground()显示一个通知，不然会崩溃
             */
            mServiceData.writeInterfaceToken("android.app.IActivityManager");
            mServiceData.writeStrongBinder(null);
            mServiceData.writeInt(1);
            intent.writeToParcel(mServiceData, 0);
            mServiceData.writeString(null); // resolvedType
//            mServiceData.writeInt(context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O ? 1 : 0);
            mServiceData.writeInt(0);
            mServiceData.writeString(context.getPackageName()); // callingPackage
            mServiceData.writeInt(0); // userId
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // http://aospxref.com/android-7.1.2_r36/xref/frameworks/base/core/java/android/app/ActivityManagerNative.java
            /* ActivityManagerNative#START_SERVICE_TRANSACTION
             *  case START_SERVICE_TRANSACTION: {
             *             data.enforceInterface(IActivityManager.descriptor);
             *             IBinder b = data.readStrongBinder();
             *             IApplicationThread app = ApplicationThreadNative.asInterface(b);
             *             Intent service = Intent.CREATOR.createFromParcel(data);
             *             String resolvedType = data.readString();
             *             String callingPackage = data.readString();
             *             int userId = data.readInt();
             *             ComponentName cn = startService(app, service, resolvedType, callingPackage, userId);
             *             reply.writeNoException();
             *             ComponentName.writeToParcel(cn, reply);
             *             return true;
             *         }
             */
            mServiceData.writeInterfaceToken("android.app.IActivityManager");
            mServiceData.writeStrongBinder(null);
            intent.writeToParcel(mServiceData, 0);
            mServiceData.writeString(null);  // resolvedType
            mServiceData.writeString(context.getPackageName()); // callingPackage
            mServiceData.writeInt(0); // userId
        } else {
            /* Android4.4 ActivityManagerNative#START_SERVICE_TRANSACTION
             * case START_SERVICE_TRANSACTION: {
             *             data.enforceInterface(IActivityManager.descriptor);
             *             IBinder b = data.readStrongBinder();
             *             IApplicationThread app = ApplicationThreadNative.asInterface(b);
             *             Intent service = Intent.CREATOR.createFromParcel(data);
             *             String resolvedType = data.readString();
             *             int userId = data.readInt();
             *             ComponentName cn = startService(app, service, resolvedType, userId);
             *             reply.writeNoException();
             *             ComponentName.writeToParcel(cn, reply);
             *             return true;
             *         }
             */
            mServiceData.writeInterfaceToken("android.app.IActivityManager");
            mServiceData.writeStrongBinder(null);
            intent.writeToParcel(mServiceData, 0);
            mServiceData.writeString(null);  // resolvedType
            mServiceData.writeInt(0); // userId
        }
    }

    private boolean startServiceByAmsBinder() {
        try {
            if (mRemote == null || mServiceData == null) {
                Log.e("Daemon", "REMOTE IS NULL or PARCEL IS NULL !!!");
                return false;
            }
            mRemote.transact(transactCode, mServiceData, null, 1); // flag=FLAG_ONEWAY=1
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean initIndicatorFiles(Context context) {
        File dirFile = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        try {
            createNewFile(dirFile, INDICATOR_PERSISTENT_FILENAME);
            createNewFile(dirFile, INDICATOR_DAEMON_ASSISTANT_FILENAME);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createNewFile(File dirFile, String fileName) throws IOException {
        File file = new File(dirFile, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    private static long getNativePtr(Parcel parcel) {
        try {
            Field ptrField = parcel.getClass().getDeclaredField("mNativePtr");
            ptrField.setAccessible(true);
            // android19的mNativePtr是int类型，高版本是long类型
            return (long) ptrField.get(parcel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isIgnoringBatteryOptimizations(Context context) {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return isIgnoring;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void requestIgnoreBatteryOptimizations(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
