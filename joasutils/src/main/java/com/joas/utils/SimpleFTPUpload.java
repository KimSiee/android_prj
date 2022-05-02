/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 1. 25 오전 11:36
 *
 */

package com.joas.utils;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class SimpleFTPUpload extends AsyncTask<String, String, String> {
    public static final String TAG = "SimpleFTPUpload";
    public static final int BUFFER_SIZE = 4096;

    String ftpUrl = "ftp://%s:%s@%s/%s;type=i";
    String filePath;
    SimpleFTPUploadListener simpleFTPUploadListener;

    public SimpleFTPUpload(SimpleFTPUploadListener listener) {
        simpleFTPUploadListener = listener;
    }

    @Override
    protected String doInBackground(String... params) {

        ftpUrl = params[0];
        filePath = params[1];
        String fileName = params[2];

        FTPClient con = null;

        LogWrapper.d(TAG, "Upload URL: " + ftpUrl);
        try {
            URL url = new URL(ftpUrl);
            con = new FTPClient();
            con.connect(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort());

            String[] userInfo = url.getUserInfo().split(":");

            if (con.login(userInfo[0], userInfo[1]) ) {
                con.enterLocalPassiveMode(); // important!
                con.setFileType(FTP.BINARY_FILE_TYPE);
                if ( con.changeWorkingDirectory(url.getPath()) ) {
                    FileInputStream in = new FileInputStream(new File(filePath));
                    boolean result = con.storeFile(fileName, in);
                    in.close();

                    LogWrapper.d(TAG, "File uploaded");
                    con.logout();

                    simpleFTPUploadListener.onSimpleFTPUploadFinished();
                }
                else {
                    LogWrapper.d(TAG, "FTP Change Directory Failed");
                    simpleFTPUploadListener.onSimpleFTPUploadFileError("Change Directory Fail");
                    con.logout();
                    return "";
                }
            }
            else {
                LogWrapper.d(TAG, "FTP Login Failed");
                simpleFTPUploadListener.onSimpleFTPUploadFileError("FTP Login Fail");
                return "";
            }
        } catch (Exception ex) {
            LogWrapper.d(TAG, "File uploaded Error:"+ex.toString());
            simpleFTPUploadListener.onSimpleFTPUploadFileError("Upload Fail Error");
            return "";
        }

        return "";
    }
}
