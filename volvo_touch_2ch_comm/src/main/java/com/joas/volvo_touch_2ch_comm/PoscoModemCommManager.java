/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 5:02
 *
 */

package com.joas.volvo_touch_2ch_comm;

import android.content.Context;

import com.joas.utils.LogWrapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PoscoModemCommManager extends Thread{

    public static final int RECV_BUFF_MAX = 1024;
//    public static final int INFO_REQ_PERIOD  = 60;        //1분에 한번씩 요청(test)
    public static final int INFO_REQ_PERIOD  = 1800;        //30분에 한번씩 요청(test)
    String modemIP = "";
    int modemPort;
    PoscoModemCommManagerListener listener;

    public Socket modemsock = null;
    //입출력 스트림
    InputStream in = null;
    OutputStream out = null;



    public PoscoModemCommManager(Context context, String ipAddress, int port, PoscoModemCommManagerListener poscoModemCommManagerListener) {
        modemIP = ipAddress;
        modemPort = port;
        listener = poscoModemCommManagerListener;

    }


    public void run() {
        int periodCnt = INFO_REQ_PERIOD;
        while (!isInterrupted()) {
            try {
                periodCnt++;
                if ( periodCnt >= INFO_REQ_PERIOD ) {
                    periodCnt = 0;
                    getModemInfoThread();
                }
            }
            catch(Exception e) {
                LogWrapper.e("PoscoModemComm",e.toString());
            }

            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e){
                LogWrapper.e("PoscoModemComm",e.toString());
            }
        }
    }

    public void getModemInfoThread()
    {
        getMdnInfo();
        getRssiInfo();
    }

    public void getMdnInfo()
    {
        String atcmd = "AT+CNUM";
        int recvSize = -1;
        byte[] recvBuf = new byte[RECV_BUFF_MAX];

        try
        {
            //socket 생성성
           modemsock = new Socket(modemIP, modemPort);
            in = modemsock.getInputStream();
            out = modemsock.getOutputStream();

            if(modemsock.isConnected()) {
                //at command 전송
                out.write(atcmd.getBytes());
                out.flush();

                Thread.sleep(10);

                recvSize = in.read(recvBuf);

                if(recvSize < 0) return;
                else
                {
                    String data = new String(recvBuf,0,recvSize);
                    if(listener!=null)listener.onRecvModemMDNInfo(data);
                }

                Disconnect();
            }
        }
        catch (Exception e)
        {
            LogWrapper.e("PoscoModemComm",e.toString());
        }
    }
    public void getRssiInfo()
    {
        String atcmd = "AT$DBGSCRN?";
        int recvSize = -1;
        byte[] recvBuf = new byte[RECV_BUFF_MAX];

        try
        {
            //socket 생성성
            modemsock = new Socket(modemIP, modemPort);
            in = modemsock.getInputStream();
            out = modemsock.getOutputStream();

            if(modemsock.isConnected()) {
                //at command 전송
                out.write(atcmd.getBytes());
                out.flush();

                Thread.sleep(10);

                recvSize = in.read(recvBuf);

                if(recvSize < 0) return;
                else
                {
                    String data = new String(recvBuf,0,recvSize);
                    if(listener!=null)listener.onRecvModemRSSiInfo(data);
                }

                Disconnect();
            }
        }
        catch (Exception e)
        {
            LogWrapper.e("PoscoModemComm",e.toString());
        }
    }


    public void Disconnect() {
        try {
            if (modemsock != null) modemsock.close();
            if(in != null) in.close();
            if(out != null) out.close();

            modemsock = null;
            in = null;
            out = null;

        }catch (Exception e)
        {
            LogWrapper.e("PoscoModemComm",e.toString());
        }
    }

}
