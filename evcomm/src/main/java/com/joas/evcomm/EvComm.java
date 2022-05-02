/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 21 오전 9:34
 *
 */

package com.joas.evcomm;

import android.util.Log;

import com.joas.utils.ByteUtil;
import com.joas.utils.CRC16;
import com.joas.utils.LogWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EvComm extends Thread {
    public static final String TAG = "EvComm";

    public static final int RECV_BUFF_MAX = 20*1024;
    public static final int SOCKET_CONNECT_TIMEOUT = 5000;

    String serverIP = "";
    int serverPort = 0;

    public Socket socket;

    boolean isEndFlag = false;
    boolean isConnected = false;

    OutputStream outputStream;
    InputStream inputStream;

    byte[] recvBuf;

    EvCommListener evCommListener = null;
    EvCommMonitorListener evCommMonitorListener = null;

    String stationID;
    String chargerID;
    int cpType = 0;

    int sendPacketSeq = 0;

    int socket_connect_retry_Timeout = 10*1000;     //add by si.-200812 : 서버접속 실패시 재시도 간격 타이머(default : 10 sec)

    public EvComm() {

    }

    public EvComm(String svrip, int svrport, String sid, String cid, int type) {
        initEvComm(svrip, svrport, sid, cid, type, socket_connect_retry_Timeout);
    }

    /**
     *
     * @param reconn_timer
     * add by si. 200812 - @param reconn_timer 추가 : 서버 재접속 시도 간격 타이머 설정값
     */
    public EvComm(String svrip, int svrport, String sid, String cid, int type, int reconn_timer) {
        initEvComm(svrip, svrport, sid, cid, type, reconn_timer);
    }

    void initEvComm(String svrip, int svrport, String sid, String cid, int type, int reconn_timer) {
        serverIP = svrip;
        serverPort = svrport;
        stationID = sid;
        chargerID = cid;
        cpType = type;

        isEndFlag = false;
        isConnected = false;
        sendPacketSeq = 0;
        recvBuf = new byte[RECV_BUFF_MAX];

        socket_connect_retry_Timeout = reconn_timer;        //add by si. 200812
    }

    public void setListener(EvCommListener listener) {
        evCommListener = listener;
    }
    public void setMonitorListener(EvCommMonitorListener listener) {
        evCommMonitorListener = listener;
    }

    public void setServerAddr(String ip, int port) {
        serverIP = ip;
        serverPort = port;
    }

    public void setID(String sid, String cid) {
        stationID = sid;
        chargerID = cid;
    }

    public void setCpType(int type) {
        cpType = type;
    }

    public void startComm() {
        isEndFlag = false;
        isConnected = false;
        this.start();
    }

    public void stopComm() {
        isEndFlag = true;
        this.interrupt();
    }

    public boolean getConnectStatus() {
        return isConnected;
    }

    @Override
    public void run() {
        // 시작 5초후에 접속 시도
        try {
//            Thread.sleep(5000); // 5 sec Sleep
            Thread.sleep(10000); // 10 sec Sleep - edit by si.
        }
        catch(Exception e)
        {}

        while ( !isEndFlag && !isInterrupted()) {
            try {
                try {
                    LogWrapper.v(TAG, "Connect to " + serverIP + ":" + serverPort);
                    SocketAddress socketAddress = new InetSocketAddress(serverIP, serverPort);
                    socket = new Socket();
//                    socket.setKeepAlive(false);
                    // 서버로 연결을 시도한다.
                    socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                } catch (Exception sockEx) {
                    // 연결 실패. timeout 후 재접속
                    LogWrapper.e(TAG, "Connect Fail. Retry after" + Integer.toString(socket_connect_retry_Timeout / 1000) + " sec");
                    Thread.sleep(socket_connect_retry_Timeout); // Sleep
//                    LogWrapper.e(TAG, "Connect Fail. Retry after 5 sec");
//                    Thread.sleep(5000); // 5 sec Sleep
                    continue;
                }

                // 접속이 성공적으로 이루어지면 이벤트 발생
                if (evCommListener != null) evCommListener.onEvCommConnected();

                // get the I/O streams for the socket.
                try {
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                } catch (IOException ioEx) {
                    LogWrapper.e(TAG, "get stream fail");
                    Thread.sleep(socket_connect_retry_Timeout); // Sleep
//                    Thread.sleep(5000); // 5 sec Sleep
                    socket.close();
                    continue;
                }

                isConnected = true; // 소켓 연결 성공
                RecvProcess();

                // To.. Do.. Notify Event
                // Disconnect Event
                if (evCommListener != null) evCommListener.onEvCommDisconnected();

                isConnected = false; // 소켓 연결 끊어짐
                if (!socket.isClosed()) socket.close();
                LogWrapper.v(TAG, "Disconnected..Retry after" + Integer.toString(socket_connect_retry_Timeout / 1000) + " sec");

                //add by si. 200812 - 서버와 접속 끊긴 후 timeout 시간동안 대기 후 재접속 시도
                try {
                    Thread.sleep(socket_connect_retry_Timeout);
                } catch (Exception e) {
                }

            }
            catch(Exception ex) {
                LogWrapper.e(TAG, "run() ex:"+ex.toString()+", stack:"+Log.getStackTraceString(ex));
                try {
                    Thread.sleep(10);
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

        while (  !isEndFlag && !isInterrupted()) {
            int recvSize = -1;

            if (bufCnt >= RECV_BUFF_MAX) bufCnt = 0; // 버퍼가 넘어가는 경우에(Overflow) 버퍼를 버린다

            try {
                recvSize = inputStream.read(recvBuf, bufCnt, RECV_BUFF_MAX-bufCnt);
            }
            catch(Exception ex){
                LogWrapper.e(TAG, "RecvProcess() read:"+ex.toString());
                // Exception 발생시 종료
                return;
//                continue;       //recv 예외발생시 소켓 끊지 않고 계속 진행하도록 수정 - edit by si.200825
            }
            // 소켓으로 받은 크기가 0보다 작으면 끊어짐
            if ( recvSize < 0 ){
                return;
//                continue;       //recv 예외발생시 소켓 끊지 않고 계속 진행하도록 수정 - edit by si.200825
            }

            bufCnt += recvSize;

            // 헤더만큼 읽어드린다.
            if (bufCnt < EvPacket.EV_MSG_HEADER_TOP_SIZE) continue;

            // Header 의 STX를 검사한다. 값이 틀리다면 현재 패킷을 버린다.
            if (recvBuf[0] != EvPacket.EV_MSG_HEADER_STX)
            {
                bufCnt = 0;
                continue;
            }

            int vdSize = ByteUtil.makeWord(recvBuf[EvPacket.EV_MSG_HEADER_ML_POS], recvBuf[EvPacket.EV_MSG_HEADER_ML_POS+1]);
            int packetSize = EvPacket.EV_MSG_HEADER_TOP_SIZE + vdSize + EvPacket.EV_MSG_HEADER_TAIL_SIZE;

            // 패킷 크기만큼 읽어드린다.
            if (bufCnt < packetSize ) continue;

            int crcval = CRC16.calc(CRC16.INITIAL_VALUE, recvBuf, EvPacket.EV_MSG_HEADER_STX_SIZE, packetSize- EvPacket.EV_MSG_HEADER_TAIL_SIZE- EvPacket.EV_MSG_HEADER_STX_SIZE);
            int crcValPacket = ByteUtil.makeWord(recvBuf[packetSize - EvPacket.EV_MSG_HEADER_TAIL_SIZE], recvBuf[packetSize- EvPacket.EV_MSG_HEADER_TAIL_SIZE+1]);

            if ( crcval == crcValPacket ) {
                recvPacketProcess(recvBuf, packetSize);
            }
            else {
                LogWrapper.e(TAG, "rcv Pkt CRC Err:"+ByteUtil.byteArrayToHexString(recvBuf, 0, packetSize)+", vdSize:"+vdSize);
            }

            // Memory Move
            System.arraycopy(recvBuf, packetSize, recvBuf, 0, bufCnt - packetSize);
            bufCnt -= packetSize;

            try {
                Thread.sleep(10);
            }
            catch(Exception e) {}
        }
    }

    void recvPacketProcess(byte[] buf, int size) {
        try {
            if (evCommListener != null) evCommListener.onJoasCommRXRawData(buf, size);
        }
        catch (Exception e) {}

        // Parse and Notify Event
        EvPacket packet = null;
        try {
            packet = new EvPacket(buf, 0, size);
            packet.isRxPacket = true;
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "Parse Err() e:"+ex.toString());
        }

        if ( packet != null ) {
            if ( evCommMonitorListener != null ) evCommMonitorListener.onRecvEvPacket(packet);
            if ( evCommListener != null ) evCommListener.onTimeUpdate(packet.sendDate);
            if ( evCommListener != null ) evCommListener.onJoasCommPacketRecv(packet);
        }
    }

    public void fillPacketInfo(EvPacket packet) {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        packet.sendDate = dateFormat.format(now);

        packet.stationId = stationID;
        packet.chargerId = chargerID;
        packet.type = cpType;
    }

    void sendPacket(EvPacket packet) {
        if ( evCommMonitorListener != null ) evCommMonitorListener.onTransEvPacket(packet);
        try {
            byte[] rawData = packet.encode();
            outputStream.write(rawData );
            outputStream.flush();

            try {
                if (evCommListener != null) evCommListener.onJoasCommTXRawData(rawData, rawData.length);
            }
            catch (Exception e) {}
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "sendPacket:" + e.toString()+", "+ Log.getStackTraceString(e));
        }
    }
}
