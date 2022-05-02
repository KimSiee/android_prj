/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 22 오후 1:49
 *
 */

package com.joas.utils;

import android.util.Log;

public class LogWrapperMsg {
    public String time;
    public int level;
    public String TAG;
    public String msg;

    public LogWrapperMsg(String datetime, int level, String tag, String content) {
        this.time = datetime;
        this.level = level;
        this.TAG = tag;
        this.msg = content;
    }

    public static String getLevelString(int level) {
        switch (level) {
            case Log.VERBOSE: return "V";
            case Log.DEBUG: return "D";
            case Log.ERROR: return "E";
        }
        return "N";
    }
}
