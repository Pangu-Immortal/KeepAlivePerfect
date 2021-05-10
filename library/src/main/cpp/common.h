#ifndef KEEPALIVE_COMMON_H
#define KEEPALIVE_COMMON_H

#include <android/log.h>
#include <stdint.h>
#include <sys/types.h>

#define TAG        "KeepAlive"
#define LOGW(...)    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifdef LIB_DEBUG
#define LOGI(...)    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...)    __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define LOGI(...)
#define LOGD(...)
#endif

typedef unsigned short Char16;
typedef unsigned int Char32;
#endif //KEEPALIVE_COMMON_H
