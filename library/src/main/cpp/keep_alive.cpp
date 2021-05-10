
#include <jni.h>
#include <sys/wait.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/file.h>
#include <binder/IServiceManager.h>
#include <binder/Parcel.h>

#define TAG        "KeepAlive"
#define LOGI(...)    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...)    __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...)    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define    DAEMON_CALLBACK_NAME        "onDaemonDead"

using namespace android;

extern "C" {
void set_process_name(JNIEnv *env) {
    jclass process = env->FindClass("android/os/Process");
    jmethodID setArgV0 = env->GetStaticMethodID(process, "setArgV0", "(Ljava/lang/String;)V");
    jstring name = env->NewStringUTF("app_d");
    env->CallStaticVoidMethod(process, setArgV0, name);
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
               char *observer_self_path, char *observer_daemon_path, int code,
               const uint8_t *data, size_t data_size) {
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

    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder = sm->getService(String16("activity"));
    Parcel parcel;
    parcel.setData(data, data_size);

    int pid = getpid();

    LOGD("Watch >>>>to lock_file<<<<< !!");
    lock_status = lock_file(indicator_daemon_path);
    if (lock_status) {
        LOGE("Watch >>>>DAEMON<<<<< Daed !!");
        int result = binder.get()->transact(code, parcel, NULL, 0);
        remove(observer_self_path);// it`s important ! to prevent from deadlock
//        java_callback(env, jobj, DAEMON_CALLBACK_NAME);
        if (pid > 0) {
            killpg(pid, SIGTERM);
        }
    }
}

JNIEXPORT void JNICALL
Java_com_boolbird_keepalive_NativeKeepAlive_doDaemon(JNIEnv *env, jobject jobj,
                                                             jstring indicatorSelfPath,
                                                             jstring indicatorDaemonPath,
                                                             jstring observerSelfPath,
                                                             jstring observerDaemonPath,
                                                             jint code, jlong parcel_ptr) {
    if (indicatorSelfPath == NULL || indicatorDaemonPath == NULL || observerSelfPath == NULL ||
        observerDaemonPath == NULL) {
        LOGE("parameters cannot be NULL !");
        return;
    }
    if (parcel_ptr == 0) {
        return;
    }

    char *indicator_self_path = (char *) env->GetStringUTFChars(indicatorSelfPath, 0);
    char *indicator_daemon_path = (char *) env->GetStringUTFChars(indicatorDaemonPath, 0);
    char *observer_self_path = (char *) env->GetStringUTFChars(observerSelfPath, 0);
    char *observer_daemon_path = (char *) env->GetStringUTFChars(observerDaemonPath, 0);

    Parcel *parcel = (Parcel *) parcel_ptr;
    size_t data_size = parcel->dataSize();

    int fd[2];
    if (pipe(fd) < 0) {
        LOGE("pipe create error\n");
        return;
    }

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

        close(fd[1]);
        uint8_t data[data_size];
        int result = read(fd[0], data, data_size);
        close(fd[0]);

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
                  observer_self_path_child, observer_daemon_path_child, code, data, data_size);
    }

    close(fd[0]);
    int result = write(fd[1], parcel->data(), data_size);
    LOGD("pipe write result=%d", result);
    close(fd[1]);

    if (waitpid(pid, NULL, 0) != pid)
        LOGE("waitpid error\n");

    LOGD("do_daemon pid=%d ppid=%d", getpid(), getppid());
    do_daemon(env, jobj, indicator_self_path, indicator_daemon_path, observer_self_path,
              observer_daemon_path, code, parcel->data(), data_size);
}
}