/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 23 오후 2:01
 *
 */

package com.joas.utils;


public class WatchDogTimer {

    public static final int WATCHDOG_MAX_UPDATE_TIMEOUT = 300; // 5분

    public boolean isStarted = false;

    public WatchDogTimer() {

    }

    public void openAndStart(int timeout) {
        open(timeout);
        start();
        isStarted = true;
    }

    public void stopAndClose() {
        close();
        stop();
        isStarted = false;
    }

    public native void open(int timeout); // 자동시작됨.
    public native void start(); // Stop이후에 재시작할때 필요
    public native void stop();
    public native void update();
    public native void close();
    public native void hdmiOn();
    public native void hdmiOff();

    static {
        System.loadLibrary("watchdog");
    }
}
