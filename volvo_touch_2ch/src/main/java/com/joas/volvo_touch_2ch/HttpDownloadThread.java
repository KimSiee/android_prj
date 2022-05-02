/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 8. 12. 오후 2:40
 *
 */

package com.joas.volvo_touch_2ch;

import android.util.Log;

import com.joas.utils.LogWrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpDownloadThread extends Thread{
    String m_serverURL;
    String m_localPath;
    byte dstFlag;
    UIFlowManager flowManager;
    String msg_downloadstat;
    public static final String TAG = "HttpDownloadThread";

    HttpDownloadThread(String serverpath, String localpath, byte destinationFlag, UIFlowManager uiFlowManager){
        m_serverURL = serverpath;
        m_localPath = localpath;
        dstFlag = destinationFlag;
        flowManager = uiFlowManager;
    }

    @Override
    public void run() {
        super.run();

        URL imgurl;
        int Read;
        int totalReadLen = 0;

        try {
            imgurl = new URL(m_serverURL);
            HttpURLConnection conn = (HttpURLConnection) imgurl.openConnection();
            conn.setRequestProperty("Accept-Encoding", "identity");

            Log.i("Net", "length = " + conn.getContentLength());
            Log.i("Net", "respCode = " + conn.getResponseCode());
            Log.i("Net", "contentType = " + conn.getContentType());
            Log.i("Net", "content = " + conn.getContent());
            int len = conn.getContentLength();
            byte[] tmpByte = new byte[len];
            InputStream is = conn.getInputStream();
            File file = null;
            if(dstFlag == 0x01) file = new File(m_localPath + "/update.zip");
            else if(dstFlag == 0x05) file = new File(m_localPath + "/psmember.txt");
            else if(dstFlag == 0x06) file = new File(m_localPath + "/psmemadd.txt");
            else if(dstFlag == 0x07) file = new File(m_localPath + "/psmemdel.txt");

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
        }
        catch (MalformedURLException e){
            LogWrapper.e("ERROR1",e.getMessage());
//            Log.e("ERROR1",e.getMessage());
        }
        catch (IOException e){
            LogWrapper.e("ERROR2",e.getMessage());
//            Log.e("ERROR2",e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e){
            LogWrapper.e("ERROR3",e.getMessage());
        }
        //다운로드 완료되면 처리되는 곳
        flowManager.completeHttpDownload();
    }
}
