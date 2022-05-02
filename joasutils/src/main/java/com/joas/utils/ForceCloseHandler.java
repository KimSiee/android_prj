/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 17 오후 2:28
 *
 */

package com.joas.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ForceCloseHandler implements
        java.lang.Thread.UncaughtExceptionHandler {
    private final Activity myContext;
    private final String LINE_SEPARATOR = "\n";
    String filePath = "/ForceCloseLog/";
    boolean rebootFlag = false;

    public ForceCloseHandler(Activity context, String path, boolean isReboot) {
        myContext = context;
        filePath = path;
        rebootFlag = isReboot;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("************ TIME INFO ************\n\n");
        errorReport.append((new Date()).toString()+"\n\n");

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "FaultLog_" + timeStamp+".txt";

        FileUtil.stringToFile(filePath, filename, errorReport.toString(), false);

        try {
            PowerManager pm = (PowerManager) myContext.getSystemService(Context.POWER_SERVICE);
            if ( rebootFlag  )  pm.reboot("force");
        }
        catch (Exception e ) {
            LogWrapper.e("ForceCloseHandler", "error Reboot:"+e.toString());
        }
    }
}
