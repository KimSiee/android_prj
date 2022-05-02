/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:41
 */

package com.joas.utils;

import android.os.Binder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogWrapper {
    private static final String TAG = "LogWrapper";
    private static final int LOG_FILE_SIZE_LIMIT = 512 * 1024;
    private static final int LOG_FILE_MAX_COUNT = 2;
    private static final String LOG_FILE_NAME = "FileLog%g.txt";
    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final Date date = new Date();
    private static Logger logger;
    private static FileHandler fileHandler;

    public static String lastErrorMessage = "";

    static LogWrapperListener listener;

    static {
        try {
            fileHandler = new FileHandler(Environment.getExternalStorageDirectory()
                    + File.separator +
                    LOG_FILE_NAME, LOG_FILE_SIZE_LIMIT, LOG_FILE_MAX_COUNT, true);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord r) {
                    date.setTime(System.currentTimeMillis());

                    StringBuilder ret = new StringBuilder(80);
                    ret.append(formatter.format(date));
                    ret.append(r.getMessage());
                    return ret.toString();
                }
            });

            logger = Logger.getLogger(LogWrapper.class.getName());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            Log.d(TAG, "init success");
        } catch (IOException e) {

            Log.d(TAG, "init failure:"+e.toString());
        }
    }

    public static void setLogWrapperListener(LogWrapperListener listener) {
        LogWrapper.listener = listener;
    }

    public static void msgToListener(String tag, int level, String msg) {
        try {
            if (listener != null)
                listener.onRecvDebugMsg(new LogWrapperMsg(formatter.format(new Date()), level, tag, msg));
        } catch(Exception e) {
            Log.e(tag, e.toString());
        }
    }

    public static void v(String tag, String msg) {
        if (logger != null) {
            //logger.log(Level.INFO, String.format("V/%s(%d): %s\n", tag, Binder.getCallingPid(), msg));
        }

        try {
            Log.v(tag, msg);
            msgToListener(tag, Log.VERBOSE, msg);
        }
        catch (Exception e){}
    }

    public static void d(String tag, String msg) {
        if (logger != null) {
            //logger.log(Level.DEBUG, String.format("V/%s(%d): %s\n", tag, Binder.getCallingPid(), msg));
        }

        try {
            Log.d(tag, msg);
            msgToListener(tag, Log.DEBUG, msg);
        }
        catch (Exception e){}
    }


    public static void e(String tag, String msg) {
        if (logger != null) {
            //logger.log(Level.WARNING, String.format("V/%s(%d): %s\n", tag, Binder.getCallingPid(), msg));
        }
        try {
            Log.e(tag, msg);

            msgToListener(tag, Log.ERROR, msg);
        }
        catch (Exception e){}

        lastErrorMessage = tag+":"+msg;
    }
}
