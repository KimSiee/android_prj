/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 19 오전 8:28
 *
 */

package com.joas.kepcocomm;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.hw.payment.tl3500s.TL3500SListener;
import com.joas.utils.ByteUtil;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.SerialPort;
import com.joas.utils.TimeUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.joas.kepcocomm.KepcoProtocol.KepcoCmd.*;
import static com.joas.kepcocomm.KepcoProtocol.KepcoRet.*;

public class KepcoComm extends Thread implements TL3500SListener {
    public static final String TAG = "KepcoComm";
    public static final String FILE_CONFIG_LOCALMODE = "local_mode.txt";
    public static final String FILE_CONFIG_CARDDATE = "card_date.txt";
    public static final String FILE_UPDATE_FIRMWARE = "update.zip";
    public static final String FILE_UPDATE_FIRMWARE_APK = "update.apk";
    public static final String FILE_QR_IMG = "qrimg";
    public static final String COMM_LOG_PATH = "/Log";
    public static final int COMM_LOG_RECORD_DAYS = 180; // 180일(6개월)
    public static final String PAY_TERMINAL_RESET_CTRLID = "999999";

    public static final int RECV_BUFF_MAX = 8096;
    public static final int SOCKET_CONNECT_TIMEOUT = 5000;
    public static final int RECOVERY_LOST_PKT_MAX_PER_PING = 20; // 한번에 복구 가능한 패킷수

    public static final byte[] PROTOCOL_VERSION = {(byte)0x02, (byte)0x02, (byte)0x00, (byte)0x08}; // 2.2.8


    public enum PayTerminalKind {
        NONNE,
        STANDARD,
        TL3500S
    };

    enum FwUpdateStatus {
        None,
        Recving,
        Pending,
        Finished
    };

    KepcoCommListener kepcoCommListener = null;
    //KepcoCommMonitorListener joasCommMonitorListener = null;

    String firmVersion = "V1.0";
    String serverIP = "192.168.0.1";
    int serverPort = 8484;

    Socket socket;

    boolean isEndFlag = false;
    boolean isConnected = false;

    OutputStream outputStream;
    InputStream inputStream;

    byte[] rawLocalIP = new byte[4];
    byte[] rawServerIP  = new byte[4];;

    ConcurrentLinkedDeque<KepcoPacket> sendServerQueue = new ConcurrentLinkedDeque<KepcoPacket>();
    Thread sendMsgThread;

    byte[] recvBuf = new byte[RECV_BUFF_MAX];
    byte[] recvBufSerial = new byte[RECV_BUFF_MAX];
    KepcoPacket lastSendPacket = null;

    int chargerID;

    String serverAuthKey = "";

    int sendPacketSeq = 0;
    boolean onBootMsgSent = false;

    KepcoChargerInfo chargerInfo = new KepcoChargerInfo();
    KepcoCostManager costManager;

    TimeoutTimer timerSec;
    int timerPingCnt = 0;
    int timerPaymStatCnt = 0;
    int timerPaymStatAckCnt = 0;
    int timerPaymStatErrCnt = 0;
    int timerPaymIntyCnt = 0; // 무결성 카운트
    int timerCpConfigCnt = 0;
    boolean isPaymStatErr = false;

    boolean todayCpConfigSent = true;
    boolean isChargingState = false;

    // TimerSec 에서 사용되는 변수
    int oldDay = 0;
    int oldChargingStatMin = -1;

    boolean usePayTerminal = false;
    boolean isSerialOpened = false;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;

    private SerialPort mSerialPort;

    Thread serialRecvThread;

    Activity androidActivity = null;
    Context androidContext = null;
    KepcoCommDbHelper commDbHelper;
    KepcoMember kepcoMember;
    String basePath = "";

    // CardData
    boolean isCardInfoRecving = false;
    int cardInfoRecvIdx = 0;

    //Firmware Update
    FwUpdateStatus firmwareRecvingStatus = FwUpdateStatus.None;
    int firmwareRecvIdx = 0;
    FileOutputStream firmwareOutPutStream = null;

    //QR Image Down
    int qrRecvIdx = 0;
    FileOutputStream qrOutPutStream = null;

    // ChargeCtl
    List<KepcoChargeCtl> chargeCtlList = Collections.synchronizedList(new ArrayList<KepcoChargeCtl>());

    boolean isLocalMode = true;

    // 미전송 전문 전송
    KepcoLostTransManager lostTransManager;

    // 카드리더기 종류
    PayTerminalKind payTerminalKind = PayTerminalKind.NONNE;
    String tl3500CardInputState = "N";
    TL3500S tl3500s;

    boolean useQRCodeAuth = true;

    public KepcoComm(Activity activity, String firmVer, String svrip, int svrport, String cid, String authKey, String baseFilePath, String devPayTerminal, boolean usePayTerm) {
        initKepcoComm(activity, firmVer, svrip, svrport, cid, authKey, baseFilePath, devPayTerminal, usePayTerm, PayTerminalKind.STANDARD, true);
    }

    public KepcoComm(Activity activity, String firmVer, String svrip, int svrport, String cid, String authKey, String baseFilePath, String devPayTerminal, boolean usePayTerm, PayTerminalKind payTerminal, boolean useQRCode) {
        initKepcoComm(activity, firmVer, svrip, svrport, cid, authKey, baseFilePath, devPayTerminal, usePayTerm, payTerminal, useQRCode);
    }

