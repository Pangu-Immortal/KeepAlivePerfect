
#include <jni.h>
#include <sys/wait.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/file.h>
#include <linux/android/binder.h>
#include <sys/mman.h>
#include "data_transact.h"
#include "cParcel.h"

#define    DAEMON_CALLBACK_NAME        "onDaemonDead"

using namespace android;

extern "C" {
void set_process_name(JNIEnv *env) {
    jclass process = env->FindClass("android/os/Process");
    jmethodID setArgV0 = env->GetStaticMethodID(process, "setArgV0", "(Ljava/lang/String;)V");
    jstring name = env->NewStringUTF("app_d");
    env->CallStaticVoidMethod(process, setArgV0, name);
}

void writeIntent(Parcel &out, const char *mPackage, const char *mClass) {
    // mAction
    out.writeString16(NULL, 0);
    // uri mData
    out.writeInt32(0);
    // mType
    out.writeString16(NULL, 0);
//    // mIdentifier
    out.writeString16(NULL, 0);
    // mFlags
    out.writeInt32(0);
    // mPackage
    out.writeString16(NULL, 0);
    // mComponent
    out.writeString16(String16(mPackage));
    out.writeString16(String16(mClass));
    // mSourceBounds
    out.writeInt32(0);
    // mCategories
    out.writeInt32(0);
    // mSelector
    out.writeInt32(0);
    // mClipData
    out.writeInt32(0);
    // mContentUserHint
    out.writeInt32(-2);
    // mExtras
    out.writeInt32(-1);
}

void writeService(Parcel &out, const char *mPackage, const char *mClass, int sdk_version) {
    if (sdk_version >= 26) {
        out.writeInterfaceToken(String16("android.app.IActivityManager"));
        out.writeNullBinder();
        out.writeInt32(1);
        writeIntent(out, mPackage, mClass);
        out.writeString16(NULL, 0); // resolvedType
        // mServiceData.writeInt(context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O ? 1 : 0);
        out.writeInt32(0);
        out.writeString16(String16(mPackage)); // callingPackage
        out.writeInt32(0);
    } else if (sdk_version >= 23) {
        out.writeInterfaceToken(String16("android.app.IActivityManager"));
        out.writeNullBinder();
        writeIntent(out, mPackage, mClass);
        out.writeString16(NULL, 0); // resolvedType
        out.writeString16(String16(mPackage)); // callingPackage
        out.writeInt32(0); // userId
    } else {
        out.writeInterfaceToken(String16("android.app.IActivityManager"));
        out.writeNullBinder();
        writeIntent(out, mPackage, mClass);
        out.writeString16(NULL, 0); // resolvedType
        out.writeInt32(0); // userId
    }
}

#define CHECK_SERVICE_TRANSACTION 1
uint32_t get_service(const char *serviceName, int mDriverFD) {
    Parcel *data1 = new Parcel;//, reply;
    Parcel *reply = new Parcel;
    data1->writeInterfaceToken(String16("android.os.IServiceManager"));
    data1->writeString16(String16(serviceName));
//    remote()->transact(CHECK_SERVICE_TRANSACTION, data, &reply);
    // BpBinder.transact

    status_t status = write_transact(0, CHECK_SERVICE_TRANSACTION, *data1, reply, 0, mDriverFD);
    const flat_binder_object *flat = reply->readObject(false);
    if (flat) {
        LOGD("write_transact handle is:%llu", flat->handle);
        return flat->handle;
    }
    return 0;
}

void create_file_if_not_exist(char *path) {
    FILE *fp = fopen(path, "ab+");
    if (fp) {
        fclose(fp);
    }
}

void notify_and_waitfor(char *observer_self_path, char *observer_daemon_path) {
    int observer_self_descriptor = open(observer_self_path, O_RDONLY);
    if (observer_self_descriptor == -1) {
        observer_self_descriptor = open(observer_self_path, O_CREAT, S_IRUSR | S_IWUSR);
    }
    int observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
    while (observer_daemon_descriptor == -1) {
        usleep(1000);
        observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
    }
    remove(observer_daemon_path);
    LOGE("Watched >>>>OBSERVER<<<< has been ready...");
}


int lock_file(char *lock_file_path) {
    LOGD("start try to lock file >> %s <<", lock_file_path);
    int lockFileDescriptor = open(lock_file_path, O_RDONLY);
    if (lockFileDescriptor == -1) {
        lockFileDescriptor = open(lock_file_path, O_CREAT, S_IRUSR);
    }
    int lockRet = flock(lockFileDescriptor, LOCK_EX);
    if (lockRet == -1) {
        LOGE("lock file failed >> %s <<", lock_file_path);
        return 0;
    } else {
        LOGD("lock file success  >> %s <<", lock_file_path);
        return 1;
    }
}

void java_callback(JNIEnv *env, jobject jobj, char *method_name) {
    jclass cls = env->GetObjectClass(jobj);
    jmethodID cb_method = env->GetMethodID(cls, method_name, "()V");
    env->CallVoidMethod(jobj, cb_method);
}

void do_daemon(JNIEnv *env, jobject jobj, char *indicator_self_path, char *indicator_daemon_path,
               char *observer_self_path, char *observer_daemon_path,
               const char *pkgName, const char *serviceName, int sdk_version,
               uint32_t transact_code) {
    int lock_status = 0;
    int try_time = 0;
    while (try_time < 3 && !(lock_status = lock_file(indicator_self_path))) {
        try_time++;
        LOGD("Persistent lock myself failed and try again as %d times", try_time);
        usleep(10000);
    }
    if (!lock_status) {
        LOGE("Persistent lock myself failed and exit");
        return;
    }

    notify_and_waitfor(observer_self_path, observer_daemon_path);

    int pid = getpid();


    // 1.获取service_manager, handle=0
    // 根据BpBinder(C++)生成BinderProxy(Java)对象. 主要工作是创建BinderProxy对象,并把BpBinder对象地址保存到BinderProxy.mObject成员变量
    // ServiceManagerNative.asInterface(BinderInternal.getContextObject()) = ServiceManagerNative.asInterface(new BinderProxy())
    // ServiceManagerNative.asInterface(new BinderProxy()) = new ServiceManagerProxy(new BinderProxy())
    // sp<IBinder> b = ProcessState::self()->getContextObject(NULL); // BpBinder

    // flatten_binder 将Binder对象扁平化，转换成flat_binder_object对象。
    //  BpBinder *proxy = binder->remoteBinder();
    //  const int32_t handle = proxy ? proxy->handle() : 0;

    // 2.获取activity服务
    // IBinder在Java层已知
    // mRemote.transact(GET_SERVICE_TRANSACTION, data, reply, 0);
    // 得到binder_transaction_data结构体
    // 参考native实现，获取不到则循环5次

    int mDriverFD = open_driver();
    void *mVMStart = MAP_FAILED;
    initProcessState(mDriverFD, mVMStart);

    uint32_t handle = get_service("activity", mDriverFD);
    Parcel *data = new Parcel;
    LOGD("writeService %s %s", pkgName, serviceName);
//    writeService(*data, pkgName, serviceName, sdk_version);
// com.boolbird.keepalive com.boolbird.keepalive.demo.Service1
    writeService(*data, pkgName, serviceName, sdk_version);

    LOGD("Watch >>>>to lock_file<<<<< !!");
    lock_status = lock_file(indicator_daemon_path);
    if (lock_status) {
        LOGE("Watch >>>>DAEMON<<<<< Daed !!");
        status_t status = write_transact(handle, transact_code, *data, NULL, 1, mDriverFD);
        LOGD("writeService result is %d", status);
//        int result = binder.get()->transact(code, parcel, NULL, 1);
        remove(observer_self_path);// it`s important ! to prevent from deadlock
//        java_callback(env, jobj, DAEMON_CALLBACK_NAME);
        if (pid > 0) {
            killpg(pid, SIGTERM);
        }
    }
    delete data;
}

JNIEXPORT void JNICALL
Java_com_boolbird_keepalive_NativeKeepAlive_doDaemon(JNIEnv *env, jobject jobj,
                                                     jstring indicatorSelfPath,
                                                     jstring indicatorDaemonPath,
                                                     jstring observerSelfPath,
                                                     jstring observerDaemonPath,
                                                     jstring packageName,
                                                     jstring serviceName,
                                                     jint sdk_version) {
    if (indicatorSelfPath == NULL || indicatorDaemonPath == NULL || observerSelfPath == NULL ||
        observerDaemonPath == NULL) {
        LOGE("parameters cannot be NULL !");
        return;
    }

    uint32_t transact_code = 0;
    switch (sdk_version) {
        case 26:
        case 27:
            transact_code = 26;
            break;
        case 28:
            transact_code = 30;
            break;
        case 29:
            transact_code = 24;
            break;
        default:
            transact_code = 34;
            break;
    }

    char *indicator_self_path = (char *) env->GetStringUTFChars(indicatorSelfPath, 0);
    char *indicator_daemon_path = (char *) env->GetStringUTFChars(indicatorDaemonPath, 0);
    char *observer_self_path = (char *) env->GetStringUTFChars(observerSelfPath, 0);
    char *observer_daemon_path = (char *) env->GetStringUTFChars(observerDaemonPath, 0);
    char *pkgName = (char *) env->GetStringUTFChars(packageName, 0);
    char *svcName = (char *) env->GetStringUTFChars(serviceName, 0);

    pid_t pid;
    if ((pid = fork()) < 0) {
        LOGE("fork 1 error\n");
        exit(-1);
    } else if (pid == 0) { //第一个子进程
        if ((pid = fork()) < 0) {
            LOGE("fork 2 error\n");
            exit(-1);
        } else if (pid > 0) {
            // 托孤
            exit(0);
        }

        LOGD("mypid: %d", getpid());
        const int MAX_PATH = 256;
        char indicator_self_path_child[MAX_PATH];
        char indicator_daemon_path_child[MAX_PATH];
        char observer_self_path_child[MAX_PATH];
        char observer_daemon_path_child[MAX_PATH];

        strcpy(indicator_self_path_child, indicator_self_path);
        strcat(indicator_self_path_child, "-c");

        strcpy(indicator_daemon_path_child, indicator_daemon_path);
        strcat(indicator_daemon_path_child, "-c");

        strcpy(observer_self_path_child, observer_self_path);
        strcat(observer_self_path_child, "-c");

        strcpy(observer_daemon_path_child, observer_daemon_path);
        strcat(observer_daemon_path_child, "-c");

        create_file_if_not_exist(indicator_self_path_child);
        create_file_if_not_exist(indicator_daemon_path_child);

        set_process_name(env);

        // 直接传递parcel，会导致监听不到进程被杀；改成传输u8*数据解决了
        do_daemon(env, jobj, indicator_self_path_child, indicator_daemon_path_child,
                  observer_self_path_child, observer_daemon_path_child, pkgName, svcName,
                  sdk_version, transact_code);
    }

    if (waitpid(pid, NULL, 0) != pid)
        LOGE("waitpid error\n");

    LOGD("do_daemon pid=%d ppid=%d", getpid(), getppid());
    do_daemon(env, jobj, indicator_self_path, indicator_daemon_path, observer_self_path,
              observer_daemon_path, pkgName, svcName, sdk_version, transact_code);
}

}extern "C"
JNIEXPORT void JNICALL
Java_com_boolbird_keepalive_NativeKeepAlive_test(JNIEnv *env, jobject thiz, jstring packageName,
                                                 jstring serviceName, jint sdk_version) {
    int mDriverFD = open_driver();
    void *mVMStart = MAP_FAILED;
    initProcessState(mDriverFD, mVMStart);

    uint32_t handle = get_service("activity", mDriverFD);
//    get_service("sensor");
//    get_service("power");
//    get_service("storage");
//    get_service("phone");

    char *pkgName = (char *) env->GetStringUTFChars(packageName, 0);
    char *svcName = (char *) env->GetStringUTFChars(serviceName, 0);

    Parcel *data = new Parcel;
    writeService(*data, pkgName, svcName, sdk_version);

    uint32_t transact_code = 0;
    switch (sdk_version) {
        case 26:
        case 27:
            transact_code = 26;
            break;
        case 28:
            transact_code = 30;
            break;
        case 29:
            transact_code = 24;
            break;
        default:
            transact_code = 34;
            break;
    }

    status_t status = write_transact(handle, transact_code, *data, NULL, 1, mDriverFD);
    LOGD("writeService result is %d", status);
    delete data;
    unInitProcessState(mDriverFD, mVMStart);
}