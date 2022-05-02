/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:42
 */

package com.joas.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

public class TimeoutTimer extends Timer {
    private long timeout;
    private TimeoutHandler timeoutHandler;
    boolean isTimerRun = false;
    Handler handler;

    public TimeoutTimer(long timeout, TimeoutHandler handler) {
        this.timeout = timeout;
        this.timeoutHandler = handler;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void start() { beginPeriod(); };
    public void startOnce() { beginPeriod(); };
    public void cancel() { end(); };
    public void stop() { end(); };

    public void begin() {
        // 이미 실행중이라면 중복으로 실행하지 않는다.
        if ( isTimerRun ) return;

        isTimerRun = true;
        handler = new Handler(Looper.getMainLooper());
        final Handler runHandler = handler;
        handler.postDelayed(new Runnable() {
            public void run() {
                if (isTimerRun && handler == runHandler) {
                    timeoutHandler.run();
                    end();
                }
            }
        }, timeout);
    }

    // 일정한 주기를 갖는 타이머
    public void beginPeriod() {
        // 이미 실행중이라면 중복으로 실행하지 않는다.
        if ( isTimerRun ) return;

        isTimerRun = true;
        handler = new Handler(Looper.getMainLooper());
        final Handler runHandler = handler;
        handler.postDelayed(new Runnable() {
            public void run() {
                if (isTimerRun && handler == runHandler) {
                    timeoutHandler.run();
                    runHandler.postDelayed(this, timeout);
                }
            }
        }, timeout);
    }

    public void end()
    {
        isTimerRun = false;
    }
}
