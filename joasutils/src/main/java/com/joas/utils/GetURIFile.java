/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 1. 15 오후 3:41
 *
 */

package com.joas.utils;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Async Task to download file from URL
 */
public class GetURIFile extends AsyncTask<String, String, String> {
    public final static String TAG="GetURIFile";
    private static final int BUFFER_SIZE = 4096;

    private String fileName;
    private String m_localPath;
    boolean endFlag = false;
    GetURIFileListener listener;
    /**
     * new GetURIFile(path).execute(url);
     */

    public GetURIFile(String filename, GetURIFileListener getURIFileListener) {
        this.fileName = filename;               //filename : /storage/emulated/0/Updateupdate.zip
        this.listener = getURIFileListener;
    }

    /**
     * Downloading file in background thread
     */
    @Override
    protected String doInBackground(String... uri) {

        //https://ocpp-joas.rnd.starlabs.co.kr/update.zip
//        try {
//            URL url = new URL(uri[0]);
//            URLConnection conn = url.openConnection();
//            conn.setConnectTimeout(1000*10); // 10초 타임아웃
//            conn.setReadTimeout(1000*10); // 10초 타임아웃
//            InputStream inputStream = conn.getInputStream();
//
//            FileOutputStream outputStream = new FileOutputStream(fileName);
//
//            byte[] buffer = new byte[BUFFER_SIZE];
//            int bytesRead = -1;
//            while ((bytesRead = inputStream.read(buffer)) != -1 ) {
//                outputStream.write(buffer, 0, bytesRead);
//                if (isCancelled()) break;
//            }
//
//            outputStream.close();
//            inputStream.close();
//
//            if ( this.listener != null ) this.listener.onGetURIFileFinished();
//        } catch (IOException ex) {
//            //ex.printStackTrace();
//            if ( this.listener != null ) this.listener.onGetURIFileError(ex.toString());
//        }

        URL imgurl;
        int Read;
        int totalReadLen = 0;

        try {
            imgurl = new URL(uri[0]);
            HttpURLConnection conn = (HttpURLConnection) imgurl.openConnection();
//            conn.setConnectTimeout(1000*10); // 10초 타임아웃
//            conn.setReadTimeout(1000*10); // 10초 타임아웃
            int len = conn.getContentLength();
            byte[] tmpByte = new byte[len];
            InputStream is = conn.getInputStream();
            File file = null;
            file = new File(this.fileName);

            FileOutputStream fos = new FileOutputStream(file);
            for (; ; ) {
                Read = is.read(tmpByte);
                if (Read <= 0) {
                    break;
                }
                fos.write(tmpByte, 0, Read);
                totalReadLen += Read;
//                msg_downloadstat = "" + ((int) totalReadLen * 100 / len) + "%(" + "" + len + "/" + "" + totalReadLen + ")";
//                LogWrapper.d(TAG, " " + msg_downloadstat);
            }
            is.close();
            fos.close();
            conn.disconnect();

            if ( this.listener != null ) this.listener.onGetURIFileFinished();
        }
        catch (MalformedURLException e){
            if ( this.listener != null ) this.listener.onGetURIFileError(e.toString());
        }
        catch (IOException e){
            if ( this.listener != null ) this.listener.onGetURIFileError(e.toString());
        }
        catch (Exception e){
            if ( this.listener != null ) this.listener.onGetURIFileError(e.toString());
        }


        return "";
    }
}