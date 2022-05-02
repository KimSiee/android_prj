/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 12 오전 10:20
 *
 */

package com.joas.hw.payment.tl3500s;

import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.SerialPort;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TL3500S extends Thread {
    static final String TAG = "TL3500S";
    static final String TERMID = "KIOSK1114915545";

    public String tlserStatus;
    public String tlserStatusText = "";

    public enum State {
        None,
        Ready,
        WaitingAck
    }

    public enum ResponseType {
        Check,
        Pay,
        CancelPay,
        Error,
        Event,
        Search,          //si.add
        GetVersion,
        GetConfig
    }

    static final int RECV_BUF_SIZE = 1024;
    static final int SEND_BUF_SIZE = 512;
    static final int MAX_RETRY_COUNT = 3;

    // ===========================================
    // Callback Function
    TL3500SListener listener = null;
    // ===========================================

    SerialPort serialPort;
    byte[] recvData = new byte[RECV_BUF_SIZE];
    byte[] sendData = new byte[SEND_BUF_SIZE];

    int recvPos = 0;
    final Object lockSend = new Object();

    // ===========================================
    // PrePay and LastPay Save
    public boolean[] isPrePayReq = {false, false};
    public boolean isprepayFlag = false;

    HashMap<String, String>[] prePayInfo;
    HashMap<String, String>[] lastPayInfo;

    //si.20171113 add
    public Map<String, String> prePayInfo_sub = null;
    // ===========================================

    State curState = State.None;

    // ===========================================
    // Retry Packet
    TimeoutTimer retryTimer = null;
    TL3500SPkt retryPacket = null;
    // ===========================================

    boolean isOpened = false;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;
    private boolean bEndFlag = false;

    int lastCh = 0;

    public TL3500S(int maxCh, String serialDev) {
        prePayInfo = new HashMap[maxCh];
        lastPayInfo = new HashMap[maxCh];

        for ( int i=0; i<maxCh; i++) {
            prePayInfo[i] = new HashMap<>();
            lastPayInfo[i] = new HashMap<>();
        }

        try {
            serialPort = new SerialPort(new File(serialDev), 115200, 0);

            if ( serialPort == null ) {
                LogWrapper.v("MainAct", "SerialPort Open Fail!");
                return;
            }

            serialInputStream = serialPort.getInputStream();
            serialOutputStream = serialPort.getOutputStream();

            isOpened = true;

            setState(State.Ready);
        }
        catch  (Exception e) {
            LogWrapper.e(TAG, e.getMessage());
        }

        retryTimer = new TimeoutTimer(3000, new TimeoutHandler() {
            @Override
            public void run() {
                retrySendPktTimeout();
            }
        });
    }

    public void setListener(TL3500SListener tl3500SListener) { this.listener = tl3500SListener; }

    public void stopThread() {
        bEndFlag = true;
        interrupt();
    }

    @Override
    public void run() {
        int timeoutCnt = 0;

        while ( !bEndFlag && !Thread.currentThread().isInterrupted()) {
            try {
                int readLen = serialInputStream.available();

                //DbgLog("RAW READ:" + readLen);

                if ((readLen + recvPos) > RECV_BUF_SIZE) {
                    //DbgLog("Error Buff!!");
                    tlserStatus = "Error Buff!!!";

                    // 예외처리가 필요함..// Data OverFlow
                    recvPos = 0; // 데이터 초기화

                    continue;
                }

                if ( readLen == 0 ) {
                    Thread.sleep(10);
                    continue;
                }

                // first, Push data to readData Buffer
                int readSize = serialInputStream.read(recvData, recvPos, readLen );
                recvPos += readSize;

                String hexData = ByteUtil.byteArrayToHexString(recvData, 0, recvPos);  //헥사코드로 보임
                dbgLog("<< RecvData: " + hexData);

                do {
                    // Find STX
                    while (recvData[0] != TL3500SPkt.STX && recvPos > 0) {
                        if (recvData[0] == TL3500SPkt.ACK) {
                            onAck(); // Ack 처리
                        }

                        if (recvPos > 1) System.arraycopy(recvData, 1, recvData, 0, recvPos - 1);
                        recvPos--;
                    }

                    // Find STX and Header Size
                    if (recvData[0] == TL3500SPkt.STX && recvPos > TL3500SPkt.HEADER_SIZE) {

                        int dataLen = ByteUtil.makeWord(recvData[TL3500SPkt.HEADER_SIZE - 1], recvData[TL3500SPkt.HEADER_SIZE - 2]);
                        int totalSize = dataLen + TL3500SPkt.HEADER_SIZE + 2; // 2(ETX+BCC)
                        if (recvPos >= totalSize) {
                            checkData(recvData, dataLen, totalSize);
                            if (recvPos > totalSize)
                                System.arraycopy(recvData, totalSize, recvData, 0, recvPos - totalSize);
                            recvPos -= totalSize;
                        } else {
                            break;
                        }
                    }
                } while (recvPos > TL3500SPkt.HEADER_SIZE);
            }
            catch (Exception e) {
                LogWrapper.e(TAG, "run():"+e.getMessage());
                recvPos = 0; // 데이터 초기화

                try { Thread.sleep(10); } catch (Exception ex){}
            }
        }
    }

    //ACK, 및 NACK를 보낼때 사용한다.
    private void sendOneCommand(byte cmd)
    {
        byte[] buff = new byte[1];
        buff[0] = cmd;

        sendToSerial(buff, 0, 1);

        if (cmd == TL3500SPkt.NACK)
        {
            dbgLog(">>SendNack>");
        }
        else if (cmd == TL3500SPkt.ACK)
        {
            dbgLog(">>SendACK>");
        }
    }

    // 데이터가 올바른지 검사한다. 올바르지 않다면 NACK를 보내고, 맞으면 ACK를 전송한다.
    private void checkData(byte[] pkt, int dataSize, int size)
    {
        boolean isAckNakSend = true;

        String hexData = ByteUtil.byteArrayToHexString(pkt, 0, size);  //헥사코드로 보임
        dbgLog("<< checkData: " + hexData);

        if (pkt[31] == 0x40) // Evnet
        {
            isAckNakSend = false;
        }

        // Check ETX 
        if (pkt[size - 2] != TL3500SPkt.ETX)
        {
            if (isAckNakSend) sendOneCommand(TL3500SPkt.NACK); // Send NACK
            return; // Packet Error
        }

        // Check BCC(XOR)

        byte xor = 0;
        for (int i = 0; i < size - 1; i++)
        {
            xor ^= pkt[i];
        }

        if (xor != pkt[size - 1])
        {
            if (isAckNakSend) sendOneCommand(TL3500SPkt.NACK); // Send NACK
            return; // Packet Error
        }

        // Send ACK
        if (isAckNakSend) sendOneCommand(TL3500SPkt.ACK);

        // Packet OK!!
        parsePacket(pkt, dataSize, size);
    }

    // TL3500SPkt Object를 만들어서 파싱한다.
    private void parsePacket(byte[] pkt, int dataSize, int size)
    {
        TL3500SPkt tlPkt = new TL3500SPkt();

        int idx = 1;

        tlPkt.termID = new String(pkt, idx, 16);
        idx += 16;

        tlPkt.dateTime = new String(pkt, idx, 14);
        idx += 14;
        tlPkt.jodCode = pkt[idx++];
        tlPkt.respCode = pkt[idx++];
        tlPkt.dataLength = dataSize;
        idx += 2;

        tlPkt.data = new byte[dataSize];
        System.arraycopy(pkt, idx, tlPkt.data, 0, dataSize);

        ProcessPacket(tlPkt);
    }

    // 파싱하여 각 맞는 함수로 Dispatch한다.
    private void ProcessPacket(TL3500SPkt pkt)
    {
        switch (pkt.jodCode)
        {
            case TL3500SPkt.CMD_RX_TREMCHACK:
                onTermCheck(pkt);
                break;

            case TL3500SPkt.CMD_RX_PAY:
                onPay(pkt);
                break;

            case TL3500SPkt.CMD_RX_PAY_G:       //si.20180205
                onPayG(pkt);
                break;

            case TL3500SPkt.CMD_RX_PAYCANCEL:
                onPayCancel(pkt);
                break;

            case TL3500SPkt.CMD_RX_SEARCH:
                onSearch(pkt);
                break;

            case TL3500SPkt.CMD_RX_WAITNG:
                onWaiting(pkt);
                break;

            case TL3500SPkt.CMD_RX_UID:
                onUid(pkt);
                break;

            case TL3500SPkt.CMD_RX_RESET:
                break;

            case TL3500SPkt.CMD_RX_EVENT:
                onEvent(pkt);
                break;

            case TL3500SPkt.CMD_RX_VERSION:
                onGetVersion(pkt);
                break;

            case TL3500SPkt.CMD_RX_GET_CONFIG:
                onGetConfig(pkt);
                break;

            case TL3500SPkt.CMD_RX_SET_CONFIG:
                writeConfigReq();
                break;

            case TL3500SPkt.CMD_RX_WRITE_CONFIG:
                termResetReq();
                break;

        }
    }

    // ACK가 들어오면 Retry를 더이상 하지 않는다.
    private void onAck()
    {
        retryTimer.cancel();
        setState(State.Ready);
    }

    //===============================================
    // 패킷 수신 처리
    private void onTermCheck(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        retVal.put("commStat", "" + (char)pkt.data[0]);
        retVal.put("rfModuleStat", "" + (char)pkt.data[1]);
        retVal.put("vanStat", "" + (char)pkt.data[2]);
        retVal.put("serverStat", "" + (char)pkt.data[3]);

        //DbgLog("<< TermCheck< comm:" + commStat + ", rf:" + rfModuleStat + ", van:" + vanStat + ", serv:" + serverStat);

        if (listener != null) listener.responseCallback(ResponseType.Check, retVal, lastCh);

    }

    //si.add
    private void onSearch(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        int idx = 0;

        //거래매체
        retVal.put("card_type", "" + (char)pkt.data[idx++]);
        //카드 종류
        retVal.put("card", "" + (char)pkt.data[idx++]);
        //카드 번호
        retVal.put("cardnum", new String(pkt.data, idx, 20));
        idx += 20;
        //직전거래일시
        retVal.put("date", new String(pkt.data, idx, 14));
        idx += 14;
        //직전거래금액
        retVal.put("price", new String(pkt.data, idx, 8));
        idx += 8;
        //카드잔액
        retVal.put("balance", new String(pkt.data, idx, 8));
        idx += 8;
        //거래구분
        retVal.put("info", "" + (char)pkt.data[idx]);

        //DbgLog("<< TermCheck< comm:" + commStat + ", rf:" + rfModuleStat + ", van:" + vanStat + ", serv:" + serverStat);

        if (listener != null) listener.responseCallback(ResponseType.Search, retVal, lastCh);
    }

    private void onPay(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        int idx = 0;
        char payCode = (char)(pkt.data[idx++]);
        //거래구분코드 “1”[신용승인], “3”[선불카드] “X”[거래거절]:거래매체~단말기번호 space 채움
        retVal.put("payCode", "" + payCode);

        // 거래매체 “1”[IC], “2”[MS], “3”[RF]
        retVal.put("payType", "" + (char)(pkt.data[idx++]));

        // 카드번호
        retVal.put("cardNum", new String(pkt.data, idx, 20));
        idx += 20;

        // 승인금액
        retVal.put("totalCost", new String(pkt.data, idx, 10));
        idx += 10;

        // 세금
        retVal.put("tax", new String(pkt.data, idx, 8));
        idx += 8;

        // 봉사료
        retVal.put("service", new String(pkt.data, idx, 8));
        idx += 8;

        // 할부개월
        retVal.put("div", new String(pkt.data, idx, 2));
        idx += 2;

        // 승인번호/선불카드정보
        retVal.put("authNum", new String(pkt.data, idx, 12));
        idx += 12;

        // 매출일자
        retVal.put("payDate", new String(pkt.data, idx, 8));
        idx += 8;

        // 매출시간
        retVal.put("payTime", new String(pkt.data, idx, 6));
        idx += 6;

        //고유번호
        retVal.put("uniqueNum", new String(pkt.data, idx, 12));
        idx += 12;

        //가맹점번호
        retVal.put("regNum", new String(pkt.data, idx, 15));
        idx += 15;

        //단말기번호
        retVal.put("termId", new String(pkt.data, idx, 14));
        idx += 14;

        if (payCode == 'X')
        {
            retVal.put("retCode", new String(pkt.data, idx, 3));
            idx += 3;

            retVal.put("errMsg", new String(pkt.data, idx, 37));
            idx += 37;
        }

        ////거래거절(발급사) 메시지
        //retVal.put("errMsg1", new String(pkt.data, idx, 20));
        //idx += 20;

        ////거래거절(매입사) 메시지
        //retVal.put("errMsg2", new String(pkt.data, idx, 20));
        //idx += 20;

        if (isPrePayReq[lastCh])
        {
            prePayInfo[lastCh] = retVal;
            //prePayInfo_sub = retVal;

            isPrePayReq[lastCh] = false;
        }

        lastPayInfo[lastCh] = retVal;


        if (listener != null) listener.responseCallback(ResponseType.Pay, retVal, lastCh);
    }

    //si.20180205
    private void onPayG(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        int idx = 0;
        char payCode = (char)(pkt.data[idx++]);
        //거래구분코드 “1”[신용승인], “3”[선불카드] “X”[거래거절]:거래매체~단말기번호 space 채움
        retVal.put("payCode", "" + payCode);

        // 거래매체 “1”[IC], “2”[MS], “3”[RF]
        retVal.put("payType", "" + (char)(pkt.data[idx++]));

        // 카드번호
        retVal.put("cardNum", new String(pkt.data, idx, 20));
        idx += 20;

        // 승인금액
        retVal.put("totalCost", new String(pkt.data, idx, 10));
        idx += 10;

        // 세금
        retVal.put("tax", new String(pkt.data, idx, 8));
        idx += 8;

        // 봉사료
        retVal.put("service", new String(pkt.data, idx, 8));
        idx += 8;

        // 할부개월
        retVal.put("div", new String(pkt.data, idx, 2));
        idx += 2;

        // 승인번호/선불카드정보
        try {
            retVal.put("authNum", new String(pkt.data, idx, 12, "euc-kr"));
            idx += 12;
        }
        catch (Exception e) {

        }

        // 매출일자
        retVal.put("payDate", new String(pkt.data, idx, 8));
        idx += 8;

        // 매출시간
        retVal.put("payTime", new String(pkt.data, idx, 6));
        idx += 6;

        //고유번호
        retVal.put("uniqueNum", new String(pkt.data, idx, 12));
        idx += 12;

        //가맹점번호
        retVal.put("regNum", new String(pkt.data, idx, 15));
        idx += 15;

        //단말기번호
        retVal.put("termId", new String(pkt.data, idx, 14));
        idx += 14;


        if (payCode == 'X')
        {
            retVal.put("retCode", new String(pkt.data, idx, 3));
            idx += 3;

            try {
                String errMsg = new String(pkt.data, idx, 37, "euc-kr");
                errMsg = errMsg.replaceAll("(\\r|\\n)", "");
                retVal.put("errMsg", errMsg);
            }
            catch (Exception e) {}
            idx += 37;
        }
        else idx += 40;

        //si.20180102 - PG 거래일련번호
        retVal.put("pgnum", new String(pkt.data, idx, 30));
        idx += 30;

        if (isPrePayReq[lastCh])
        {
            prePayInfo[lastCh] = retVal;
            isPrePayReq[lastCh] = false;
        }

        lastPayInfo[lastCh] = retVal;

        if (listener != null) listener.responseCallback(ResponseType.Pay, retVal, lastCh);
    }

    private void onPayCancel(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        int idx = 0;

        char payCode = (char)(pkt.data[idx++]);
        //거래구분코드 “1”[신용승인], “3”[선불카드] “X”[거래거절]:거래매체~단말기번호 space 채움
        retVal.put("payCode", "" + payCode);

        // 거래매체 “1”[IC], “2”[MS], “3”[RF]
        retVal.put("payType", "" + (char)(pkt.data[idx++]));

        // 카드번호
        retVal.put("cardNum", new String(pkt.data, idx, 20));
        idx += 20;

        // 승인금액
        retVal.put("totalCost", new String(pkt.data, idx, 10));
        idx += 10;

        // 세금
        retVal.put("tax", new String(pkt.data, idx, 8));
        idx += 8;

        // 봉사료
        retVal.put("service", new String(pkt.data, idx, 8));
        idx += 8;

        // 할부개월
        retVal.put("div", new String(pkt.data, idx, 2));
        idx += 2;

        // 승인번호/선불카드정보
        retVal.put("authNum", new String(pkt.data, idx, 12));
        idx += 12;

        // 매출일자
        retVal.put("payDate", new String(pkt.data, idx, 8));
        idx += 8;

        // 매출시간
        retVal.put("payTime", new String(pkt.data, idx, 6));
        idx += 6;

        //고유번호
        retVal.put("uniqueNum", new String(pkt.data, idx, 12));
        idx += 12;

        //가맹점번호
        retVal.put("regNum", new String(pkt.data, idx, 15));
        idx += 15;

        //단말기번호
        retVal.put("termId", new String(pkt.data, idx, 14));
        idx += 14;

        if (payCode == 'X')
        {
            retVal.put("retCode", new String(pkt.data, idx, 3));
            idx += 3;

            retVal.put("errMsg", new String(pkt.data, idx, 37));
            idx += 37;
        }

        ////거래거절(발급사) 메시지
        //retVal.put("errMsg1", new String(pkt.data, idx, 20));
        //idx += 20;

        ////거래거절(매입사) 메시지
        //retVal.put("errMsg2", new String(pkt.data, idx, 20));
        //idx += 20;            

        if (listener != null) listener.responseCallback(ResponseType.CancelPay, retVal, lastCh);
    }


    private void onWaiting(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();

        retVal.put("type", "waiting");
        if (listener != null) listener.responseCallback(ResponseType.Event, retVal, lastCh);
    }

    private void onUid(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();
        retVal.put("type", "uid");
        retVal.put("uid", new String(pkt.data, 0, 10));
        if (listener != null) listener.responseCallback(ResponseType.Event, retVal, lastCh);
    }


    private void onEvent(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();
        retVal.put("type", "event");
        retVal.put("event", "" + (char)(pkt.data[0]));
        if (listener != null) listener.responseCallback(ResponseType.Event, retVal, lastCh);

    }

    private void onGetVersion(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();
        retVal.put("type", "version");
        retVal.put("version", getStringRemoveNull(pkt.data, 30, 15));
        if (listener != null) listener.responseCallback(ResponseType.GetVersion, retVal, lastCh);
    }
    private void onGetConfig(TL3500SPkt pkt)
    {
        HashMap<String, String> retVal = new HashMap<>();
        retVal.put("type", "config");
        //가상 ID 시작 지점 		
        int idx = 16 + 4 + 1 + 16 + 16;

        retVal.put("mid", getStringRemoveNull(pkt.data, idx,16));
        idx += 16;

        retVal.put("van_ip", getStringRemoveNull(pkt.data, idx, 16));
        idx += 16;

        retVal.put("van_port", getStringRemoveNull(pkt.data, idx, 16));
        idx += 16;
        retVal.put("net_type", new String(pkt.data, idx, 1));
        idx++;

        retVal.put("ip", getStringRemoveNull(pkt.data, idx, 16));
        idx += 16;

        retVal.put("netmask", getStringRemoveNull(pkt.data, idx, 16));
        idx += 16;

        retVal.put("gateway", getStringRemoveNull(pkt.data, idx, 16));
        idx += 16;
        if (listener != null) listener.responseCallback(ResponseType.GetConfig, retVal, lastCh);
    }

    public State getCurState()  //UI 프로그램에서 사용하기 위해 public 으로 변경
    {
        return curState;
    }

    void setState(State state)
    {
        curState = state;

        //DbgLog("SetState:" + state.ToString());
        tlserStatus = "SetState : " + state.name();
        tlserStatusText = state.name();
    }

    void sendToSerial(byte[] data, int start, int size) {
        synchronized (lockSend) {
            try {
                serialOutputStream.write(data, start, size);

            }
            catch (Exception err) {
                //serStatus = "Error in read event: " + err.Message;
            }
        }
    }

    private void sendRequest(TL3500SPkt pkt)
    {
        if (curState == State.Ready)
        {
            sendRequestPacket(pkt);

            retryPacket = pkt;

            setState(State.WaitingAck);

            //Start Timer
            retryTimer.start();
        }
    }

    private void sendRequestPacket(TL3500SPkt pkt)
    {
        // 배열 초기화
        Arrays.fill(sendData, (byte)0);

        int idx = 0;
        sendData[idx++] = TL3500SPkt.STX;

        byte[] idTerm = pkt.termID.getBytes();
        System.arraycopy(idTerm, 0, sendData, idx, idTerm.length);
        idx += 16; // TERM ID Size

        byte[] datetime = pkt.dateTime.getBytes();
        System.arraycopy(datetime, 0, sendData, idx, datetime.length);
        idx += 14; // DateTime

        sendData[idx++] = pkt.jodCode;
        sendData[idx++] = pkt.respCode;

        sendData[idx++] = (byte)(pkt.dataLength & 0xff);
        sendData[idx++] = (byte)((pkt.dataLength >> 8) & 0xff);

        if (pkt.data != null) System.arraycopy(pkt.data, 0, sendData, idx, pkt.dataLength);
        idx += pkt.dataLength;
        sendData[idx++] = TL3500SPkt.ETX;

        // XOR CheckSum
        byte xor = 0;
        for (int i = 0; i < idx; i++) xor ^= sendData[i];

        sendData[idx++] = xor;

        sendToSerial(sendData, 0, idx);

        String hexData = ByteUtil.byteArrayToHexString(sendData, 0, idx);  //헥사코드로 보임
//        dbgLog(">> Send: " + hexData);
    }

    public void retrySendPktTimeout()
    {
        // 재전송 패킷이 없는경우!! 에러처리
        if (retryPacket == null)
        {
            retryTimer.cancel();
            retryPacket = null;
            setState(State.Ready);

            HashMap<String, String> retVal = new HashMap<>();

            retVal.put("error", "nopacket");
            if (listener != null) listener.responseCallback(ResponseType.Error, retVal, lastCh);

            return;
        }

        // 재전송 패킷 처리
        retryPacket.retry++;
        if (retryPacket.retry >= MAX_RETRY_COUNT)
        {
            dbgLog("Max Retry!!. Packet Timeout Error");

            retryTimer.cancel();
            retryPacket = null;

            //Timeout Error Event!!
            setState(State.Ready);

            HashMap<String, String> retVal = new HashMap<>();

            retVal.put("error", "nopacket");
            if (listener != null) listener.responseCallback(ResponseType.Error, retVal, lastCh);
            return;
        }

        sendRequestPacket(retryPacket);

        dbgLog("Retry Packet:" + retryPacket.retry);
    }

    //=================================
    //외부 함수 콜 부분
    //=================================

    //장치 체크
    public void termCheckReq(int ch)
    {
        lastCh = ch;
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_TREMCHACK);

        sendRequest(pkt);
    }

    //si.add
    public void cardInfoReq(int ch)
    {
        lastCh = ch;
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_SEARCH);

        sendRequest(pkt);
    }

    public void payReq(int cost, int tax, boolean isPrePay, int ch)
    {
        lastCh = ch;

        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_PAY);

        int totalCost = cost + tax;

        byte[] arrCost = String.format("%010d",totalCost).getBytes();
        byte[] arrTax = String.format("%08d", tax).getBytes();
        byte[] arrService = "00000000".getBytes();

        pkt.data = new byte[30];
        int idx = 0;
        pkt.data[idx++] = (byte)'1'; // 승인

        System.arraycopy(arrCost, 0, pkt.data, idx, arrCost.length);
        idx += 10;
        System.arraycopy(arrTax, 0, pkt.data, idx, arrTax.length);
        idx += 8;
        System.arraycopy(arrService, 0, pkt.data, idx, arrService.length);
        idx += 8;

        // 할부
        pkt.data[idx++] = (byte)'0';
        pkt.data[idx++] = (byte)'0';

        // 비서명
        pkt.data[idx++] = (byte)'1';

        this.isPrePayReq[lastCh] = isPrePay;
        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);
    }

    //si.20180205
    public void payReq_G(int cost, int tax, boolean isPrePay, int ch)
    {
        lastCh = ch;

        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_PAY_G);

        int totalCost = cost + tax;

        byte[] arrCost = String.format("%010d",totalCost).getBytes();
        byte[] arrTax = String.format("%08d", tax).getBytes();
        byte[] arrService = "00000000".getBytes();

        //si.20180102
        byte[] arrName = new byte[30];       
        Arrays.fill(arrName, (byte)0x20);
        
        byte[] arrPhone = new byte[20];
        Arrays.fill(arrPhone, (byte)0x20);

        byte[] arrEmail = new byte[40];
        Arrays.fill(arrEmail, (byte)0x20);

        byte[] arrCP = new byte[20];
        Arrays.fill(arrCP, (byte)0x20);

        byte[] arrPName = new byte[50];
        Arrays.fill(arrPName, (byte)0x20);

        byte[] arrAddress = new byte[100];
        Arrays.fill(arrAddress, (byte)0x20);

        byte[] arrMessage = new byte[50];
        Arrays.fill(arrMessage, (byte)0x20);


        pkt.data = new byte[339];
        int idx = 0;
        pkt.data[idx++] = (byte)'1'; // 승인

        System.arraycopy(arrCost, 0, pkt.data, idx, arrCost.length);
        idx += 10;
        System.arraycopy(arrTax, 0, pkt.data, idx, arrTax.length);
        idx += 8;
        System.arraycopy(arrService, 0, pkt.data, idx, arrService.length);
        idx += 8;

        // 할부
        pkt.data[idx++] = (byte)'0';
        pkt.data[idx++] = (byte)'0';

        //구매자명 30
        System.arraycopy(arrName, 0, pkt.data, idx, arrName.length);
        idx += 30;

        //전화번호 20
        System.arraycopy(arrPhone, 0, pkt.data, idx, arrPhone.length);
        idx += 20;

        //이메일주소 40
        System.arraycopy(arrEmail, 0, pkt.data, idx, arrEmail.length);
        idx += 40;

        //구매자 연락처 20
        System.arraycopy(arrCP, 0, pkt.data, idx, arrCP.length);
        idx += 20;

        //상품명 50
        System.arraycopy(arrPName, 0, pkt.data, idx, arrPName.length);
        idx += 50;

        //주소 100
        System.arraycopy(arrAddress, 0, pkt.data, idx, arrAddress.length);
        idx += 100;

        //수취인 메시지 50
        System.arraycopy(arrMessage, 0, pkt.data, idx, arrMessage.length);
        idx += 50;

        //// 비서명
        //pkt.data[idx++] = (byte)'1';

        this.isPrePayReq[lastCh] = isPrePay;
        this.isprepayFlag = isPrePay;
        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);

        if(isPrePay) LogWrapper.d(TAG, "PrePayReq Complete!!");
        else LogWrapper.d(TAG, "RealPayReq Complete!!");
    }

    //* payinfo : 한도승인 결제에 대한 정보
    //* cost : 취소해야할 금액
    //* tax : 세금
    //* cancelType : 무카드취소(4) 혹은 부분취소(5) 구분코드
    public void cancelPay_G(HashMap<String, String> payInfo, int cost, int tax, String apprNum, String datetime, String pgnum, int cancelType) {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_PAYCANCEL);

        int totalCost = cost + tax;

        byte[] arrCost = String.format("%010d", totalCost).getBytes();
        byte[] arrTax = String.format("%08d", tax).getBytes();
        byte[] arrService = "00000000".getBytes();

        pkt.data = new byte[89];
        int idx = 0;

        //취소구분코드
        // PG부분 취소 (4:무카드 취소) (5:부분취소)
        //무카드 취소 : 취소해야할 금액(cost)가 한도승인금액과 같을 경우에 무카드취소 진행
        //부분취소 : 취소해야할 금액(cost)이 0보다 크고 한도승인금액보다 작을 경우 부분취소 진행
        switch (cancelType) {
            case 4:
                pkt.data[idx++] = (byte) '4';
                break;
            case 5:
                pkt.data[idx++] = (byte) '5';
                break;
        }

        //“2”[RF/MS 신용승인, 카카오페 이(신용)]  must be
        pkt.data[idx++] = (byte) '2';

        System.arraycopy(arrCost, 0, pkt.data, idx, arrCost.length);
        idx += 10;
        System.arraycopy(arrTax, 0, pkt.data, idx, arrTax.length);
        idx += 8;
        System.arraycopy(arrService, 0, pkt.data, idx, arrService.length);
        idx += 8;

        // 할부
        pkt.data[idx++] = (byte) '0';
        pkt.data[idx++] = (byte) '0';

        // 비서명
        pkt.data[idx++] = (byte) '1';

        //승인번호
        String m_authNum = apprNum + "    ";
        byte[] authNum = m_authNum.getBytes();
        System.arraycopy(authNum, 0, pkt.data, idx, authNum.length);
        idx += 12;

        // 일자
        String m_date = datetime.substring(0, 8);
        byte[] costDate = m_date.getBytes();
        System.arraycopy(costDate, 0, pkt.data, idx, costDate.length);
        idx += 8;

        // 시간
        //무카드 취소일 경우, pgnum 마지막 6자리
        //부분취소일 경우 선결제 시간
        switch (cancelType) {
            case 4:
                String tmp_pgnum = pgnum.substring(25);
                byte[] pgnum6digit = tmp_pgnum.getBytes();
                System.arraycopy(pgnum6digit, 0, pkt.data, idx, pgnum6digit.length);
                break;

            case 5:
                String m_time = datetime.substring(8);
                byte[] costTime = m_time.getBytes();
                System.arraycopy(costTime, 0, pkt.data, idx, costTime.length);
                break;

        }
        idx += 6;

        //부가정보 데이터 길이(부분취소,무카드취소 반드시 30)
        pkt.data[idx++] = (byte)'3';
        pkt.data[idx++] = (byte)'0';

        //부가정보(pgnum)
        byte[] arr_pgNum = pgnum.getBytes();
        System.arraycopy(arr_pgNum, 0, pkt.data, idx, arr_pgNum.length);
        idx += 30;

        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);

    }


    public void cancelPay(HashMap<String, String> payInfo)
    {
        //
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_PAYCANCEL);

        byte[] arrCost = payInfo.get("totalCost").getBytes();
        byte[] arrTax = payInfo.get("tax").getBytes();
        byte[] arrService = payInfo.get("service").getBytes();

        pkt.data = new byte[57];
        int idx = 0;
        pkt.data[idx++] = (byte)'1'; // 요청전문취소
        pkt.data[idx++] = (byte)'1'; // 신용승인

        System.arraycopy(arrCost, 0, pkt.data, idx, arrCost.length);
        idx += 10;
        System.arraycopy(arrTax, 0, pkt.data, idx, arrTax.length);
        idx += 8;
        System.arraycopy(arrService, 0, pkt.data, idx, arrService.length);
        idx += 8;

        // 할부
        pkt.data[idx++] = (byte)'0';
        pkt.data[idx++] = (byte)'0';

        // 비서명
        pkt.data[idx++] = (byte)'1';

        //승인번호
        byte[] authNum = payInfo.get("authNum").getBytes();
        System.arraycopy(authNum, 0, pkt.data, idx, authNum.length);
        idx += 12;

        // 일자
        byte[] costDate = payInfo.get("payDate").getBytes();
        System.arraycopy(costDate, 0, pkt.data, idx, costDate.length);
        idx += 8;

        // 시간
        byte[] costTime =payInfo.get("payTime").getBytes();
        System.arraycopy(costTime, 0, pkt.data, idx, costTime.length);
        idx += 6;

        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);
    }


    public void termReadyReq()
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_WAITNG);

        sendRequest(pkt);

        ////최초결제
        //SharedData.ipcm[0].tl_pre_pay = 0;
        //SharedData.ipcm[0].tl_pre_date = "";
        //SharedData.ipcm[0].tl_pre_authno = "";
        //SharedData.ipcm[0].tl_pre_unino = "";

        //////취소결제
        //SharedData.ipcm[set_channel].tl_cancel_pay = 0;
        //SharedData.ipcm[set_channel].tl_cancel_date = "";
        //SharedData.ipcm[set_channel].tl_cancel_authno = "";
        //SharedData.ipcm[set_channel].tl_cancel_unino = "";

        //////실결제
        //SharedData.ipcm[set_channel].tl_pay = 0;
        //SharedData.ipcm[set_channel].tl_authNum = "";
        //SharedData.ipcm[set_channel].tl_date = "";
        //SharedData.ipcm[set_channel].tl_uniqueNum = "";
        //SharedData.ipcm[set_channel].tl_pgnum = "";
    }

    public void termResetReq()
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_RESET);

        sendRequest(pkt);
    }


    public void cancelPrePay(int ch)
    {
        try
        {
            lastCh = ch;
            cancelPay(prePayInfo[ch]);
            LogWrapper.d(TAG, "CancelPrePayReq Complete!!");
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "CancelPrePay:" + e.getMessage());
        }

    }

    //부분취소 및 무카드취소(PG)
    public void cancelPay_Partial(int ch, int cost, int tax, String approvalNum, String datetime, String pgnum, int cancelType) {
        try {
            lastCh = ch;
            cancelPay_G(prePayInfo[ch], cost, tax, approvalNum, datetime, pgnum, cancelType);
            if (cancelType == 5) LogWrapper.d(TAG, "PartialCancelPayReq Complete!!");
            else if (cancelType == 4) LogWrapper.d(TAG, "NonCardCancelPayreq Complete!!");
        } catch (Exception e) {
            LogWrapper.e(TAG, "PartialCancel:" + e.getMessage());
        }
    }


    //차지비 p1,q1에 대한 요청전문 취소시
    public void cancelPayFromServer(int cost, int tax, String approvalNum, String datetime, int ch){
        lastCh = ch;

        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_PAYCANCEL);

        int totalCost = cost + tax;

        byte[] arrCost = String.format("%010d",totalCost).getBytes();
        byte[] arrTax = String.format("%08d", tax).getBytes();
        byte[] arrService = "00000000".getBytes();

        pkt.data = new byte[57];
        int idx = 0;
        pkt.data[idx++] = (byte)'1'; // 요청전문취소
        pkt.data[idx++] = (byte)'1'; // 신용승인

        System.arraycopy(arrCost, 0, pkt.data, idx, arrCost.length);
        idx += 10;
        System.arraycopy(arrTax, 0, pkt.data, idx, arrTax.length);
        idx += 8;
        System.arraycopy(arrService, 0, pkt.data, idx, arrService.length);
        idx += 8;

        // 할부
        pkt.data[idx++] = (byte)'0';
        pkt.data[idx++] = (byte)'0';

        // 비서명
        pkt.data[idx++] = (byte)'1';

        //승인번호
        String m_authNum = approvalNum+"    ";
        byte[] authNum = m_authNum.getBytes();
        System.arraycopy(authNum, 0, pkt.data, idx, authNum.length);
        idx += 12;

        // 일자
        String m_date = datetime.substring(0,8);
        byte[] costDate = m_date.getBytes();
        System.arraycopy(costDate, 0, pkt.data, idx, costDate.length);
        idx += 8;

        // 시간
        String m_time = datetime.substring(8);
        byte[] costTime = m_time.getBytes();
        System.arraycopy(costTime, 0, pkt.data, idx, costTime.length);
        idx += 6;

        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);
    }

    public void cancelLastPay(int ch)
    {
        lastCh = ch;
        cancelPay(lastPayInfo[ch]);
    }

    //=====================================		
    // 2019.10.10 조회/설정 추가		
    //=====================================		
    public void getVersionReq()
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_VERSION);
        sendRequest(pkt);
    }
    public void getConfigReq()
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_GET_CONFIG);
        sendRequest(pkt);
    }
    
    public void setConfigReq(String mid, String vanIP, String vanPort, String paymip, String paymgw)
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_SET_CONFIG);
        pkt.data = new byte[150];
        Arrays.fill(pkt.data, (byte)0);

        // SAM Slot 1		
        int idx = 16;


        pkt.data[idx++] = 0x30; // SAM slot 1 후불		
        idx += 3;

        pkt.data[idx++] = 0x30; // 통신포트 COM		

        idx += 16; // 이더넷 통신 IP 사용안함		

        idx += 16; // 이더넷 통신 포트 사용안함		
        //MID		
        byte[] rawMid = mid.getBytes();
        System.arraycopy(rawMid, 0, pkt.data, idx, rawMid.length);
        idx += 16;
        // VanIP		
        byte[] rawVanIp = vanIP.getBytes();
        System.arraycopy(rawVanIp, 0, pkt.data, idx, rawVanIp.length);
        idx += 16;
        // VanPort		
        byte[] rawVanPort = vanPort.getBytes();
        System.arraycopy(rawVanPort, 0, pkt.data, idx, rawVanPort.length);
        idx += 16;
        // 단말기 IP 방식		
        //pkt.data[idx++] = 0x30; // DHCP
        pkt.data[idx++] = 0x31; // DHCP	 고정 아이피 적용 swpark 0x31 static
        // IP Set to "0"		
        //pkt.data[idx++] = 0x30;     
        byte[] rawPayMIp = paymip.getBytes();
        System.arraycopy(rawPayMIp, 0, pkt.data, idx, rawPayMIp.length);
        idx += 16;

        byte[] rawPayMsnet = "255.255.255.0".getBytes();    //서브넷 마스크 기본 255.255.255.0
        System.arraycopy(rawPayMsnet, 0, pkt.data, idx, rawPayMsnet.length);
        idx += 16;

        byte[] rawPayMgw = paymgw.getBytes();
        System.arraycopy(rawPayMgw, 0, pkt.data, idx, rawPayMgw.length);
        idx += 16;

        //오류 있을 경우 여기 까지 삭제

        pkt.dataLength = pkt.data.length;

        sendRequest(pkt);		 // 수정필요!!!
    }
    public void writeConfigReq()
    {
        TL3500SPkt pkt = new TL3500SPkt(TERMID, TL3500SPkt.CMD_TX_WRITE_CONFIG);
        sendRequest(pkt);
    }

    public void close()
    {
        serialPort.close();
        retryTimer.cancel();
        setState(State.None);
    }

    //디버깅용 함수
    void dbgLog(String str)
    {
        LogWrapper.d(TAG, str);
    }

    public static String getStringRemoveNull(byte[] data, int offset, int length)
    {
        String stringValue = new String(data, offset, length);
        return stringValue.replace("\0", "");
    }

}