    void initKepcoComm(Activity activity, String firmVer, String svrip, int svrport, String cid, String authKey, String baseFilePath, String devPayTerminal, boolean usePayTerm, PayTerminalKind payTerminal, boolean useQRCode) {
        androidActivity = activity;
        androidContext = activity.getBaseContext();
        firmVersion = firmVer;
        serverIP = svrip;
        serverPort = svrport;
        basePath = baseFilePath;
        payTerminalKind = payTerminal;
        useQRCodeAuth = useQRCode;

        try {
            chargerID = Integer.parseInt(cid);
        }
        catch (Exception e) {
            chargerID = 0;
        }

        // Make BaseFilePath directory
        File parent = new File(baseFilePath);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        serverAuthKey = authKey;

        isEndFlag = false;
        isConnected = false;
        sendPacketSeq = 0;

        setLocalServerIPInfo();

        costManager = new KepcoCostManager(baseFilePath);

        commDbHelper = new KepcoCommDbHelper(androidContext, baseFilePath);
        commDbHelper.open();

        kepcoMember = new KepcoMember(commDbHelper);
        lostTransManager = new KepcoLostTransManager(commDbHelper);

        loadChargerInfo();

        this.usePayTerminal = usePayTerm;

        // 결재 단말기 초기화
        if ( this.usePayTerminal == true ) {
            initPayTerminal(devPayTerminal);
        }

        updateFileRemove();

        timerSec = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerTaskPeriodSecond();
            }
        });

        FileUtil.pastDateLogRemove(basePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제
    }

    void loadChargerInfo() {
        File file = new File(basePath +"/"+FILE_CONFIG_CARDDATE);

        if (file.exists()) {
            try {
                chargerInfo.card_date = FileUtil.getStringFromFile(basePath + "/" + FILE_CONFIG_CARDDATE);
                chargerInfo.card_date = chargerInfo.card_date.replaceAll("(\\r|\\n)", "");
            }
            catch (Exception e) {}
        }

        file = new File(basePath +"/"+FILE_CONFIG_LOCALMODE);
        String local_mode = "";
        if (file.exists()) {
            try {
                local_mode = FileUtil.getStringFromFile(basePath + "/" + FILE_CONFIG_LOCALMODE);
                local_mode = local_mode.replaceAll("(\\r|\\n)", "");

                chargerInfo.localMode = local_mode.equals("1") ? KepcoProtocol.LocalMode.CARDNUM_SEARCH : KepcoProtocol.LocalMode.CARD_TAG_AUTO;
            }
            catch (Exception e) {}
        }
    }

    public TL3500S getTL3500S() { return tl3500s; }

    void initPayTerminal(String devPayTerminal) {
        if (payTerminalKind == PayTerminalKind.STANDARD) initPayTerminalStandard(devPayTerminal);
        else if (payTerminalKind == PayTerminalKind.TL3500S) {
            tl3500s = new TL3500S(1, devPayTerminal);
            tl3500s.setListener(this);
            tl3500s.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tl3500s.getVersionReq();
                }
            }, 500);
        }
    }

    void initPayTerminalStandard(String devPayTerminal) {
        try {
            mSerialPort = new SerialPort(new File(devPayTerminal), 115200, 0);
            if ( mSerialPort == null ) {
                LogWrapper.v("MainAct", "SerialPort Open Fail!");
                return;
            }

            serialInputStream = mSerialPort.getInputStream();
            serialOutputStream = mSerialPort.getOutputStream();

            isSerialOpened = true;

            serialRecvThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while ( !isEndFlag ) {
                        try {
                            RecvProcess(serialInputStream, recvBufSerial);
                        }
                        catch (Exception e) {
                            LogWrapper.e(TAG, "run SerialRx() ex:"+e.toString());
                            try {
                                Thread.sleep(5000);
                            }
                            catch(Exception ex){
                                //Nothing..
                            }
                        }
                    }
                }
            });
            serialRecvThread.start();

            sendGlobPaymInty_66(true);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendGlobPaymStat_61();
                }
            }, 500);

        }
        catch (Exception e) {
            LogWrapper.e("MainAct", e.getMessage());
        }
    }

    public KepcoChargerInfo getChargerInfo() { return chargerInfo; }
    public TL3500S getTl3500s() { return tl3500s; }

    public void setListener(KepcoCommListener listener) {
        kepcoCommListener = listener;
    }

    public void setServerAddr(String ip, int port) {
        serverIP = ip;
        serverPort = port;

        setLocalServerIPInfo();
    }

    public void setChargerID(String cid) {
        try {
            chargerID = Integer.parseInt(cid);
        }
        catch (Exception e) {
            chargerID = 0;
        }
    }

    public void setServerAuthKey(String authKey) {
        serverAuthKey = authKey;
    }

    void setLocalServerIPInfo() {
        String strLocalIP = NetUtil.getLocalIpAddress();
        if ( strLocalIP == null ) strLocalIP = "127.0.0.1";
        rawLocalIP = NetUtil.getBytesFromStrIPAddr(strLocalIP);
        rawServerIP = NetUtil.getBytesFromStrIPAddr(serverIP);
        if ( rawServerIP == null ) {
            rawServerIP = new byte[4];
        }

    }

    public KepcoCostManager getCostManager() { return costManager; }

    public boolean getIsLocalMode() { return isLocalMode; }

    public void setPayStatError(boolean tf) { isPaymStatErr = tf; }

    public void updateFileRemove() {
        try {
            File file = new File(basePath + "/" + FILE_UPDATE_FIRMWARE);
            if (file.exists()) file.delete();

            file = new File(basePath + "/" + FILE_UPDATE_FIRMWARE_APK);
            if (file.exists()) file.delete();
        }
        catch (Exception e) {

        }
    }

    public void startComm() {
        isEndFlag = false;
        isConnected = false;
        this.start();

        sendMsgThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMsgProcess();
            }
        });
        sendMsgThread.start();

        timerSec.start();
    }

    public void stopComm() {
        isEndFlag = true;
        isConnected = false;
        timerSec.cancel();

        this.interrupt();
        sendMsgThread.interrupt();
    }

    public boolean getConnectStatus() {
        return isConnected;
    }

    public int getNewSeqNumber() {
        sendPacketSeq = (sendPacketSeq + 1) % 65536;
        return sendPacketSeq;
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
                    LogWrapper.e(TAG, "서버 연결 실패. 10초후 재시도...:"+ sockEx.toString());
                    Thread.sleep(10000); // 10 sec Sleep
                    continue;
                }

                // 접속이 성공적으로 이루어지면 이벤트 발생
                if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommConnected();
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

                onConnectedProcess();

                RecvProcess(inputStream, recvBuf);

                // Disconnect Event
                if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommDisconnected();
                isLocalMode = true;

                isConnected = false; // 소켓 연결 끊어짐
                if ( !socket.isClosed() ) socket.close();
                LogWrapper.v(TAG, "서버 연결 끊어짐. 5초후 재연결..");
                Thread.sleep(5000); // 5 sec Sleep

                updateOrFirmwareError();

                dataToLog("CONN", "==== DISCONNECTED ====");
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

    void RecvProcess(InputStream readStream, byte[] buffer) {
        int bufCnt = 0;

        while ( !isEndFlag ) {
            int recvSize = -1;

            if (bufCnt >= RECV_BUFF_MAX) bufCnt = 0; // 버퍼가 넘어가는 경우에(Overflow) 버퍼를 버린다

            try {
                recvSize = readStream.read(buffer, bufCnt, RECV_BUFF_MAX-bufCnt);
            }
            catch(Exception ex){
                LogWrapper.e(TAG, "RecvProcess() read:"+ex.toString());
                // Exception 발생시 종료
                return;
            }
            // 소켓으로 받은 크기가 0보다 작으면 끊어짐
            if ( recvSize < 0 )  return;

            bufCnt += recvSize;

            if (bufCnt < KepcoProtocol.HEADER_SIZE) continue;

            // 나머지 Length만큼 읽어들인다.
            int length = ByteUtil.makeInt(buffer, KepcoProtocol.HEADER_LENGTH_FIELD, true);
            int packetSize = (KepcoProtocol.HEADER_SIZE + length);

            // 전체 크기가 MAX보다 크다면 버린다.(length 잘못)
            if ( packetSize > RECV_BUFF_MAX) {
                bufCnt = 0;
                continue;
            }

            // 전체 Length만큼 읽어들인다.
            if ( bufCnt < packetSize ) continue;

            // 데이터 로그 기록(파싱 에러전에 남김)
            dataToLog(buffer, bufCnt, "RX");

            // 처음 헤더를 검사하여 맞지 않으면 버린다.
            if ( ByteUtil.arrayCompare(KepcoProtocol.HEADER_MAGIC, 0, buffer, 0, KepcoProtocol.HEADER_MAGIC.length) == false )
            {
                LogWrapper.e(TAG, "RecvProcess(): 헤더가 맞지 않음:"+ByteUtil.byteArrayToHexStringDiv(buffer, 0, KepcoProtocol.HEADER_MAGIC.length, '-'));
                bufCnt = 0;
                continue;
            }

            recvPacketProcess(buffer, packetSize);

            //메모리 이동(읽은만큼 버퍼에서 당긴다.)
            System.arraycopy(buffer, packetSize, buffer, 0, bufCnt - packetSize);
            bufCnt -= packetSize;
        }
    }

    /**
     * 마지막 보낸 패킷의 ACK를 처리한다.
     * 만약 cmd와 messageID가 같다면 ack를 처리한다.
     * @param recvPacket 받은 패킷
     */
    void lastSendPacketAckProcess(KepcoPacket recvPacket) {
        if ( lastSendPacket.cmd == recvPacket.cmd ) {
            if ( ByteUtil.arrayCompare(lastSendPacket.messageID, 0, recvPacket.messageID, 0, recvPacket.messageID.length) == true ) {
                lastSendPacket.isRecvAck = true;
            }

            if ( lostTransManager.isPacketBackupNeed(lastSendPacket.cmd)) {
                lostTransManager.removeTransPacket(lastSendPacket.messageID);
            }
        }
    }

    void dataToLog(byte[] data, int size, String tag) {
        String strLog = ByteUtil.byteArrayToHexStringDiv(data, 0, size, '-');
        FileUtil.appendDateLog(basePath+COMM_LOG_PATH, tag+">"+strLog);

        if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommLogEvent(tag, strLog);
    }

    void dataToLog(String tag, String data) {
        FileUtil.appendDateLog(basePath+COMM_LOG_PATH, tag+">"+ data);
        if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommLogEvent(tag, data);
    }

    synchronized void recvPacketProcess(byte[] packet, int packetSize) {
        try {
            KepcoPacket recvPacket = new KepcoPacket(packet, packetSize);
            KepcoProtocol.KepcoCmd cmd = KepcoProtocol.KepcoCmd.getValue(recvPacket.cmd);

            LogWrapper.d("CommRX", recvPacket.toString());
            dataToLog("CommRX", recvPacket.toString());

            // Sent Packet에 대한 ACK 처리
            if (lastSendPacket != null) lastSendPacketAckProcess(recvPacket);

            if ( recvPacket.ret == PKT_ERROR.getID() ) {
                LogWrapper.e(TAG, "recvPacketProcess() Get Return ERROR !!");
                return;
            }

            switch (cmd) {
                case GLOB_STAT_PING_10:
                    onGlobStatPing_10(recvPacket);
                    break;
                case GLOB_INIT_START_11:
                    onGlobInitStart_11(recvPacket);
                    break;
                case GLOB_INIT_END_12:
                    break;
                case PAYM_STAT_INFO_13:
                    onPaymStatInfo_13(recvPacket);
                    break;
                case GLOB_TIME_SYNC_14:
                    break;
                case GLOB_CHARGE_CTL_15:
                    onGlobChargeCtl_15(recvPacket);
                    break;

                case INFO_CHARGEUCOST_REQ_22:
                    onChargeUCostReq_22(recvPacket);
                    break;
                case GLOB_LOCK_OPEN_30:
                    onGlobLockOpen_30(recvPacket);
                    break;
                case EV_MEMB_CERT_31:
                    onEvMembCert_31(recvPacket);
                    break;
                case EV_CHARGE_END_35:
                    break;
                case EV_MEMB_CARD_36:
                    onEvMembCard_36(recvPacket);
                    break;
                case GLOB_CARD_INFO_37:
                    onGlobCardInfo_37(recvPacket);
                    break;
                case GLOB_CARD_APRV_38:
                    break;
                case EV_CHARGE_STAT_34:
                    onEVChargeStat_34(recvPacket);
                    break;
                case GLOB_CP_CONFIG_72:
                    break;
                case GLOB_PAYM_STAT_61:
                    onGlobPaymStat_61(recvPacket);
                    break;
                case GLOB_CHARGE_AMT_62:
                    onGlobChargeAmt_62(recvPacket);
                    break;
                case GLOB_PAYM_COMM_64:
                    break;
                case GLOB_PAYM_ANT_65:
                    onGlobPaymAnt_65(recvPacket);
                    break;
                case GLOB_PAYM_INTY_66:
                    onGlobPaymInty_66(recvPacket);
                    break;
                case GLOB_PAYM_STAT_SEND_67:
                    onGlobPaymStatSend_67(recvPacket);
                    break;
                case GLOB_PAYM_CTRL_68:
                    onGlobPaymCtl_68(recvPacket);
                    break;
                case GLOB_FAULT_EVENT_16:
                    break;
                case GLOB_FIRM_UPDATE_71:
                    onGlobFirmUpdate_71(recvPacket);
                    break;
                case GLOB_QRIMG_DOWN_73:
                    onGlobQRImgDown_73(recvPacket);
                    break;
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "recvPacketProcess() ex:"+e.toString()+", trace:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    synchronized void sendPacket(KepcoPacket packet) {
        try {
            LogWrapper.d("CommTX", packet.toString());
            dataToLog(packet.raw, packet.raw.length, "TX");
            dataToLog("CommTX", packet.toString());

            outputStream.write(packet.raw);
            outputStream.flush();
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "sendPacket Err() e:"+e.toString());
        }
    }

    synchronized void sendPayTerminalPacket(KepcoPacket packet) {
        if (usePayTerminal && payTerminalKind == PayTerminalKind.STANDARD) {
            try {
                LogWrapper.d("CommSTX", packet.toString());

                dataToLog(packet.raw, packet.raw.length, "STX");
                dataToLog("CommSTX", packet.toString());

                serialOutputStream.write(packet.raw);
                serialOutputStream.flush();
            } catch (Exception e) {
                LogWrapper.e(TAG, "sendPayTerminalPacket Err() e:" + e.toString());
            }
        }
        else {
            LogWrapper.d(TAG, "No use payTerminal!! pkt:" + KepcoProtocol.KepcoCmd.getValue(packet.cmd).name());
        }
    }

    /**
     * 보낼 패킷을 큐에 넣는다.
     * @param packet SendQueue에 저장할 패킷
     */
    public void AddSendServerPacket(KepcoPacket packet) {
        if ( lostTransManager.isPacketBackupNeed(packet.cmd)) {
            lostTransManager.addTransPacket(packet);
        }

        // 결제단말기로 보내는 명령어는 시리얼로 데이터를 보냄
        if ( KepcoProtocol.isCmdForPayTerminal(packet.cmd)) {
            sendPayTerminalPacket(packet);
        }
        else {
            sendServerQueue.addLast(packet);
        }
    }

    void sendMsgProcess() {
        while ( !isEndFlag ) {
            try {
                if (sendServerQueue.size() == 0) {
                    Thread.sleep(10);
                    continue;
                }

                lastSendPacket = sendServerQueue.peekFirst();

                if ( lastSendPacket == null ) {
                    Thread.sleep(10);
                    continue;
                }

                if ( isConnected ) sendPacket(lastSendPacket);
                else {
                    sendServerQueue.pollFirst();
                    Thread.sleep(10);
                    continue;
                }

                // ACK가 필요없는 패킷일 경우 Skip함.
                if ( lastSendPacket.ret == PKT_YES.getID() || lastSendPacket.ret == PKT_ACK.getID() ) {
                    sendServerQueue.pollFirst();
                    Thread.sleep(10);
                    continue;
                }

                // Ack를 기다린다.
                int loop = 3000; // Timeout ( 첫시도 후 30 sec )
                if ( lastSendPacket.retryCnt > 0 ) loop = 1500; // 두번째 부터 15초

                while ( loop-- > 0 ) {
                    if ( lastSendPacket.isRecvAck == true ) {
                        sendServerQueue.pollFirst();
                        break;
                    }
                    Thread.sleep(10);
                }

                if ( lastSendPacket.isRecvAck == false ) {
                    lastSendPacket.retryCnt++;
                    if ( lastSendPacket.retryCnt > 2 ) {
                        sendServerQueue.pollFirst();

                        LogWrapper.e(TAG, "sendMsgProcess cmd:"+Integer.toHexString(lastSendPacket.cmd)+" Retry Over");

                        onErrorChangeLocalMode();
                    }
                    else {
                        LogWrapper.e(TAG, "sendMsgProcess cmd:"+Integer.toHexString(lastSendPacket.cmd)+" Retry:"+lastSendPacket.retryCnt);
                    }
                }
                Thread.sleep(10);
            }
            catch (Exception e) {
                LogWrapper.e(TAG, "sendMsgProcess e:"+e.toString());
            }
        }
    }

    /**
     * 초당 발생하는 이벤트를 처리한다.
     */
    void timerTaskPeriodSecond() {
        // 5분에 한번씩 Ping 메시지를 전송한다.
        if ( timerPingCnt++ > KepcoProtocol.PING_MSG_PERIOD ) {
            if ( isChargingState == false ) {
                sendGlobStatPing_10();
            }
            timerPingCnt = 0;
        }

        if ( usePayTerminal ) {
            if ( payTerminalKind == PayTerminalKind.STANDARD ) {
                if (timerPaymStatCnt++ > KepcoProtocol.PING_MSG_PERIOD) {
                    sendGlobPaymStat_61();
                    timerPaymStatCnt = 0;
                }

                if (timerPaymStatAckCnt > 0) {
                    timerPaymStatAckCnt--;
                    if (timerPaymStatAckCnt == 0) {
                        timerPaymStatErrCnt++;
                        if (timerPaymStatErrCnt > 3) {
                            if (kepcoCommListener != null) kepcoCommListener.onPaymStatErr(true);
                            isPaymStatErr = true;
                            timerPaymStatErrCnt = 0;
                        }
                    }
                }

                if (timerPaymIntyCnt++ > KepcoProtocol.PAYM_INTY_PERIOD) {
                    sendGlobPaymInty_66(false);
                    timerPaymIntyCnt = 0;
                }
            }
            else if ( payTerminalKind == PayTerminalKind.TL3500S ) {
                if (timerPaymStatCnt++ > KepcoProtocol.PING_MSG_PERIOD) {
                    tl3500s.getVersionReq();
                    timerPaymStatCnt = 0;

                    if (timerPaymStatErrCnt++ > 3) {
                        if (kepcoCommListener != null) kepcoCommListener.onPaymStatErr(true);
                        isPaymStatErr = true;
                        timerPaymStatErrCnt = 0;
                    }
                }
            }
        }

        // 시작시간 단가, 구간 단가 설정

        Calendar c = Calendar.getInstance();
        int curTimeSec = (c.get(Calendar.HOUR_OF_DAY)*3600)+(c.get(Calendar.MINUTE)*60) + c.get(Calendar.SECOND);

        if ( isChargingState == false ) updateLoadClAndUCost(curTimeSec/1800);

        if ( curTimeSec >= chargerID && todayCpConfigSent == false) {
            sendGlobCpConfig_72();
            todayCpConfigSent = true;
        }

        int curDay =  c.get(Calendar.DAY_OF_MONTH);

        // 날짜가 바뀌였을때 처리
        if ( oldDay != 0 && oldDay != curDay ) {
            todayCpConfigSent = false;
            FileUtil.pastDateLogRemove(basePath+COMM_LOG_PATH, "txt", COMM_LOG_RECORD_DAYS); // 이전 통신로그 삭제

            // GLOB_CP_CONFIG(0x72) 메시지 타이머 시작
            timerCpConfigCnt = chargerID;
        }

        // GLOB_CP_CONFIG(0x72) 카운터
        if ( timerCpConfigCnt > 0 ) {
            timerCpConfigCnt--;
            if ( timerCpConfigCnt == 0 ) sendGlobCpConfig_72();
        }

        processChargeCtl();

        oldDay = curDay;
    }

    public void doChargingInfoPeroidPerSecond(int curMin, int timeCost) {
        if ( oldChargingStatMin != curMin && (curMin % 5) == 0 ) {
            oldChargingStatMin = curMin;

            processChargingInfoPeriod(timeCost);
            sendEvChargeStat_34();
        }
    }

    void processChargingInfoPeriod(int time_cost) {
        chargerInfo.cp_stat = KepcoProtocol.CPStat.CHARGING;
        KepcoCostManager.ChargingPeriodInfo info = costManager.getAndResetPeriodData(time_cost);
        chargerInfo.current_kwh = info.periodSumKwh;
        chargerInfo.current_amt = info.periodSumCost;
        chargerInfo.current_ucost = info.periodUCost;
        chargerInfo.load_cl = info.periodLoadCL;
    }

    void updateLoadClAndUCost(int time_cost) {
        chargerInfo.load_cl = costManager.getCurrentCostInfo().LOAD_CL[time_cost];
        chargerInfo.current_ucost = costManager.getCurTimeCostVal(time_cost);
    }

    public double getCurrentCostUnit() {
        Calendar c = Calendar.getInstance();
        int curTimeSec = (c.get(Calendar.HOUR_OF_DAY)*3600)+(c.get(Calendar.MINUTE)*60) + c.get(Calendar.SECOND);
        int timeCost = curTimeSec/1800;

        return costManager.getCurTimeCostVal(timeCost);
    }

    void processChargeCtl() {
        Iterator<KepcoChargeCtl> it = chargeCtlList.listIterator();
        while (it.hasNext()) {
            KepcoChargeCtl chargeCtl = it.next();

            switch (chargeCtl.state) {
                case NONE:
                    if ( chargeCtl.isStartCondition(isChargingState) ) {
                        chargeCtl.state = KepcoChargeCtl.State.READY;

                        // 결제 단말기 제어는 결제단말기에서 처리함
                        if ( chargeCtl.ctrl_cd.matches("6|7|8") ) {
                            if ( usePayTerminal ) {
                                if ( payTerminalKind == PayTerminalKind.STANDARD ) {
                                    sendGlobPaymCtl_68(chargeCtl);
                                    it.remove();
                                }
                                else if ( payTerminalKind == PayTerminalKind.TL3500S ){
                                    tl3500s.termResetReq();
                                }
                            }
                        }
                        else if ( kepcoCommListener != null ) kepcoCommListener.onChargeCtl(chargeCtl);
                    }
                    break;

                case STARTED:
                    break;

                case FINISHED:
                    it.remove();
                    break;
                    default:
                        break;
            }
        }
    }

    /**
     * 서버 연결 이벤트가 발생되었을때 처리 함수
     */
    void onConnectedProcess() {
        dataToLog("CONN", "==== CONNECTED ====");
        if ( onBootMsgSent == false ) {
            //GLOB_START
            sendGlobInitStart_11();

            sendGlobCpConfig_72();

            if ( useQRCodeAuth ) sendGlobQRImgDown_73(PKT_REQUEST);

            onBootMsgSent = true;

            if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommConnectedFirst();
        }
        if ( isChargingState == false ) {
            sendGlobStatPing_10();
        }
    }

    void initStatVal() {
        isChargingState = false;

        chargerInfo.member_card_no = "";
        chargerInfo.charge_req_cfm_mthd = KepcoProtocol.CHARGE_REQ_CFM_METHOD_FULL; // 1:full, 2:전력량, 3:금액
        chargerInfo.charge_req_kwh = 99.9;
        chargerInfo.charge_req_amt = 0;
        chargerInfo.pay_mthd = KepcoProtocol.PAY_METHOD_MEMBER;
        chargerInfo.charge_st_datetime = "";
        chargerInfo.current_kwh = 0;
        chargerInfo.current_amt = 0;
        chargerInfo.current_ucost = 0;
        chargerInfo.charge_accum_time = "";
        chargerInfo.car_soc = 0;
        chargerInfo.load_cl = 0;

        chargerInfo.charge_end_stat = KepcoProtocol.CHARGE_END_FULL; // Full 충전

        chargerInfo.isPaymentCharging = false;
        chargerInfo.initKepcoPayRet();
    }

    public void setCPStat(KepcoProtocol.CPStat state) {
        if ( chargerInfo.cp_stat == state ) return;
        chargerInfo.cp_stat = state;

        switch ( chargerInfo.cp_stat) {
            case READY:

                // 결제를 하고 35를 보내지 않았으면 무조건 35를 보낸다.
                if (chargerInfo.isPaymentCharging) {
                    // 사용자 종료가 아닌경우 Fault처리함.
                    if ( chargerInfo.charge_end_stat == KepcoProtocol.CHARGE_END_FULL ) {
                        chargerInfo.charge_end_stat = KepcoProtocol.CHARGE_END_FAULT;
                    }
                    sendEvChargeEnd_35();
                }

                // 변수 초기화
                initStatVal();
                break;
            case START:
                break;
            case CONNECT:
                break;
            case CANCEL_CHARGE:
                break;
            case CHARGING:
                break;
            case FINISH_CHARGE:
                break;
            case SEPERATE_CONNECTOR:
                break;
        }

        sendEvChargeStat_34();
    }

    public void setChargeReqCfmMethod(int method) { chargerInfo.charge_req_cfm_mthd = method; }
    public int getChargeReqCfmMethod() { return chargerInfo.charge_req_cfm_mthd; }
    public void setOutletType(KepcoProtocol.OutletType type) {
        chargerInfo.outlet_type = type;
    }

    public void setChargingTime(int timeSec) {
        chargerInfo.charge_accum_time = String.format("%d:%d:%d", timeSec / 3600, (timeSec / 60) % 60, timeSec % 60);
    }

    public void setCarSoc(int soc ) {
        chargerInfo.car_soc = soc;
    }

    public void startCharge(double meterVal) {
        // 충전 시작시간 설정
        chargerInfo.charge_st_datetime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");

        // 단가 계산 관련 초기화
        Calendar c = Calendar.getInstance();
        int curTimeSec = (c.get(Calendar.HOUR_OF_DAY)*3600)+(c.get(Calendar.MINUTE)*60) + c.get(Calendar.SECOND);
        costManager.startCostCalc(meterVal, chargerInfo.rxInfo.ucost_aply_tp, chargerInfo.rxInfo.tax_tp,  curTimeSec/1800);

        processChargingInfoPeriod(curTimeSec/1800);

        isChargingState = true;
        // 0, 5, 10, 15분등 정각 5분마다 전송하는 이전 값 최초값 설정
        oldChargingStatMin = c.get(Calendar.MINUTE);

        setCPStat(KepcoProtocol.CPStat.CHARGING);
    }

    public void finishCharge() {
        isChargingState = false;

        Calendar c = Calendar.getInstance();
        int curTimeSec = (c.get(Calendar.HOUR_OF_DAY)*3600)+(c.get(Calendar.MINUTE)*60) + c.get(Calendar.SECOND);

        processChargingInfoPeriod(curTimeSec/1800);
        setCPStat(KepcoProtocol.CPStat.FINISH_CHARGE);

        sendEvChargeEnd_35();

        processChargingInfoPeriod(curTimeSec/1800);
        setCPStat(KepcoProtocol.CPStat.FINISH_CONNECTING);
    }

    void localAuthProcess() {
        boolean isSuccess = false;
        // 카드테깅이고 자동인증일 경우 무조건 승인
        if ( chargerInfo.localMode == KepcoProtocol.LocalMode.CARD_TAG_AUTO && chargerInfo.cert_tp == 1) {
            isSuccess = true;
        }
        else if ( chargerInfo.localMode == KepcoProtocol.LocalMode.CARDNUM_SEARCH ) {

            if ( chargerInfo.cert_tp == 1 || chargerInfo.cert_pass.length() == 0) { // 카드테깅 혹은 번호조회일경우
                isSuccess = kepcoMember.searchMember(chargerInfo.member_card_no);
            }
            else { // 번호입력일경우 패스워드검사
                isSuccess = kepcoMember.searchMember(chargerInfo.member_card_no, chargerInfo.cert_pass);
            }
        }

        if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommAuthResult(isSuccess);
    }

    //인증 요청
    public void memberAuthReq(String membernum, int type, String passwd) {
        chargerInfo.member_card_no = membernum;
        chargerInfo.cert_tp = type;
        chargerInfo.cert_pass = passwd;

        if ( isLocalMode ) {
            androidActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            localAuthProcess();
                        }
                    }, 3000);
                }
            });
        }
        else {
            sendEvMembCert_31();
        }
    }

    // 카드 안테나 온오프 요청
    public void payTerminalAntReq(boolean onoff) {
        if ( payTerminalKind == PayTerminalKind.TL3500S) {
            if ( onoff ) tl3500s.cardInfoReq(0);
            else tl3500s.termReadyReq();
        }
        else {
            sendGlobPaymAnt_65(onoff);
        }
    }

    // 업데이트
    void createUpdateFile() {
        File parent = new File(basePath);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (firmwareOutPutStream != null) {
                firmwareOutPutStream.close();
            }
            File file = new File(basePath+ "/" + FILE_UPDATE_FIRMWARE);
            if (file.exists()) file.delete();

            file.createNewFile();

            //Create a stream to file path
            firmwareOutPutStream= new FileOutputStream(file);
        }catch (Exception e) {
            LogWrapper.e(TAG, "Create Update File Err:"+e.toString());
        }
    }

    void finishUpdateFile() {
        try {
            //Clear Stream
            firmwareOutPutStream.flush();
            //Terminate STream
            firmwareOutPutStream.close();
            firmwareOutPutStream = null;
        }catch (Exception e) {
            LogWrapper.e(TAG, "finishUpdateFile Err:"+e.toString());
        }
    }

    void updateOrFirmwareError() {
        // 카드 정보 업데이트 초기화
        if ( isCardInfoRecving ) {
            cardInfoRecvIdx = 1;
            isCardInfoRecving = false;
        }

        // 업데이트 상태 Pending
        if ( firmwareRecvingStatus == FwUpdateStatus.Recving ) {
            firmwareRecvingStatus = FwUpdateStatus.Pending;
        }
    }

    /**
     * 통신 에러 상태가 발생될때 처리
     */
    void onErrorChangeLocalMode() {
        try {
            socket.close();
            isConnected = false;
        } catch (Exception e) {
            LogWrapper.e(TAG, "onErrorChangeLocalMode:"+e.toString());
        }

        updateOrFirmwareError();

        if ( kepcoCommListener != null ) kepcoCommListener.onCommLocalMode(true);
        isLocalMode = true;
    }

    /**
     * QR 이미지 생성
     * @param ext 확장자
     */
    void createQRImageFile(String ext) {
        File parent = new File(basePath);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            if (qrOutPutStream != null) {
                qrOutPutStream .close();
            }
            File file = new File(basePath+ "/" + FILE_QR_IMG+"."+ext);
            if (file.exists()) file.delete();

            file.createNewFile();

            //Create a stream to file path
            qrOutPutStream= new FileOutputStream(file);
        }catch (Exception e) {
            LogWrapper.e(TAG, "Create QR File Err:"+e.toString());
        }
    }

    void finishQRDown() {
        try {
            //Clear Stream
            qrOutPutStream.flush();
            //Terminate STream
            qrOutPutStream.close();
            qrOutPutStream = null;
            qrRecvIdx = 0;
        }catch (Exception e) {
            LogWrapper.e(TAG, "finishQRDown Err:"+e.toString());
        }
    }

    void processRetransLostPacket() {
        int addCount = 0;
        try {
            KepcoPacket[] list = lostTransManager.getListPacket();
            if (list != null) {
                //Queue에서 해당 msgId가 없을때 add한다.
                for (int i = 0; i < list.length; i++) {
                    boolean isContain = false;
                    Iterator<KepcoPacket> itor = sendServerQueue.iterator();
                    while (itor.hasNext()) {
                        KepcoPacket packet = itor.next();
                        //packet.messageID
                        if (ByteUtil.arrayCompare(packet.messageID, 0, list[i].messageID, 0, packet.messageID.length) == true) {
                            isContain = true;
                            break;
                        }
                    }
                    if (isContain == false) {
                        sendServerQueue.addLast(list[i]);
                        addCount++;
                        if ( addCount >= RECOVERY_LOST_PKT_MAX_PER_PING ) {
                            return;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "processRetransLostPacket err:"+e.toString());
        }
    }

    public void payRequest(int payType, double kwh, int amt) {
        chargerInfo.paym_aprv_type = ""+ payType;
        chargerInfo.paym_charge_kwh = String.format("%.2f", kwh );
        chargerInfo.paym_charge_amt = ""+ amt;

        chargerInfo.pay_mthd = KepcoProtocol.PAY_METHOD_CREDIT; // 신용카드결제

        if ( payTerminalKind == PayTerminalKind.STANDARD) sendGlobChargeAmt_62();
        else if ( payTerminalKind == PayTerminalKind.TL3500S ){
            tl3500s.payReq_G(amt, 0, true, 0);
            // 카드가 꽂아져 있는 상태에서 인증 진행시 처리
            if ( tl3500CardInputState.equals("I")) kepcoCommListener.onKepcoTL3500CardEvent("I");
        }
    }

    public void payTerminalResetReq() {
        if ( payTerminalKind == PayTerminalKind.STANDARD ) {
            KepcoChargeCtl chargeCtl = new KepcoChargeCtl();
            chargeCtl.ctrl_id = PAY_TERMINAL_RESET_CTRLID;
            chargeCtl.st_datetime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss"); // 충전기 현재 시간
            chargeCtl.end_datetime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
            chargeCtl.ctrl_type = "1";
            chargeCtl.ctrl_cd = "8";

            sendGlobPaymCtl_68(chargeCtl);
        }
        else {
            tl3500s.termResetReq();
        }
    }

    //========================================================================
    // 보내는 패킷 정의
    //========================================================================

    KepcoPacket getNewSendRequestPacket() {
        return new KepcoPacket(chargerID, PROTOCOL_VERSION, rawServerIP, rawLocalIP, getNewSeqNumber());
    }

    public void sendGlobInitStart_11() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(serverAuthKey);
        data.add("0"); // 수신 Port 사용안함

        kepcoPacket.encode(GLOB_INIT_START_11.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    public void sendGlobInitEnd_12() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(serverAuthKey);

        kepcoPacket.encode(GLOB_INIT_END_12.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobStatPing_10() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();

        data.add(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss")); // 충전기 현재 시간
        data.add(""+chargerInfo.cp_stat.getID()); // 충전기 상태
        data.add(""+chargerInfo.outlet_id); // 아웃랫 ID: 1 고정
        data.add(""+chargerInfo.outlet_type.getID()); // 아웃랫 타입
        data.add(costManager.getLastVersion()); // 충전기 최종 단가일
        data.add(chargerInfo.card_date); // 회원카드 최종날짜
        data.add(chargerInfo.mng_pass); // 관리자 암호

        kepcoPacket.encode(GLOB_STAT_PING_10.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendPaymStatInfo_13() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.payRxInfo.paym_model_no);
        data.add(chargerInfo.payRxInfo.paym_fw_ver);
        data.add(chargerInfo.payRxInfo.paym_ant_stat);
        data.add(chargerInfo.payRxInfo.pam_ip);
        data.add(chargerInfo.payRxInfo.pam_gw);
        data.add(chargerInfo.payRxInfo.van_ip);
        data.add(chargerInfo.payRxInfo.van_port);
        data.add(chargerInfo.payRxInfo.m2m_fw_ver);

        data.add(chargerInfo.payRxInfo.paym_tid);
        data.add(chargerInfo.payRxInfo.svr_ip);
        data.add(chargerInfo.payRxInfo.svr_port);
        data.add(chargerInfo.payRxInfo.paym_stat);
        data.add(chargerInfo.payRxInfo.paym_update_stat);

        kepcoPacket.encode(PAYM_STAT_INFO_13.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }


    void sendGlobChargeCtl_15(KepcoProtocol.KepcoRet ret) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(""+chargerInfo.outlet_id);

        kepcoPacket.encode(GLOB_CHARGE_CTL_15.getID(), ret.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    public void sendGlobChargeCtlBegin_15(KepcoChargeCtl ctl) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(ctl.ctrl_id);
        data.add(ctl.st_datetime);
        data.add(ctl.end_datetime);
        data.add(ctl.ctrl_type);
        data.add(ctl.ctrl_cd);
        data.add(ctl.pwm_rate);
        data.add(ctl.card_no);
        data.add(ctl.outlet_id);

        kepcoPacket.encode(GLOB_CHARGE_CTL_15.getID(), PKT_BEGIN.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    public void sendGlobChargeCtlCompt_15(KepcoChargeCtl ctl, boolean isSuccess) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(ctl.ctrl_id);
        data.add(ctl.st_datetime);
        data.add(ctl.end_datetime);
        data.add(isSuccess ? "1" : "0");
        data.add(ctl.outlet_id);

        kepcoPacket.encode(GLOB_CHARGE_CTL_15.getID(), PKT_COMPT.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    // 폴트 정보 전송
    public void sendGlobFaultEvent_16(FaultInfo faultInfo, int stat) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(""+faultInfo.id);
        data.add(""+stat);
        data.add(faultInfo.occurDate);
        String endDate = "";
        if ( stat == FaultInfo.FAULT_REPAIR || stat == FaultInfo.FAULT_OCCUR_END ) endDate = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");

        data.add(endDate);
        data.add(chargerInfo.manufr); // 제조사코드 중앙제어
        data.add(""+chargerInfo.charge_tp);

        data.add(""+chargerInfo.cp_tp);
        data.add(""+faultInfo.tp);
        data.add(""+faultInfo.cd);
        data.add(""+faultInfo.errorMsg);

        kepcoPacket.encode(GLOB_FAULT_EVENT_16.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendInfoChargeUCostReq_22() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();

        data.add(chargerInfo.rxInfo.ucost_ymd); // 요청 ymd(0x10 PING)
        data.add(""+chargerInfo.charge_tp);

        kepcoPacket.encode(INFO_CHARGEUCOST_REQ_22.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobLockOpenAck_30(String reqId, String membercard, String outletId, String connType, String resultCd, String resultMsg, KepcoPacket recvPacket) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(reqId);
        data.add(membercard);
        data.add(outletId);
        data.add(connType);
        data.add(resultCd);
        data.add(resultMsg);

        kepcoPacket.encodeAck(GLOB_LOCK_OPEN_30.getID(), PKT_YES.getID(), data.toArray(new String[data.size()]), recvPacket);
        AddSendServerPacket(kepcoPacket);
    }

    void sendEvMembCert_31() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.member_card_no);
        data.add(""+chargerInfo.cert_tp);
        data.add(chargerInfo.cert_pass);

        kepcoPacket.encode(EV_MEMB_CERT_31.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendEvChargeStat_34() {
        chargerInfo.ucost_ymd = costManager.getCurrentCostInfo().version;

        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss")); // 충전기 현재 시간
        data.add(""+chargerInfo.cp_stat.getID()); // 충전기 상태
        data.add(chargerInfo.member_card_no);
        data.add(""); // 차량등록번호 Skip
        data.add("1"); // 충전타입(1:충전, 2:V2G, 3:부하제어)
        data.add(""+chargerInfo.charge_req_cfm_mthd); // 충전요구량 1:full, 2:전력량, 3:금액
        data.add(chargerInfo.charge_req_kwh == 0 ? "0" : String.format("%.2f", chargerInfo.charge_req_kwh) );
        data.add(""+chargerInfo.charge_req_amt);
        data.add(""+chargerInfo.pay_mthd);
        data.add(chargerInfo.charge_st_datetime);
        data.add(""+chargerInfo.car_soc);
        data.add(String.format("%.4f", chargerInfo.current_kwh));
        data.add(String.format("%.4f", chargerInfo.current_amt));
        data.add(String.format("%.2f",chargerInfo.current_ucost));
        data.add(""); // 전기요금 고객번호 Skip
        data.add(""); // 전기요금 청구자명 Skip
        data.add("1"); // 충전모드
        data.add(""); // 배터리상태
        data.add(""); // 배터리용량
        data.add(""); // 배터리잔량
        data.add(""); // 배터리전압
        data.add(""); // 배터리전류
        data.add("0"); // 제어량
        data.add(chargerInfo.charge_accum_time); // 누적 충전시간
        data.add(""); // 자동차종류
        data.add(""+chargerInfo.car_soc); // SOC
        data.add(""); // 충전종료 예상일시
        data.add("1"); // 아웃랫 ID: 1 고정
        data.add(""+chargerInfo.outlet_type.getID()); // 아웃랫 타입
        data.add(String.format("%.4f", costManager.getSumChargeKwh(KepcoCostManager.IDX_SUM_TOTAL)));
        data.add(String.format("%.4f", costManager.getSumChargeCost(KepcoCostManager.IDX_SUM_TOTAL)));
        data.add(chargerInfo.ucost_ymd);
        data.add(""+chargerInfo.load_cl);

        data.add("1"); // 서버통신모드
        data.add(""+chargerInfo.rxInfo.ucost_aply_tp);
        data.add(""+chargerInfo.rxInfo.tax_tp);

        kepcoPacket.encode(EV_CHARGE_STAT_34.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendEvChargeEnd_35() {
        String strCurTime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();

        //0x35메시지에서 시작 시간이 없는경우에 시작 시간 채워서 보냄(결제 실패시에주로 사용)
        if ( chargerInfo.charge_st_datetime.length() == 0 ) chargerInfo.charge_st_datetime = strCurTime;
        data.add(chargerInfo.charge_st_datetime); // 시작일시
        data.add(strCurTime); //완료일시
        data.add(""+chargerInfo.charge_tp); // 충전 유형
        data.add(String.format("%.4f", costManager.getSumChargeKwh(KepcoCostManager.IDX_SUM_TOTAL))); // 충전량 합계
        data.add(chargerInfo.charge_accum_time); // 충전 시간

        data.add(""+(int)costManager.getSumKwhCost()); // kwhBill 합계
        data.add(""+(int)costManager.getSumInfraCost()); // Infra 합계
        data.add(""+(int)costManager.getSumServiceCost()); // service 합계
        data.add(""+(int)Math.round(costManager.getTaxCost())); // tax 합계

        data.add(""+chargerInfo.pay_mthd);
        data.add(""); // 전기요금 고객번호 Skip
        data.add(""); // 전기요금 청구자명 Skip
        data.add(""+chargerInfo.charge_end_stat);
        data.add(chargerInfo.member_card_no);
        data.add(""); // 차량등록번호 Skip
        data.add(strCurTime); // 갱신일시
        data.add(chargerInfo.payRetInfo.card_aprv_no); // 카드승인번호
        data.add(""); // 차감마일리지
        data.add(""+chargerInfo.outlet_id); // 아웃랫 ID: 1 고정
        data.add(""+chargerInfo.outlet_type.getID()); // 아웃랫 타입

        data.add(String.format("%.4f", costManager.getSumChargeKwh(KepcoCostManager.IDX_SUM_MIN))); // 경부하 충전량
        data.add(String.format("%.4f", costManager.getSumChargeKwh(KepcoCostManager.IDX_SUM_MID))); // 중간부하 충전량
        data.add(String.format("%.4f", costManager.getSumChargeKwh(KepcoCostManager.IDX_SUM_MAX))); // 최대부하 충전량

        data.add(""+(int)costManager.getSumChargeCost(KepcoCostManager.IDX_SUM_MIN)); // 경부하 충전요금
        data.add(""+(int)costManager.getSumChargeCost(KepcoCostManager.IDX_SUM_MID)); // 중간부하 충전요금
        data.add(""+(int)costManager.getSumChargeCost(KepcoCostManager.IDX_SUM_MAX)); // 최대부하 충전요금

        data.add(isLocalMode ? "2" : "1"); // 서버통신모드
        data.add(""+chargerInfo.rxInfo.ucost_aply_tp);
        data.add(""+chargerInfo.rxInfo.tax_tp);

        kepcoPacket.encode(EV_CHARGE_END_35.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);

        // 충전완료후 결제 플레그 클리어
        chargerInfo.isPaymentCharging = false;
    }

    void sendGlobCardInfo_37(KepcoProtocol.KepcoRet ret) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();
        Vector<String> data = new Vector<>();

        switch ( ret )
        {
            case PKT_REQUEST: // 초기화
                cardInfoRecvIdx = 1;
            case PKT_NEXT:
                data.add(""+cardInfoRecvIdx);
                data.add(chargerInfo.card_date);
                // 브레이크문 없음. 아래로 이어짐
            case PKT_YES:
                //None
            default:
                break;
        }

        kepcoPacket.encode(GLOB_CARD_INFO_37.getID(), ret.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobCardAprv_38() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.payRetInfo.aprv_type);
        data.add(chargerInfo.payRetInfo.charge_kwh);
        data.add(chargerInfo.payRetInfo.charge_amt);
        data.add(chargerInfo.payRetInfo.pay_card_no);
        data.add(chargerInfo.payRetInfo.card_name);
        data.add(chargerInfo.payRetInfo.card_aprv_no);
        data.add(chargerInfo.payRetInfo.aprv_amt);
        data.add(chargerInfo.payRetInfo.pay_stat);
        data.add(chargerInfo.payRetInfo.pay_datetime);
        data.add(chargerInfo.payRetInfo.cncl_datetime);
        data.add(chargerInfo.payRetInfo.pay_ret_code);
        data.add(chargerInfo.payRetInfo.pay_ret_msg1);
        data.add(chargerInfo.payRetInfo.pay_ret_msg2);
        data.add(chargerInfo.payRetInfo.pay_ret_text);
        data.add(chargerInfo.payRetInfo.trade_id);
        data.add(""+chargerInfo.outlet_id);

        kepcoPacket.encode(GLOB_CARD_APRV_38.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobPaymStat_61() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.rxInfo.paym_model_no);
        data.add(chargerInfo.rxInfo.paym_fw_ver);
        data.add(chargerInfo.rxInfo.paym_ant_stat);
        data.add(chargerInfo.rxInfo.pam_ip);
        data.add(chargerInfo.rxInfo.pam_gw);
        data.add(chargerInfo.rxInfo.van_ip);
        data.add(chargerInfo.rxInfo.van_port);
        data.add(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss"));
        data.add(chargerInfo.rxInfo.m2m_fw_ver);

        kepcoPacket.encode(GLOB_PAYM_STAT_61.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);

        //ack 검사
        timerPaymStatAckCnt = 10;
    }

    void sendGlobChargeAmt_62() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.paym_aprv_type);
        data.add(chargerInfo.paym_charge_kwh);
        data.add(chargerInfo.paym_charge_amt);

        kepcoPacket.encode(GLOB_CHARGE_AMT_62.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobPaymComm_64() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(chargerInfo.rxInfo.paym_fw_ver);
        data.add(chargerInfo.rxInfo.pam_ip);
        data.add(chargerInfo.rxInfo.pam_gw);
        data.add(chargerInfo.rxInfo.van_ip);
        data.add(chargerInfo.rxInfo.van_port);
        data.add(chargerInfo.rxInfo.paym_tid);
        data.add(chargerInfo.rxInfo.svr_ip);
        data.add(chargerInfo.rxInfo.svr_port);
        kepcoPacket.encode(GLOB_PAYM_COMM_64.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    /**
     * 안테나 On/Off
     * @param onoff
     */
    void sendGlobPaymAnt_65(boolean onoff) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(onoff ? "1" : "0");
        kepcoPacket.encode(GLOB_PAYM_ANT_65.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));

        // 결제 단말기쪽으로 패킷 전송
        AddSendServerPacket(kepcoPacket);
    }

    /**
     * 결제단말기 무결성 확인
     * @param isOnBoot 부팅시 true
     */
    public void sendGlobPaymInty_66(boolean isOnBoot) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(isOnBoot ? "1" : "2");
        kepcoPacket.encode(GLOB_PAYM_INTY_66.getID(), PKT_REQUEST.getID(), data.toArray(new String[data.size()]));

        // 결제 단말기쪽으로 패킷 전송
        AddSendServerPacket(kepcoPacket);
    }

    public void sendGlobPaymCtl_68(KepcoChargeCtl chargeCtl) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();

        data.add(chargeCtl.ctrl_id);
        data.add(chargeCtl.st_datetime);
        data.add(chargeCtl.end_datetime);
        data.add(chargeCtl.ctrl_type);
        data.add(chargeCtl.ctrl_cd);
        kepcoPacket.encode(GLOB_PAYM_CTRL_68.getID(), PKT_PUSH.getID(), data.toArray(new String[data.size()]));

        // 결제 단말기쪽으로 패킷 전송
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobFirmUpdate_71(KepcoProtocol.KepcoRet ret) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();
        Vector<String> data = new Vector<>();

        switch ( ret )
        {
            case PKT_REQUEST: // 초기화
            case PKT_NEXT:
                data.add(""+firmwareRecvIdx);
                data.add(chargerInfo.rxInfo.firmVersion);
                // 브레이크문 없음. 아래로 이어짐
            case PKT_YES:
                //None
            default:
                break;
        }

        kepcoPacket.encode(GLOB_FIRM_UPDATE_71.getID(), ret.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobCpConfig_72() {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();
        data.add(""+chargerInfo.charge_tp);
        data.add(""+chargerInfo.cp_tp);
        data.add(chargerInfo.manufr);
        data.add(chargerInfo.model_nm);
        data.add(chargerInfo.serial_no);
        data.add(""+chargerInfo.trnas_tp);
        data.add(""+chargerInfo.send_port);
        data.add("0"); // 수신포트 (사용안함)
        data.add(""+chargerInfo.net_type);
        data.add(""+chargerInfo.charge_ability);
        data.add(chargerInfo.cp_firmware_ver);
        data.add(chargerInfo.cp_ip);
        data.add(chargerInfo.paym_manufr);
        data.add(chargerInfo.rxInfo.paym_model_no);
        data.add(chargerInfo.rxInfo.paym_fw_ver);
        data.add(chargerInfo.rxInfo.pam_ip);
        data.add(usePayTerminal ? "Y" :"N");
        data.add(chargerInfo.board_ver);
        data.add("1"); // 아웃렛 수량

        kepcoPacket.encode(GLOB_CP_CONFIG_72.getID(), PKT_DATA.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    void sendGlobQRImgDown_73(KepcoProtocol.KepcoRet ret) {
        KepcoPacket kepcoPacket = getNewSendRequestPacket();

        Vector<String> data = new Vector<>();

        switch ( ret )
        {
            case PKT_REQUEST: // 초기화
                qrRecvIdx = 1; // Index 초기화
            case PKT_NEXT:
                data.add(""+qrRecvIdx);
                data.add("QR"); //FILE_TP ("QR")
                // 브레이크문 없음. 아래로 이어짐
            case PKT_YES:
                //None
            default:
                break;
        }

        kepcoPacket.encode(GLOB_QRIMG_DOWN_73.getID(), ret.getID(), data.toArray(new String[data.size()]));
        AddSendServerPacket(kepcoPacket);
    }

    //========================================================================
    // 받는 패킷 정의
    //========================================================================

    void onGlobStatPing_10(KepcoPacket recvPacket) {
        int idx = 0;

        String ucost_ymd = recvPacket.getDataAt(idx++); // 단가일자
        String ucost_change_yn = recvPacket.getDataAt(idx++); // 단가변경여부
        String ctrn_yn = recvPacket.getDataAt(idx++); // 충전기제어여부
        String cp_firmware_ver = recvPacket.getDataAt(idx++); // 버전
        String paym_firm_ver =  recvPacket.getDataAt(idx++); // 버전(결제 단말기)
        String card_date =  recvPacket.getDataAt(idx++); // 회원카드 최종일시 // 카드 정보 업데이트

        String local_mode = recvPacket.getDataAt(idx++);
        String timeStamp = recvPacket.getDataAt(idx++); // 기준시각
        String base_paym_amt = recvPacket.getDataAt(idx++); // 선결제 기준금액 // 무조건 입력함. 사용안함

        String ucost_aply_tp = recvPacket.getDataAt(idx++);
        String tax_tp = recvPacket.getDataAt(idx++);
        String mng_pass = recvPacket.getDataAt(idx++); // 관리자 모드 패스워드
        String aprv_cd = recvPacket.getDataAt(idx++); // 결제승인 거래구분 // 2019년 이후 PG만 사용함 사용안함

        chargerInfo.rxInfo.ucost_ymd = ucost_ymd;
        chargerInfo.rxInfo.ucost_aply_tp = Integer.parseInt(ucost_aply_tp);
        chargerInfo.rxInfo.tax_tp = Integer.parseInt(tax_tp);
        chargerInfo.mng_pass = mng_pass;
        KepcoProtocol.LocalMode newMode = local_mode.equals("1") ? KepcoProtocol.LocalMode.CARDNUM_SEARCH : KepcoProtocol.LocalMode.CARD_TAG_AUTO;
        if ( chargerInfo.localMode != newMode ) {
            chargerInfo.localMode = newMode;
            // 로컬모드 저장
            FileUtil.stringToFile(basePath + "/", FILE_CONFIG_LOCALMODE, local_mode, false);
        }


        if ( ucost_ymd.length() > 0 && ucost_ymd.compareTo(costManager.getLastVersion()) > 0 || ucost_change_yn.equals("1") == true ) {
            sendInfoChargeUCostReq_22();
        }

        if (cp_firmware_ver.length() > 0 && cp_firmware_ver.compareTo(firmVersion) > 0 ) {
            if ( firmwareRecvingStatus == FwUpdateStatus.None || !cp_firmware_ver.equals(chargerInfo.rxInfo.firmVersion)) {
                chargerInfo.rxInfo.firmVersion = cp_firmware_ver;
                // 파일 준비
                createUpdateFile();
                firmwareRecvingStatus = FwUpdateStatus.Recving;
                firmwareRecvIdx = 1;
                sendGlobFirmUpdate_71(PKT_REQUEST);
            } else if ( firmwareRecvingStatus == FwUpdateStatus.Pending) {
                chargerInfo.rxInfo.firmVersion = cp_firmware_ver;
                if ( firmwareRecvIdx == 1 ) {
                    createUpdateFile();
                    sendGlobFirmUpdate_71(PKT_REQUEST);
                }
                else {
                    sendGlobFirmUpdate_71(PKT_NEXT);
                }
                firmwareRecvingStatus = FwUpdateStatus.Recving;
            }
        }
        chargerInfo.rxInfo.firmVersion = cp_firmware_ver;

        if ( firmwareRecvingStatus == FwUpdateStatus.None) {
            if ((card_date.length() > 0 && card_date.compareTo(chargerInfo.card_date) > 0) || chargerInfo.card_date.length() == 0) {
                if (!isCardInfoRecving || !card_date.equals(chargerInfo.rxInfo.card_date)) {
                    isCardInfoRecving = true;
                    cardInfoRecvIdx = 1;
                    sendGlobCardInfo_37(PKT_REQUEST);
                }
            } else if (card_date.length() == 0) { // NULL 인경우 초기화
                //  초기화를 하고 Date를 NULL로 만든다.
                kepcoMember.eraseAll();
                chargerInfo.card_date = "";
                sendGlobCardInfo_37(PKT_REQUEST);
            }
            chargerInfo.rxInfo.card_date = card_date;
        }

        // 충전기 제어가 있는경우 0x15를 모낸다.
        if (ctrn_yn.equals("1")) {
            sendGlobChargeCtl_15(PKT_REQUEST);
        }

        if ( kepcoCommListener != null ) {
            kepcoCommListener.onTimeUpdate(timeStamp);
            kepcoCommListener.onOnlineModeAndPwdUpdate(mng_pass);
        }
        isLocalMode = false;

        processRetransLostPacket();

        timerPaymStatCnt = KepcoProtocol.PING_MSG_PERIOD - 1; // PaymStat를 Ping 메시지 뒤 1초후 전송
    }

    void onGlobInitStart_11(KepcoPacket recvPacket) {
        int idx = 0;
        String timeStamp = recvPacket.getDataAt(idx++); // 기준시각
        String firmware = recvPacket.getDataAt(idx++); // 펌웨어 정보

        if ( kepcoCommListener != null ) kepcoCommListener.onTimeUpdate(timeStamp);
    }

    void onPaymStatInfo_13(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            chargerInfo.rxInfo.paym_model_no = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.paym_fw_ver = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.pam_ip = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.pam_gw = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.van_ip = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.van_port = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.paym_setup_yn = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.m2m_fw_ver = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.paym_tid = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.svr_ip = recvPacket.getDataAt(idx++);
            chargerInfo.rxInfo.svr_port = recvPacket.getDataAt(idx++);

            if ( chargerInfo.rxInfo.paym_setup_yn.toLowerCase().equals("y") ) {
                if ( payTerminalKind == PayTerminalKind.STANDARD) sendGlobPaymComm_64();
                else if ( payTerminalKind == PayTerminalKind.TL3500S ) sendConfigTL3500();
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "onPaymStatInfo_13 err:" + e.toString());
        }
    }

    void onGlobChargeCtl_15(KepcoPacket recvPacket) {
        int idx = 0;

        // Push인경우에 request전송
        if ( recvPacket.ret == PKT_PUSH.getID() ) {
            sendGlobChargeCtl_15(PKT_REQUEST);
        }
        else if (recvPacket.ret == PKT_DATA.getID() ) {
            KepcoChargeCtl chargeCtl = new KepcoChargeCtl();

            chargeCtl.ctrl_id = recvPacket.getDataAt(idx++);
            chargeCtl.st_datetime = recvPacket.getDataAt(idx++);
            chargeCtl.end_datetime = recvPacket.getDataAt(idx++);
            chargeCtl.ctrl_type = recvPacket.getDataAt(idx++);
            chargeCtl.ctrl_cd = recvPacket.getDataAt(idx++);
            chargeCtl.pwm_rate = recvPacket.getDataAt(idx++);
            chargeCtl.card_no = recvPacket.getDataAt(idx++);
            chargeCtl.outlet_id = recvPacket.getDataAt(idx++);

            chargeCtlList.add(chargeCtl);
        }
        else if (recvPacket.ret == PKT_YES.getID() ) {
            //Nothing
        }
    }

    void onChargeUCostReq_22(KepcoPacket recvPacket) {
        if (recvPacket.ret != PKT_DATA.getID()) return;

        int idx = 0;

        String ymd = recvPacket.getDataAt(idx++); // 단가일자
        String charge_tp = recvPacket.getDataAt(idx++); // 충전기 타입

        double[] kwhbill_ucost = new double[48];
        double[] infrabill_ucost = new double[48];
        double[] svcbill_ucost = new double[48];
        int[] load_cl = new int[48];

        try {
            for (int i = 0; i < 48; i++) {
                idx++; // 시간
                idx++; // 인터벌 번호
                kwhbill_ucost[i] = Double.parseDouble(recvPacket.getDataAt(idx++));
                infrabill_ucost[i] = Double.parseDouble(recvPacket.getDataAt(idx++));
                svcbill_ucost[i] = Double.parseDouble(recvPacket.getDataAt(idx++));
                load_cl[i] = Integer.parseInt(recvPacket.getDataAt(idx++));
            }
            costManager.saveKepcoCostTable(ymd, kwhbill_ucost, infrabill_ucost, svcbill_ucost, load_cl);
        } catch (Exception e ) {
            LogWrapper.e(TAG, "onChargeUCostReq_22 e:"+e.toString());
        }
        costManager.updateCurrentCostTable();
    }

    void onGlobLockOpen_30(KepcoPacket recvPacket) {
        int idx = 0;
        String reqId = recvPacket.getDataAt(idx++);
        String membercard = recvPacket.getDataAt(idx++);
        String outletId = recvPacket.getDataAt(idx++);
        String connType = recvPacket.getDataAt(idx++);

        String resultCd = "1";// 디폴트 정상
        String resultMsg = "정상LOCK 오픈";
        try {
            if (chargerInfo.cp_stat != KepcoProtocol.CPStat.READY || chargerInfo.outlet_id != Integer.parseInt(outletId)) {
                resultCd = "2";
                resultMsg = "해당 아웃렛ID 사용불가";
            } else if (chargerInfo.charge_tp == KepcoProtocol.CHARGER_TYPE_SLOW && !connType.matches("1|2")) {
                resultCd = "5";
                resultMsg = "해당 아웃렛타입 없음";

            } else if (chargerInfo.charge_tp == KepcoProtocol.CHARGER_TYPE_FAST && !connType.matches("3|4|5")) {
                resultCd = "5";
                resultMsg = "해당 아웃렛타입 없음";
            }
            else {
                chargerInfo.member_card_no = membercard;
                boolean ret = false;
                if ( kepcoCommListener != null ) ret = kepcoCommListener.onRemoteAuthReq(connType);
                if ( ret == false ) {
                    chargerInfo.member_card_no = "";
                    resultCd = "2";
                    resultMsg = "해당 아웃렛ID 사용불가";
                }
            }
            sendGlobLockOpenAck_30(reqId, membercard, outletId, connType, resultCd, resultMsg, recvPacket);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onGlobLockOpen_30 e:"+e.toString());
        }
    }

    void onEvMembCert_31(KepcoPacket recvPacket) {
        boolean ret = false;
        if (recvPacket.ret == KepcoProtocol.KepcoRet.PKT_NOT.getID()) {
            ret = false;
        }
        else {
            ret = true;
        }

        if ( kepcoCommListener != null ) kepcoCommListener.onKepcoCommAuthResult(ret);
    }

    void onEVChargeStat_34(KepcoPacket recvPacket) {
        if ( isLocalMode ) {
            isLocalMode = false;
            if (kepcoCommListener != null) kepcoCommListener.onCommLocalMode(false);
        }

        if ( recvPacket.ret == PKT_YES.getID() ) {
            int idx = 0;
            String outletId = recvPacket.getDataAt(idx++);
            String timestamp = recvPacket.getDataAt(idx++);
            String ucost_ymd = recvPacket.getDataAt(idx++);
            String ucost_change_yn = recvPacket.getDataAt(idx++);
            String ctrn_yn = recvPacket.getDataAt(idx++);
            String card_date = recvPacket.getDataAt(idx++);

            chargerInfo.rxInfo.ucost_ymd = ucost_ymd;
            if ( ucost_ymd.length() > 0 && ucost_ymd.compareTo(costManager.getLastVersion()) > 0 || ucost_change_yn.equals("1") == true ) {
                sendInfoChargeUCostReq_22();
            }

            // 충전기 제어가 있는경우 0x15를 모낸다.
            if (ctrn_yn.equals("1")) {
                sendGlobChargeCtl_15(PKT_REQUEST);
            }
        }
    }

    void onEvMembCard_36(KepcoPacket recvPacket) {
        try {
            if (kepcoCommListener != null)
                kepcoCommListener.onKepcoReadCardNum(recvPacket.getDataAt(0));
        } catch(Exception e) {
            LogWrapper.e(TAG, "onEvMembCard_36 e:"+e.toString());
        }
    }

    void onGlobCardInfo_37(KepcoPacket recvPacket) {
        int idx = 0;

        if ( recvPacket.ret == PKT_NOT.getID() ) return;

        try {
            cardInfoRecvIdx = Integer.parseInt(recvPacket.getDataAt(idx++));
            int infoNum = Integer.parseInt(recvPacket.getDataAt(idx++));
            String[] listCardNo = new String[infoNum];
            String[] listPwd = new String[infoNum];
            String[] listAddCl = new String[infoNum];

            for (int i = 0; i < infoNum; i++) {
                listCardNo[i] = recvPacket.getDataAt(idx++);
                listPwd[i] = recvPacket.getDataAt(idx++);
                listAddCl[i] = recvPacket.getDataAt(idx++);
            }

            if (infoNum > 0) {
                kepcoMember.addMembers(listCardNo, listPwd, listAddCl, infoNum);
            }

            cardInfoRecvIdx++;

            if (recvPacket.ret == PKT_DATA.getID()) {
                sendGlobCardInfo_37(PKT_NEXT);
            } else if (recvPacket.ret == PKT_COMPT.getID()) {
                cardInfoRecvIdx = 1;
                isCardInfoRecving = false;
                chargerInfo.card_date = chargerInfo.rxInfo.card_date;
                if (chargerInfo.card_date.length() == 0) {
                    chargerInfo.card_date = TimeUtil.getCurrentTimeAsString("yyyyMMddHHmmss");
                }
                // 버전 정보 저장
                FileUtil.stringToFile(basePath + "/", FILE_CONFIG_CARDDATE, chargerInfo.card_date, false);
                sendGlobCardInfo_37(PKT_YES);
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onGlobCardInfo_37 err:"+e.toString());
        }
    }

    void onGlobPaymStat_61(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            chargerInfo.payRxInfo.paym_model_no = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_fw_ver = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_ant_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.pam_ip = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.pam_gw = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.van_ip = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.van_port = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_update_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.m2m_fw_ver = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_tid = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.svr_ip = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.svr_port = recvPacket.getDataAt(idx++);

            sendPaymStatInfo_13();
        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobPaymStat_61 err:" + e.toString());
        }

        timerPaymStatAckCnt = 0;
        timerPaymStatErrCnt = 0;
        if ( isPaymStatErr ) {
            isPaymStatErr = false;
            if ( kepcoCommListener != null ) kepcoCommListener.onPaymStatErr(false);
        }
    }

    void onGlobChargeAmt_62(KepcoPacket recvPacket) {
        int idx = 0;
        if (recvPacket.ret == PKT_YES.getID()) {

            try {
                chargerInfo.payRetInfo.aprv_type = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.charge_kwh = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.charge_amt = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_card_no = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.card_name = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.card_aprv_no = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.aprv_amt = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_stat = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_datetime = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.cncl_datetime = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_ret_code = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_ret_msg1 = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_ret_msg2 = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.pay_ret_text = recvPacket.getDataAt(idx++);
                chargerInfo.payRetInfo.trade_id = recvPacket.getDataAt(idx++);

                // 0x34 카드 번호를 신용카드 번호로 대체
                chargerInfo.member_card_no = chargerInfo.payRetInfo.pay_card_no;

                KepcoPacket kepcoPacket = getNewSendRequestPacket();
                Vector<String> data = new Vector<>();

                kepcoPacket.encodeAck(GLOB_CHARGE_AMT_62.getID(), PKT_ACK.getID(), null, recvPacket);
                AddSendServerPacket(kepcoPacket);

                sendGlobCardAprv_38();

                // 안테나를 OFF한다.
                sendGlobPaymAnt_65(false);

                // 결제 충전 플래그 세팅
                chargerInfo.isPaymentCharging = true;

                if ( kepcoCommListener != null ) kepcoCommListener.onPayResult(chargerInfo.payRetInfo);
            } catch (Exception e) {
                LogWrapper.e(TAG, "onGlobChargeAmt_62 err:" + e.toString());
            }
        }
    }

    void onGlobPaymAnt_65(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            chargerInfo.rxInfo.paym_ant_stat = recvPacket.getDataAt(idx++);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobPaymAnt_65 err:" + e.toString());
        }
    }

    void onGlobPaymInty_66(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            for (int i=0; i<5; i++) {
                String recv = recvPacket.getDataAt(idx++);
                byte[] raw = recv.getBytes();
                chargerInfo.payRxInfo.paym_inty_chk_ret[i] = raw[0] == 0x06 ? "정상" : "무결성 오류";
                recv = recvPacket.getDataAt(idx++);
                raw = recv.getBytes();
                chargerInfo.payRxInfo.paym_inty_chk_date[i] = ByteUtil.bcdToString(raw);
                recv = recvPacket.getDataAt(idx++);
                raw = recv.getBytes();
                chargerInfo.payRxInfo.paym_inty_chk_type[i] = raw[0] == 0x01 ? "부팅시 검사" : "사용자 검사";
            }
            chargerInfo.payRxInfo.paym_reg_model = recvPacket.getDataAt(idx++);

        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobPaymInty_66 err:" + e.toString());
        }
    }

    void onGlobPaymStatSend_67(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            chargerInfo.payRxInfo.paym_tid = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_ant_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_update_stat = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.paym_fw_ver = recvPacket.getDataAt(idx++);
            chargerInfo.payRxInfo.m2m_fw_ver = recvPacket.getDataAt(idx++);

            KepcoPacket kepcoPacket = getNewSendRequestPacket();
            Vector<String> data = new Vector<>();

            kepcoPacket.encodeAck(GLOB_PAYM_STAT_SEND_67.getID(), PKT_YES.getID(), null, recvPacket);
            AddSendServerPacket(kepcoPacket);

        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobPaymStatSend_67 err:" + e.toString());
        }
    }

    void onGlobPaymCtl_68(KepcoPacket recvPacket) {
        int idx = 0;

        if ( recvPacket.ret == PKT_ACK.getID() ) return;

        try {
            KepcoChargeCtl chargeCtl = new KepcoChargeCtl();

            chargeCtl.ctrl_id = recvPacket.getDataAt(idx++);
            chargeCtl.ctrl_id = ""+Long.parseLong(chargeCtl.ctrl_id);
            chargeCtl.st_datetime = recvPacket.getDataAt(idx++);
            chargeCtl.end_datetime = recvPacket.getDataAt(idx++);
            if ( recvPacket.ret == PKT_BEGIN.getID()) {
                chargeCtl.ctrl_type = recvPacket.getDataAt(idx++);
                chargeCtl.ctrl_cd = recvPacket.getDataAt(idx++);
                // 자체 리셋이 아닌경우에 서버로 전송
                if ( chargeCtl.ctrl_id.equals(PAY_TERMINAL_RESET_CTRLID) == false ) {
                    chargeCtl.outlet_id = ""+chargerInfo.outlet_id; // 충전기 outlet
                    sendGlobChargeCtlBegin_15(chargeCtl);
                }
            }
            else if ( recvPacket.ret == PKT_COMPT.getID()) {
                String ctrlyn = recvPacket.getDataAt(idx++);
                // 자체 리셋이 아닌경우에 서버로 전송
                if ( chargeCtl.ctrl_id.equals(PAY_TERMINAL_RESET_CTRLID) == false ) {
                    chargeCtl.outlet_id = ""+chargerInfo.outlet_id; // 충전기 outlet
                    sendGlobChargeCtlCompt_15(chargeCtl, ctrlyn.equals("1"));
                }
            }

            KepcoPacket kepcoPacket = getNewSendRequestPacket();
            Vector<String> data = new Vector<>();

            kepcoPacket.encodeAck(GLOB_PAYM_CTRL_68.getID(), PKT_YES.getID(), null, recvPacket);
            AddSendServerPacket(kepcoPacket);

        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobPaymCtl_68 err:" + e.toString());
        }
    }

    void onGlobFirmUpdate_71(KepcoPacket recvPacket) {
        int idx = 0;

        try {
            String strFwSize = recvPacket.getDataAt(idx++);
            String strSegNo = recvPacket.getDataAt(idx++);
            String strFwVer = recvPacket.getDataAt(idx++);

            int segNo = Integer.parseInt(strSegNo);
            if (segNo != firmwareRecvIdx) {
                sendGlobFirmUpdate_71(PKT_NEXT);
                return;
            }

            int headerSize = strFwSize.length() + strSegNo.length() + strFwVer.length() + 3;

            firmwareOutPutStream.write(recvPacket.vdDataRaw, headerSize, recvPacket.vdDataRaw.length - headerSize);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobFirmUpdate_71 err:" + e.toString());
        }

        LogWrapper.d(TAG, "Recv Update Idx:" + firmwareRecvIdx);

        firmwareRecvIdx++;
        if (recvPacket.ret == PKT_DATA.getID()) {
            sendGlobFirmUpdate_71(PKT_NEXT);
        } else if (recvPacket.ret == PKT_COMPT.getID()) {
            sendGlobFirmUpdate_71(PKT_YES);
            finishUpdateFile();
            firmwareRecvingStatus = FwUpdateStatus.Finished;

            if (kepcoCommListener != null) kepcoCommListener.onFinishUpdateDownload();
        }
    }

    void onGlobQRImgDown_73(KepcoPacket recvPacket) {
        int idx = 0;
        String strFileExt = ".png";

        if (recvPacket.ret == PKT_NOT.getID() || recvPacket.ret == PKT_FAIL.getID() ) return;

        try {
            String strFileSize = recvPacket.getDataAt(idx++);
            String strSegNo = recvPacket.getDataAt(idx++);
            String strFileTp = recvPacket.getDataAt(idx++);
            strFileExt = recvPacket.getDataAt(idx++);
            String strImgPx = recvPacket.getDataAt(idx++);

            int segNo = Integer.parseInt(strSegNo);
            if (segNo != qrRecvIdx) {
                sendGlobQRImgDown_73(PKT_NEXT);
                return;
            }
            if ( qrRecvIdx == 1 ) createQRImageFile(strFileExt);

            int headerSize = strFileSize.length() + strSegNo.length() + strFileTp.length() + strFileExt.length()+strImgPx.length()+5;

            qrOutPutStream.write(recvPacket.vdDataRaw, headerSize, recvPacket.vdDataRaw.length - headerSize);
        } catch (Exception e) {
            LogWrapper.e(TAG, "onGlobQRImgDown_73 err:" + e.toString());
        }

        LogWrapper.d(TAG, "Recv QR Idx:" + qrRecvIdx);

        qrRecvIdx++;

        if (recvPacket.ret == PKT_DATA.getID()) {
            sendGlobQRImgDown_73(PKT_NEXT);
        } else if (recvPacket.ret == PKT_COMPT.getID()) {
            sendGlobQRImgDown_73(PKT_YES);

            finishQRDown();

            if (kepcoCommListener != null) kepcoCommListener.onQRImageDownFinish(basePath+ "/" + FILE_QR_IMG+"."+strFileExt);
        }
    }

    //=============================================
    // TL3500 Callback code
    //=============================================
    @Override
    public void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch) {
        switch (type)
        {
            case Check:
                //onTL3500S_Check(retVal, ch);
                break;

            case Search:
                onTL3500S_Search(retVal, ch);
                break;

            case Pay:
                onTL3500S_Pay(retVal, ch);
                break;

            case CancelPay:
                //onTL3500S_CancelPay(retVal, ch);
                break;

            case Error:
                //onTL3500S_Error(retVal, ch);
                break;

            case Event:
                onTL3500S_Event(retVal, ch);
                break;

            case GetVersion:
                onTL3500S_GetVersion(retVal);
                break;

            case GetConfig:
                onTL3500S_GetConfig(retVal);
                break;
        }
    }

    void sendConfigTL3500() {
        // 내부 저장 정보랑 비교하여 같으면 재세팅하지 않는다.
        if ( chargerInfo.rxInfo.pam_ip.equals(chargerInfo.payRxInfo.pam_ip) &&
             chargerInfo.rxInfo.pam_gw.equals(chargerInfo.payRxInfo.pam_gw) &&
             chargerInfo.rxInfo.van_ip.equals(chargerInfo.payRxInfo.van_ip) &&
             chargerInfo.rxInfo.van_port.equals(chargerInfo.payRxInfo.van_port) &&
             chargerInfo.rxInfo.paym_tid.equals(chargerInfo.payRxInfo.paym_tid) ) return;

        tl3500s.setConfigReq(
                chargerInfo.rxInfo.paym_tid,
                chargerInfo.rxInfo.van_ip,
                chargerInfo.rxInfo.van_port,
                chargerInfo.rxInfo.pam_ip,
                chargerInfo.rxInfo.pam_gw
                );
    }

    void onTL3500S_GetVersion(Map<String, String> retVal) {
        timerPaymStatErrCnt = 0;
        if ( isPaymStatErr ) {
            isPaymStatErr = false;
            if ( kepcoCommListener != null ) kepcoCommListener.onPaymStatErr(false);
        }

        chargerInfo.payRxInfo.paym_model_no = "TL-3500BS";
        chargerInfo.payRxInfo.paym_fw_ver = retVal.get("version");
        tl3500s.getConfigReq();
    }

    void onTL3500S_GetConfig(Map<String, String> retVal) {
        try {
            chargerInfo.payRxInfo.paym_ant_stat = "0";
            chargerInfo.payRxInfo.pam_ip = retVal.get("ip");
            chargerInfo.payRxInfo.pam_gw = retVal.get("gateway");
            chargerInfo.payRxInfo.van_ip = retVal.get("van_ip");
            chargerInfo.payRxInfo.van_port = retVal.get("van_port");
            chargerInfo.payRxInfo.paym_stat = "1";
            chargerInfo.payRxInfo.paym_update_stat = "1";
            chargerInfo.payRxInfo.m2m_fw_ver = "0";
            chargerInfo.payRxInfo.paym_tid = retVal.get("mid");
            chargerInfo.payRxInfo.svr_ip = serverIP;
            chargerInfo.payRxInfo.svr_port = ""+serverPort;

            sendPaymStatInfo_13();
        } catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_GetConfig err:" + e.toString());
        }
    }

    void onTL3500S_Event(Map<String, String> retVal, int ch) {
        if ( retVal.get("event") != null ) {
            kepcoCommListener.onKepcoTL3500CardEvent(retVal.get("event"));

            if (retVal.get("event").matches("[IO]")) tl3500CardInputState = retVal.get("event");
        }
    }

    void onTL3500S_Search(Map<String, String> retVal, int ch) {
        String cnum = retVal.get("cardnum").substring(4);

        LogWrapper.d(TAG, "onTL3500S_Search: cardnum:"+cnum);

        kepcoCommListener.onKepcoReadCardNum(cnum);
    }

    void onTL3500S_Pay(Map<String, String> retVal, int ch) {
        try {
            chargerInfo.payRetInfo.aprv_type = chargerInfo.paym_aprv_type;
            chargerInfo.payRetInfo.charge_kwh = chargerInfo.paym_charge_kwh;

            String cardNum = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거

            chargerInfo.payRetInfo.pay_card_no = cardNum; // 카드번호
            chargerInfo.payRetInfo.card_name = "";

            if ( retVal.get("payCode").equals("1")) { // 정상 승인
                chargerInfo.payRetInfo.charge_amt = retVal.get("totalCost").replace(" ", "");
                chargerInfo.payRetInfo.pay_stat = "01"; // 정상

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                Date payDate = sdf.parse(retVal.get("payDate")+retVal.get("payTime"));

                chargerInfo.payRetInfo.pay_datetime = TimeUtil.getDateAsString("yyyy-MM-dd HH:mm:ss", payDate);

                chargerInfo.payRetInfo.cncl_datetime = "";
                chargerInfo.payRetInfo.trade_id = retVal.get("pgnum").replace(" ", "");
                chargerInfo.payRetInfo.card_aprv_no = retVal.get("authNum").replace(" ", ""); // 승인번호
                chargerInfo.payRetInfo.aprv_amt = chargerInfo.payRetInfo.charge_amt;

                // 0x34 카드 번호를 신용카드 번호로 대체
                chargerInfo.member_card_no = chargerInfo.payRetInfo.pay_card_no;
            }
            else if (retVal.get("payCode").equals("X")){
                chargerInfo.payRetInfo.charge_amt = chargerInfo.paym_charge_amt;
                chargerInfo.payRetInfo.aprv_amt = chargerInfo.paym_charge_amt;
                chargerInfo.payRetInfo.pay_stat = "02"; // 실패
                chargerInfo.payRetInfo.pay_ret_code = retVal.get("retCode");
                chargerInfo.payRetInfo.pay_ret_msg1 = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();
            }

            sendGlobCardAprv_38();

            // 결제 충전 플래그 세팅
            chargerInfo.isPaymentCharging = true;

            if (kepcoCommListener != null) kepcoCommListener.onPayResult(chargerInfo.payRetInfo);
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_Pay:"+e.toString());
        }
    }

    //=============================================
    // Test Comm
    //=============================================
    public boolean sendTestCmd(KepcoProtocol.KepcoCmd cmd) {
        boolean ret = true;
        switch (cmd) {
            case GLOB_STAT_PING_10:
                sendGlobStatPing_10();
                break;
            case GLOB_INIT_START_11:
                sendGlobInitStart_11();
                break;
            case GLOB_INIT_END_12:
                sendGlobInitEnd_12();
                break;
            case PAYM_STAT_INFO_13:
                sendPaymStatInfo_13();
                break;
            case GLOB_CHARGE_CTL_15:
                sendGlobChargeCtl_15(PKT_REQUEST);
                break;
            case GLOB_FAULT_EVENT_16:
                FaultInfo info = new FaultInfo();
                sendGlobFaultEvent_16(info, FaultInfo.FAULT_OCCUR_END);
                break;
            case INFO_CHARGEUCOST_REQ_22:
                sendInfoChargeUCostReq_22();
                break;
            case EV_MEMB_CERT_31:
                sendEvMembCert_31();
                break;
            case EV_CHARGE_STAT_34:
                sendEvChargeStat_34();
                break;
            case EV_CHARGE_END_35:
                sendEvChargeEnd_35();
                break;
            case GLOB_CARD_INFO_37:
                sendGlobCardInfo_37(PKT_REQUEST);
                break;
            case GLOB_CARD_APRV_38:
                sendGlobCardAprv_38();
                break;
            case GLOB_PAYM_STAT_61:
                sendGlobPaymStat_61();
                break;
            case GLOB_CHARGE_AMT_62:
                sendGlobChargeAmt_62();
                break;
            case GLOB_PAYM_COMM_64:
                sendGlobPaymComm_64();
                break;
            case GLOB_PAYM_ANT_65:
                sendGlobPaymAnt_65(false);
                break;
            case GLOB_PAYM_INTY_66:
                sendGlobPaymInty_66(false);
                break;
            case GLOB_PAYM_STAT_SEND_67:
                break;
            case GLOB_PAYM_CTRL_68:
                payTerminalResetReq();
                break;
            case GLOB_FIRM_UPDATE_71:
                sendGlobFirmUpdate_71(PKT_REQUEST);
                break;
            case GLOB_CP_CONFIG_72:
                sendGlobCpConfig_72();
                break;
            case GLOB_QRIMG_DOWN_73:
                sendGlobQRImgDown_73(PKT_REQUEST);
                break;
            default:
                ret = false;
                break;
        }
        return ret;
    }
}
