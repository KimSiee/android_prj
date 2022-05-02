/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 21 오전 9:28
 *
 */

package com.joas.evcomm;

import android.content.Context;

import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public abstract class EvCommManager extends Thread implements EvCommListener {
    public static final String TAG = "EvCommManager";

    protected EvComm evComm;
    protected EvMessageQueue recvMsgQueue;
    protected EvMessageQueue transMsgQueue;

    Thread transMsgThread;
    protected int sendSeqCnt = 0;
    protected EvPacket lastSentPacket = null;

    protected boolean conneted = false;
    protected boolean isAckReceive = false;

    TimeoutTimer timer_b1 = null;
    int timerPeriodSec_b1 = EvCommDefine.DEFAULT_TIMER_PERIOD_SEC_b1;

    protected boolean bEndFlag = false;
    Context baseContext;
    EVCommDbOpenHelper dbHelper;
    protected NonTransPacketRecord nonTransPacketRecord;
    boolean useNontransPacketRecord = false;

    /**
     * 환경부 프로토콜 초기화(민수용, 환경부)
     * @param svrip 서버 주소
     * @param svrport 서버 포트
     * @param sid 충전소 ID
     * @param cid 충전기 ID
     * @param type 충전기 Type
     *
     * @param timeout 서버 재접속 대기 시간 - add by si.200812
     */
    public EvCommManager(Context context, String svrip, int svrport, String sid, String cid, int type, String basePath,int timeout) {
        initManager(context, svrip, svrport, sid, cid, type, basePath, timeout);
    }

    public EvCommManager(Context context, String svrip, int svrport, String sid, String cid, int type, String basePath) {
        initManager(context, svrip, svrport, sid, cid, type, basePath, 10*1000);
    }

    void initManager(Context context, String svrip, int svrport, String sid, String cid, int type, String basePath,int timeout) {
        baseContext = context;
        dbHelper = new EVCommDbOpenHelper(context, basePath);
        dbHelper.open();

        nonTransPacketRecord = new NonTransPacketRecord(dbHelper, basePath);

        recvMsgQueue = new EvMessageQueue();
        transMsgQueue = new EvMessageQueue();

        evComm = new EvComm(svrip, svrport, sid, cid, type, timeout);       //add by si. 200812(timeout)
        evComm.setListener(this);

        evComm.startComm();

        this.start();

        transMsgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                transMsgProcess();
            }
        });

        transMsgThread.start();
    }

    public void setTimerPeriod_b1(int sec) {
        timerPeriodSec_b1 = sec;
    }

    public void stopManager() {
        evComm.stopComm();
        bEndFlag = true;
        this.interrupt();
        transMsgThread.interrupt();
    }

    public void setMonitorListener(EvCommMonitorListener listener) {
        evComm.setMonitorListener(listener);
    }

    public void setCommSetting( String svrip, int svrport, String sid, String cid, int type ) {
        evComm.setServerAddr(svrip, svrport);
        evComm.setID(sid, cid);
        evComm.setCpType(type);
        evComm.disconnect();
    }

    public boolean isConneted() { return conneted; }

    public void setUseNontransPacketRecord(boolean tf) { useNontransPacketRecord = tf;}

    @Override
    public void run() {
        while (!bEndFlag && !isInterrupted()) {
            EvPacket packet = recvMsgQueue.pop();
            // 더이상 처리할 메시지가 없으면 sleep
            if (packet == null) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
            } else {
                if ( packet.isRequest ) processRequestMsg(packet);
                else {
                    if ( lastSentPacket != null ) {
                        if (compareAckSeq(lastSentPacket, packet)){
                            isAckReceive = true;

                            processNonTransPacketAck(lastSentPacket);
                        }
                    }

                    processResponseMsg(packet);
                }
            }
        }
    }

    protected boolean compareAckSeq(EvPacket lastPacket, EvPacket newPacket) {
        return (lastPacket.seq == newPacket.seq);
    }

    boolean waitAckAndRetry() {
        int retryCnt = 0;
        int timerCnt = 0;

        while ( retryCnt < EvCommDefine.COMM_PACKET_RETRY_MAX_CNT  ) {
            if ( isAckReceive == true ) return true;
            else {
                timerCnt++;
                if ( retryCnt == 0 && timerCnt > (EvCommDefine.COMM_WAIT_ACK_FIRST_TIMEOUT_SEC*100) ) {
                    retryCnt++;
                    evComm.sendPacket(lastSentPacket);
                    timerCnt = 0;
                }
                else if ( retryCnt > 0 && timerCnt > (EvCommDefine.COMM_WAIT_ACK_SECOND_TIMEOUT_SEC*100) ) {
                    retryCnt++;
                    if ( retryCnt >= EvCommDefine.COMM_PACKET_RETRY_MAX_CNT ) {
                        return false;
                    }else {
                        evComm.sendPacket(lastSentPacket);
                        timerCnt = 0;
                    }
                }
            }
            try {
                Thread.sleep(10);
            }
            catch (Exception ex) {}
        }

        return false;
    }

    // To..Do..Transaction 처리
    public void transMsgProcess() {
        while (!bEndFlag && !isInterrupted()) {
            try {
                if (evComm.getConnectStatus() == false) {
                    Thread.sleep(10);
                    continue;
                }

                EvPacket packet;
                synchronized (transMsgQueue) {
                    packet = transMsgQueue.top();
                }
                // 더이상 처리할 메시지가 없으면 sleep
                if (packet == null) {
                    Thread.sleep(10);
                } else {
                    //1. 연결되었는지 검사
                    if (evComm.getConnectStatus() == true) {
                        isAckReceive = false;
                        evComm.sendPacket(packet);
                        lastSentPacket = packet;
                        // Wait Ack Message...(first 30 sec. later(retry) 15 sec)
                        if ( waitAckAndRetry() ) {
                            //ACK 처리가 되면...
                            try {
                                synchronized (transMsgQueue) {
                                    transMsgQueue.pop();
                                }
                            }
                            catch (Exception e) {
                                LogWrapper.e(TAG, "transMsgQueue.pop:"+e.toString());
                            }
                        }
                        else {
                            // 패킷 삭제함. // 미전송 처리는 각각 프로그램에서 처리함.
                            transMsgQueue.pop();

                            // 통신 재연결
                            evComm.disconnect();
                        }
                    }

                    Thread.sleep(10);
                }
            }
            catch(Exception ex) {
                LogWrapper.e(TAG, "transMsg Ex:"+ex.toString());
            }
        }
    }

    protected void processNonTransPacketAck(EvPacket packet) {
        if ( useNontransPacketRecord ) {
            if (isSaveInfoPacket(packet)) {
                nonTransPacketRecord.removeRecord(packet.getUniqueId());
            }
        }
    }

    //Need Override
    public abstract void processRequestMsg(EvPacket packet);
    public abstract void processResponseMsg(EvPacket packet);

    public void startTimer_b1() {
        if ( timer_b1 != null ) timer_b1.cancel();

        timer_b1  = new TimeoutTimer(timerPeriodSec_b1*1000, new TimeoutHandler() {
            @Override
            public void run() {
                onSendStatusPeriod();
                timer_b1.setTimeout(timerPeriodSec_b1*1000);
            }
        });

        timer_b1.start();
    }

    public void cancelTimer_b1() {
        if ( timer_b1 != null ) timer_b1.cancel();
    }

    //====================================================
    // Send Packet
    //====================================================
    protected void sendReponse(EvPacket lastPacket, byte[] vdData) {
        sendReponse(lastPacket, vdData, vdData.length);
    }

    protected void sendReponse(EvPacket lastPacket, byte[] vdData, int vdSize) {
        EvPacket ackPacket = new EvPacket();

        evComm.fillPacketInfo(ackPacket);
        ackPacket.seq = lastPacket.seq;
        ackPacket.vd = vdData;
        ackPacket.ml = vdSize;
        ackPacket.ins = new StringBuilder(lastPacket.ins).reverse().toString();

        evComm.sendPacket(ackPacket);
    }

    protected void sendRequest(EvPacket packet) {
        if ( conneted ) {
            transMsgQueue.add(packet);
        }
    }

    protected void sendRequest(String ins, byte[] vdData) {
        sendRequest(ins, vdData, vdData.length, false);
    }

    protected void increaseSendSeq() {
        sendSeqCnt++;

        if ( sendSeqCnt >= 65536 ) sendSeqCnt = 0;
    }

    /**
     *  패킷 전송 요청을 받아서 큐에 저장한다.
     * @param ins 명령
     * @param vdData 데이터
     * @param vdSize 크기
     * @param isNotSend 보내지 않고 큐에 저장할지 여부 결정(미전송 전문관련 기능 추가)
     */
    protected void sendRequest(String ins, byte[] vdData, int vdSize, boolean isNotSend) {
        EvPacket packet = new EvPacket();

        evComm.fillPacketInfo(packet);
        packet.seq = sendSeqCnt;
        packet.ins = ins;
        packet.vd = vdData;
        packet.ml = vdSize;

        increaseSendSeq();

        // d1, f1, g1 인 경우에 미전송 전문에 저장함
        if ( useNontransPacketRecord ) {
            if (isSaveInfoPacket(packet)) nonTransPacketRecord.addRecord(packet);
        }

        if ( isNotSend == false ) sendRequest(packet);
    }

    public boolean isSaveInfoPacket(EvPacket evPacket) {
        if ( evPacket.ins.equals("d1") || evPacket.ins.equals("f1") || evPacket.ins.equals("g1") ) {
            return true;
        }
        return false;
    }

    // 상속 필요
    public abstract void onSendStatusPeriod();
    public abstract void onSystemTimeUpdate(Date d);
    public abstract void onCommConnected();
    public abstract void onCommDisconnected();

    //====================================================
    // Joas Comm 이벤트 처리
    //====================================================

    @Override
    public void onJoasCommPacketRecv(EvPacket packet) {
        recvMsgQueue.add(packet);
    }

    /**
     * 서버에서 받은 시간을 비교하여 설정한다.
     * @param strTime
     */
    @Override
    public void onTimeUpdate(String strTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            Date d = sdf.parse(strTime);
            onSystemTimeUpdate(d);
        } catch (Exception ex) {
            LogWrapper.e(TAG, "onTimeUpdate Parse Err:"+strTime+", e:"+ ex.toString());
        }
    }

    @Override
    public void onEvCommConnected() {
        conneted = true;

        onCommConnected();

        // 연결되었을 때 b1타이머 시작.
        startTimer_b1();
    }

    @Override
    public void onEvCommDisconnected() {
        conneted = false;
        cancelTimer_b1();
        onCommDisconnected();

        synchronized (transMsgQueue) {
            transMsgQueue.clear();
        }
    }
}
