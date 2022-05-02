/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 9. 21 오전 9:17
 *
 */

package com.joas.utils;

import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.TimerTask;

/**
 * Created by user on 2018-09-20.
 */

public class EMINetworkTest extends Thread {
    public static final String TAG = "EMINetworkTest";

    public static final int DEFAULT_PORT = 9494;
    public static final int RECV_BUFF_MAX = 8096;
    public static final int SOCKET_CONNECT_TIMEOUT = 5000;

    String serverIP = "192.168.0.1";
    int serverPort = DEFAULT_PORT;

    Socket socket;

    boolean isEndFlag = false;
    boolean isConnected = false;

    OutputStream outputStream;
    InputStream inputStream;

    byte[] recvBuf = new byte[RECV_BUFF_MAX];

    public EMINetworkTest(String svrip, int svrport)  {
        serverIP = svrip;
        serverPort = svrport;
    }
    public void startComm() {
        isEndFlag = false;
        isConnected = false;
        this.start();
    }
    public void stopComm() {
        isEndFlag = true;
        isConnected = false;

        this.interrupt();
    }

    @Override
    public void run() {
        // 시작 5초후에 접속 시도
        try {
            Thread.sleep(5000); // 5 sec Sleep
        }
        catch(Exception e)
        {}

        while ( !isEndFlag ) {
            try {
                try {
                    LogWrapper.v(TAG, "서버로 연결 시도중:"+serverIP+":"+serverPort);
                    SocketAddress socketAddress = new InetSocketAddress(serverIP, serverPort);
                    socket = new Socket();

                    // 서버로 연결을 시도한다.
                    socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                } catch (Exception sockEx) {
                    // 연결 실패. 5초후 재접속
                    LogWrapper.e(TAG, "서버 연결 실패. 5초후 재시도...");
                    Thread.sleep(5000); // 5 sec Sleep
                    continue;
                }

                // 접속이 성공적으로 이루어지면 이벤트 발생
                isConnected = true; // 소켓 연결 성공

                // get the I/O streams for the socket.
                try {
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                } catch (IOException ioEx) {
                    LogWrapper.e(TAG, "get stream fail");
                    Thread.sleep(5000); // 5 sec Sleep
                    continue;
                }
                LogWrapper.d(TAG, "서버 연결 성공 !!");

                RecvProcess();

                isConnected = false; // 소켓 연결 끊어짐
                if ( !socket.isClosed() ) socket.close();
                LogWrapper.v(TAG, "서버 연결 끊어짐. 5초후 재연결..");
                Thread.sleep(5000); // 5 sec Sleep
            }
            catch(Exception ex) {
                LogWrapper.e(TAG, "run() ex:"+ex.toString());
                try {
                    Thread.sleep(5000);
                }
                catch(Exception e){
                    //Nothing..
                }
            }
        }
    }

    public void disconnect() {
        try {
            if ( !socket.isClosed() ) socket.close();
        }
        catch (Exception ex) {}
    }

    void RecvProcess() {
        int bufCnt = 0;

        while ( !isEndFlag ) {
            int recvSize = -1;

            try {
                recvSize = inputStream.read(recvBuf, bufCnt, RECV_BUFF_MAX);

                //ECHO Packet
                outputStream.write(recvBuf, 0, recvSize);
                outputStream.flush();
            }
            catch(Exception ex){
                LogWrapper.e(TAG, "RecvProcess() read:"+ex.toString());
                // Exception 발생시 종료
                return;
            }
        }
    }
}
