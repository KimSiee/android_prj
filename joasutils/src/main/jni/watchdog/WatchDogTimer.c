/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 23 오전 9:28
 *
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <asm/types.h>
#include <linux/watchdog.h>

#include "WatchDogTimer.h"

#include "android/log.h"

static const char *TAG="WatchDog";

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

#define WDT_DEV_NAME "/dev/watchdog"

static int wdtFd = -1;
static int timeoutVal = 16;

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_open(JNIEnv *env, jobject obj, jint timeout)
{
    timeoutVal = (int)timeout;
    if ( wdtFd > 0 ) return;
    wdtFd = open(WDT_DEV_NAME, O_RDWR);
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_start(JNIEnv *env, jobject obj)
{
    int option = WDIOS_ENABLECARD;

    if (wdtFd > 0 )
    {
        ioctl(wdtFd , WDIOC_SETOPTIONS, &option);
        ioctl(wdtFd, WDIOC_SETTIMEOUT, &timeoutVal);
    }
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_stop(JNIEnv *env, jobject obj)
{
    int option = WDIOS_DISABLECARD;
    if (wdtFd > 0 )
    {
        ioctl(wdtFd , WDIOC_SETOPTIONS, &option);
        ioctl(wdtFd, WDIOC_SETTIMEOUT, &timeoutVal);
    }
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_update(JNIEnv *env, jobject obj)
{
    if (wdtFd > 0 )
    {
        ioctl(wdtFd , WDIOC_KEEPALIVE);
    }
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_close(JNIEnv *env, jobject obj)
{
    if (wdtFd > 0 )
    {
        close(wdtFd);
        wdtFd = -1;
    }
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_hdmiOff(JNIEnv *env, jobject obj)
{
    int option = 0x1000;
    if (wdtFd > 0 )
    {
        ioctl(wdtFd , WDIOC_SETOPTIONS, &option);
    }
}

JNIEXPORT void JNICALL Java_com_joas_utils_WatchDogTimer_hdmiOn(JNIEnv *env, jobject obj)
{
    int option = 0x2000;
    if (wdtFd > 0 )
    {
        ioctl(wdtFd , WDIOC_SETOPTIONS, &option);
    }
}
