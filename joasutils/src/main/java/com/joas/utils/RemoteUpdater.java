/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 16 오후 2:59
 *
 */

package com.joas.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class RemoteUpdater extends Thread {
    public static final String UPDATE_PATH = "Update";
    String downApkName = "update.apk";
    String downUrl="";
    Activity activityMain;

    String updatePath = Environment.getExternalStorageDirectory()+"/"+UPDATE_PATH;

    public RemoteUpdater(Activity act, String path, String apkName) {
        activityMain = act;
        updatePath = path;
        downApkName = apkName;
    }

    public void DownloadFromHttp(String url) {
        downUrl = url;
        start();
    }

    @Override
    public void run() {
        DownloadFromHttpThread();
    }

    public void DownloadFromHttpThread() {
        File parent = new File(updatePath);

        if(!parent.exists()) {
            parent.mkdirs();
        }
        try {
            URLConnection uc = new URL(downUrl).openConnection();
            InputStream in = uc.getInputStream();

            int len=0, total=0;
            byte[] buf = new byte[1024];

            File apk=new File(updatePath + downApkName);
            if(apk.exists()){
                apk.delete();
            }
            apk.createNewFile();
            // 다운로드
            FileOutputStream fos = new FileOutputStream(apk);

            while((len=in.read(buf, 0, 1024)) != -1) {
                total += len;
                fos.write(buf, 0, len);
            }
            in.close();

            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doUpdateFromApk(String packageName) {
        Intent intent = activityMain.getPackageManager().getLaunchIntentForPackage("com.joas.joasappupdater");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("updateFile", updatePath + "/" + downApkName);
        intent.putExtra("appPackage", packageName);
        activityMain.startActivity(intent);
    }
}
