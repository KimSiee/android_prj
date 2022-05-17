/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvComm;
import com.joas.evcomm.EvCommManager;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.hw.payment.tl3500s.TL3500SListener;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderACM1281S;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.ocpp.chargepoint.OCPPSession;
import com.joas.ocpp.chargepoint.OCPPSessionManager;
import com.joas.ocpp.chargepoint.OCPPSessionManagerListener;
import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.ocpp.msg.TriggerMessage;
import com.joas.ocpp.stack.OCPPDiagnosticManager;
import com.joas.ocpp.stack.OCPPStackProperty;
import com.joas.ocpp.stack.OCPPTransportMonitorListener;
import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_comm.PoscoCommManager;
import com.joas.posco_comm.PoscoCommManagerListener;
import com.joas.posco_comm.PoscoModemCommManager;
import com.joas.posco_comm.PoscoModemCommManagerListener;
import com.joas.posco_slow_charlcd.page.PageEvent;
import com.joas.posco_slow_charlcd.page.PageID;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.LogWrapperListener;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;
import com.joas.utils.ZipUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

public class UIFlowManager implements PoscoCommManagerListener, PoscoModemCommManagerListener, RfidReaderListener, DSPControl2Listener,
         LogWrapperListener, TL3500SListener, OCPPSessionManagerListener, OCPPTransportMonitorListener {
    public static final String TAG = "UIFlowManager";
    public static final String OCPP_TAG = "OCPPTxRx";

    /**
     * TL3500S callback code
     * @param type : response code
     * @param retVal : response data
     * @param ch : UI channel
     */
    @Override
    public void responseCallback(TL3500S.ResponseType type, Map<String, String> retVal, int ch) {
        switch (type)
        {
            case Check:
                //onTL3500S_Check(retVal, ch);
                break;

            case Search:        //카드번호 조회 관련 응답 처리
                onTL3500S_Search(retVal, ch);
                break;

            case Pay:           //결제관련 응답처리(최초결제, 실결제)
                onTL3500S_Pay(retVal, ch);
                break;

            case CancelPay:         //취소결제 관련 응답처리
                onTL3500S_CancelPay(retVal, ch);
                break;

            case Error:
                //onTL3500S_Error(retVal, ch);
                break;

            case Event:             //결제단말기 응답이벤트 처리
                onTL3500S_Event(retVal, ch);
                break;

            case GetVersion:        //버전요청 관련 응답 처리
//                onTL3500S_GetVersion(retVal);
                break;

            case GetConfig:         //단말기 설정정보 요청 관련 응답처리
//                onTL3500S_GetConfig(retVal);
                break;
        }
    }

    void onTL3500S_Search(Map<String, String> retVal, int ch) {
        String cnum = retVal.get("cardnum").substring(4);
        onCardTagEvent(cnum, true);
        LogWrapper.d(TAG, "onTL3500S_Search: cardnum:"+cnum);
    }
    void onTL3500S_Pay(Map<String, String> retVal, int ch) {
        try {

            String cardNum = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거

            //최초결제 관련 정보 저장
            if(tl3500s.isprepayFlag){
                if ( retVal.get("payCode").equals("1")) { // 정상 승인
                    //최초결제 승인금액
                    poscoChargerInfo.paymentPreDealApprovalCost = Integer.parseInt(retVal.get("totalCost").replace(" ", ""));
                    //최초결제 승인번호
                    poscoChargerInfo.paymentPreDealApprovalNo = retVal.get("authNum").replace(" ", ""); // 승인번호
                    //최초결제 거래시간
                    poscoChargerInfo.paymentPreDealApprovalTime = (retVal.get("payDate")+retVal.get("payTime")).replace(" ","");
                    //최초결제 거래번호
                    poscoChargerInfo.paymentPreDealId = retVal.get("uniqueNum").replace(" ","");
                    //최초결제 거래일련번호
                    poscoChargerInfo.paymentPreDealSerialNo = retVal.get("pgnum").replace(" ", "");
                    //결제 성공
                    poscoChargerInfo.paymentResultStat = "01";
                    pageManger.paymentPrepayView.onPaySuccess();
                }
                else if (retVal.get("payCode").equals("X")){
                    //결제 실패
                    poscoChargerInfo.paymentErrCode = retVal.get("retCode");
                    poscoChargerInfo.paymentErrmsg = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();
                    poscoChargerInfo.paymentResultStat = "02";
                    pageManger.paymentPrepayView.onPayFailed();
                }
            }
            else if(!tl3500s.isprepayFlag){       //실결제 관련 정보 저장
                if ( retVal.get("payCode").equals("1")) { // 정상 승인
                    //실결제 승인금액
                    poscoChargerInfo.paymentDealApprovalCost = Integer.parseInt(retVal.get("totalCost").replace(" ", ""));
                    //실결제 승인번호
                    poscoChargerInfo.paymentDealApprovalNo = retVal.get("authNum").replace(" ", ""); // 승인번호
                    //실결제 거래시간
                    poscoChargerInfo.paymentDealApprovalTime = (retVal.get("payDate")+retVal.get("payTime")).replace(" ","");
                    //실결제 거래번호
                    poscoChargerInfo.paymentDealId = retVal.get("uniqueNum").replace(" ","");
                    //실결제 거래일련번호
                    poscoChargerInfo.paymentDealSerialNo = retVal.get("pgnum").replace(" ", "");
                    //결제 성공
                    poscoChargerInfo.paymentResultStat = "01";
                    pageManger.paymentRealPayView.onPaySuccess();

                }
                else if (retVal.get("payCode").equals("X")){
                    //결제 실패
                    poscoChargerInfo.paymentErrCode = retVal.get("retCode");
                    poscoChargerInfo.paymentErrmsg = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();
                    poscoChargerInfo.paymentResultStat = "02";
                    pageManger.paymentRealPayView.onPayFailed();
                }
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_Pay:"+e.toString());
        }
    }

    void onTL3500S_CancelPay(Map<String, String> retVal, int ch) {
        try {

            String cardNum = retVal.get("cardNum").replaceFirst("^0+(?!$)", ""); // 앞에 나온 0으로 제거

            if ( retVal.get("payCode").equals("1")) { // 정상 승인
                //취소금액
                poscoChargerInfo.paymentDealCancelApprovalCost = Integer.parseInt(retVal.get("totalCost").replace(" ", ""));
                //취소승인번호
                poscoChargerInfo.paymentDealCancelNo = retVal.get("authNum").replace(" ", ""); // 승인번호
                //취소일시
                poscoChargerInfo.paymentDealCancelTime = (retVal.get("payDate")+retVal.get("payTime")).replace(" ", "");
                //취소거래번호
                poscoChargerInfo.paymentDealCancelId = retVal.get("uniqueNum").replace(" ","");
                poscoChargerInfo.paymentResultStat = "01";
                if(cpConfig.useKakaoNavi) pageManger.pagePartialCancePayment.onCancelSuccess(); //카카오향 부분취소 성공 페이지 호출
                else pageManger.paymentCancelpayView.onCancelSuccess();
            }
            else if (retVal.get("payCode").equals("X")){
                //취소실패
                poscoChargerInfo.paymentErrCode = retVal.get("retCode");
                poscoChargerInfo.paymentErrmsg = retVal.get("errMsg").replaceAll("\\p{Z}", "").trim();
                poscoChargerInfo.paymentResultStat = "02";
                if(cpConfig.useKakaoNavi) pageManger.pagePartialCancePayment.onCancelFailed();
                else pageManger.paymentCancelpayView.onCancelFailed();
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "onTL3500S_CancelPay:"+e.toString());
        }
    }


    void onTL3500S_Event(Map<String, String> retVal, int ch) {
        if ( retVal.get("event") != null ) {
            poscoChargerInfo.payment_eventStat = retVal.get("event");

            String eventcode = retVal.get("event");
            switch (eventcode){
                case "I":
                    onTL3500s_ICInputEventOccured();
                    break;
                case "O":
                    onTL3500s_ICOutputEAventOccured();
                    break;
                case "F":
                    onTL3500s_FallbackEventOccured();
                    break;
                case "R":
                    onTL3500s_RFPayEventOccured();
                    break;
            }
        }
    }


    //================================================
    // OCPP Event 송/수신, OCPP 처리
    //================================================

    public void setOcppStatus(int connectorId, StatusNotification.Status status) {
        StatusNotification.Status oldStatus = chargeData.ocppStatus;
//        if ( oldStatus != status ) {
//            chargeData.ocppStatus = status;
//            ocppSessionManager.SendStatusNotificationRequest(connectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
//        }
        chargeData.ocppStatus = status;
        ocppSessionManager.SendStatusNotificationRequest(connectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
    }

    /**
     * 전체 시스템 상태(모든 커넥터)를 보낸다.
     * 오직 Available, Unavailable and Faulted 만 보낼 수 있다.(Spec 4.9 Status Notification)
     */
    public void sendNotificationStatusOfSystem() {
        // To. Do
        // 현재 Fault인지 아닌지 구별하여 보내야함..
        ocppSessionManager.SendStatusNotificationRequest(0, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );

        // 각 Connector의 상태값을 보낸다.
        ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
    }

    //ocpp v1.6 OcppSesstionManager 관련 함수
    @Override
    public void onAuthSuccess(int connectorId) {
        //차지비 사용안함
    }

    @Override
    public void onAuthFailed(int connectorId) {
        //차지비 사용안함
    }

    @Override
    public void onChangeState(int connectorId, OCPPSession.SessionState state) {
        final int cid = connectorId-1;
        final OCPPSession.SessionState chgState = state;
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        //차지비 사용안함
        return null;
    }

    @Override
    public void onBootNotificationResponse(boolean success) {
        if ( success ) {
            // 처음 접속이후에 StatusNotification을 보낸다.
            sendNotificationStatusOfSystem();

//            // 펌웨어 업데이트가 되었다면 메시지를 보내고 펌웨어 업데이트 필드를 초기화한다.
//            if ( updateManager.newFirmwareUpdateed ) {
//                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLED);
//                updateManager.onUpdateCompleteMsgSent();
//            }
        }
    }

    @Override
    public void onRemoteStopTransaction(int connectorId) {
        //차지비 사용안함
    }

    @Override
    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile) {
        //차지비 사용안함
        return false;
    }

    @Override
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo) {
        //차지비 사용안함 : id인증절차를 거치지 않기 때문에 invalid응답으로 충전이 종료됨.
//        if (chargeData.curConnectorId == connectorId) {
//            if (  flowState == UIFlowState.UI_CHARGING ) {
//                if ( tagInfo.getStatus() == IdTagInfo.Status.ACCEPTED ) {
//                    // 커넥터 상태를 충전중으로 바꾼다.(Status 메시지 보냄)
//                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);
//                }
//                else {
//                    // 커넥터 상태를 SUSPENDED_EVSE 로 바꾸고 충전을 중지한다.
//                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);
//
//                    OCPPConfiguration ocppConfiguration = ocppSessionManager.getOcppConfiguration();
//                    // 만약 StopTransactionOnInvalidId 가 true 이면 충전을 중지한다.
//                    if ( ocppConfiguration.StopTransactionOnInvalidId == true) {
//                        stopReason = StopTransaction.Reason.DE_AUTHORIZED;
//                        onChargingStop();
//                    }
//                }
//            }
//        }
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        //차지비 사용안함
        return null;
    }

    @Override
    public void onTriggerMessage(TriggerMessage message) {
        switch( message.getRequestedMessage().toString() ) {
            case "DiagnosticsStatusNotification":
                break;
            case "FirmwareStatusNotification":
//                onUpdateStatus(updateManager.getStatus());
                break;
            case "MeterValues":
//                DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
//                sendOcppMeterValues(rxData, SampledValue.Context.TRIGGER, false);
                break;
            case "StatusNotification":
                if ( message.getConnectorId() != null ) {
                    if ( message.getConnectorId() == 1 ) {
                        ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId, chargeData.ocppStatus, chargeData.ocppStatusError);
                    }
                } else {
                    sendNotificationStatusOfSystem();
                }
                break;
        }
    }

    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        //차지비 사용안함(운영, 운영중지)
//        if ( connectorId == chargeData.curConnectorId || connectorId == 0) {
//            if ( type == ChangeAvailability.Type.OPERATIVE ) {
//                isConnectorOperative = true;
//                setOcppStatus(connectorId, StatusNotification.Status.AVAILABLE);
//
//                // 사용불가 화면 숨김
//                pageManger.hideUnavailableConView();
//            }
//            else {
//                isConnectorOperative = false;
//                setOcppStatus(connectorId, StatusNotification.Status.UNAVAILABLE);
//
//                // 사용불가 화면 첫화면에서만 동작하도록 함
//                if ( flowState == UIFlowState.UI_SELECT ) pageManger.showUnavailableConView();
//            }
//        }
    }

    @Override
    public void onResetRequest(boolean isHard) {
        //차지비 사용안함
    }

    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {
        //차지비 사용안함
    }

    @Override
    public void onTimeUpdate(Calendar syncTime) {
        Calendar curTime = Calendar.getInstance();
        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);

        // 현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTimeInMillis() - syncTime.getTimeInMillis()) > TypeDefine.TIME_SYNC_GAP_MS) {
            am.setTime(syncTime.getTimeInMillis());
            LogWrapper.v(TAG, "TimeSync : "+syncTime.toString());
        }

        // bootNotification시에 연결 OK
        mainActivity.setCommConnStatus(true);
    }


    //ocpp v1.6 transport 관련 함수
    //================================================
    // 통신 이벤트 수신
    //================================================
    @Override
    public void onOCPPTransportRecvRaw(String data) {
//        String raw_recv = data;
//        LogWrapper.v(OCPP_TAG, "Recv: "+raw_recv);

        pageManger.getOcppCommMonitorView().addOCPPRawMsg("RX", data);
//        onOcppMessageTransportEvent();
//
        // Add to Diagnostic Log
        if ( ocppSessionManager != null ) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null ) {
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(
                        OCPPDiagnosticManager.DiagnosticType.Comm, "RX",data);
            }
        }
    }

    @Override
    public void onOCPPTransportSendRaw(String data) {
//        String raw_send = data;
//        LogWrapper.v(OCPP_TAG, "Send: "+raw_send);

        pageManger.getOcppCommMonitorView().addOCPPRawMsg("TX", data);
//        onOcppMessageTransportEvent();

        // Add to Diagnostic Log
        if ( ocppSessionManager != null ) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null ) {
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(
                        OCPPDiagnosticManager.DiagnosticType.Comm, "TX",data);
            }
        }
    }

    @Override
    public void onOCPPTransportConnected() {
//        onChangeOcppServerConnectStatus(true);
//        pageManger.getOcppCommMonitorView().addOCPPRawMsg("RX", "OCPP Server Connected!!");
        LogWrapper.v(TAG, "OCPP Server Connected!!");
    }

    @Override
    public void onOCPPTransportDisconnected() {
//        onChangeOcppServerConnectStatus(false);
//        pageManger.getOcppCommMonitorView().addOCPPRawMsg("RX", "OCPP Server Disconnected!!");
        LogWrapper.v(TAG, "OCPP Server Disconnected!!");
    }


    public enum UIFlowState {
        UI_READY,
        UI_SELECT_AUTH,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_QRCERT_WAIT,
        UI_CELL_NUM_INPUT,
        UI_AUTH_NUM_INPUT,
        UI_SELECT_TYPE_AND_TIME,        //차지비 충전커넥터 타입 및 시간 설정 상태
        UI_PAYMENT_PREPAY_STAT,
        UI_CONNECTOR_WAIT,    //차량 연결 대기 상태
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_PAYMENT_REALPAY_STAT,
        UI_PAYMENT_CANCELPAY_STAT,
        UI_PAYMENT_PARTIAL_CANCELPAY_STAT,      //부분취소 결제상태
        UI_UNPLUG,
        UI_SERVICE_STOP,     //add by si. 운영중지 상태
        UI_EIM_COMM_ERR     //add by si. 내부통신 장애 상태
    }


    PageManger pageManger;
    
    PoscoSlowCharLCDUIActivity mainActivity;

//    UIFlowState flowState = UIFlowState.UI_CARD_TAG;
    UIFlowState flowState;
    ChargeData chargeData;
    CPConfig cpConfig;
    MeterConfig meterConfig;

    boolean isBacklightOn = false;
    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    // DSP 관련 Attr
    DSPControl2 dspControl;
    boolean isDspReady = false;
    boolean isDspAvalCharge = false;
    boolean isDspDoor = false;
    boolean isDspPlug = false;
    boolean isDspChargeRun = false;
    boolean isDspChargeFinish = false;
    boolean isDspTestMode = false;          //si.add 200729  - chargev testmode flag
    boolean backupDspTestModeFlag = false;  //si.add 200729  - chargev testmode backup flag

    boolean isDspFault = false;
    boolean isPreDspFault = false;
    boolean isEmergencyPressed = false;

    long lastMeterValue = -1;
    long lastMeterGapValue = -1;
    long lastClockedMeterValue = -1;

    TimeoutTimer timerSec = null;
    String lastCardNum = "";
    String remote_lastCardnum = "";
    int lastDateValue = 0;

    int meterTimerCnt = 0;

    boolean isHardResetEvent = false;
    boolean isConnectorOperative = true;

    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    //RFID Reader
    RfidReader rfidReader;
    public TL3500S tl3500s;
    int firmwareInstallCounter = 0;
    double powerLimit = -1;

    int unplugTimerCnt = 0;
    int finishChargingViewCnt = 0;

    private IMeterAidlInterface meterService;

    PoscoChargerInfo poscoChargerInfo;
    PoscoCommManager poscoCommManager;
    PoscoModemCommManager poscoModemCommManager;

    //ocpp v1.6 관련
    OCPPSessionManager ocppSessionManager;
    StopTransaction.Reason stopReason = StopTransaction.Reason.LOCAL;


    boolean isFWUpdateReady = false;
    int dspVersion = 0;

    String[] lastMeterStringList = null;
    boolean dispMeteringStringTempLock = false;
    int dispChargingMeterStrCnt = 0;
    int dispChargingMeterBacklightCnt = 0;


    //si.add 201026 - 충전량 변화 종료관련 변수
    long initTime;
    long endTime;
    long distanceTime;
    int backupMeterval;
    boolean nonChangeMeterStopFlag = false;

    EvComm testcom;

    boolean restartBySoftReset = false;

    public UIFlowManager(PoscoSlowCharLCDUIActivity activity, ChargeData data, CPConfig config, MeterConfig mconfig, String restartReason) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;
        meterConfig = mconfig;

        LogWrapper.setLogWrapperListener(this);

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        //단가정보 로드
        poscoChargerInfo = new PoscoChargerInfo();
        poscoChargerInfo.loadCostInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + TypeDefine.COST_INFO_FILENAME);
        setMemberCostUnit();

        //si.로드된 충전기 설치정보를 poscoChargerInfo 객체로 전달
        poscoChargerInfo.loadCpConfigInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + CPConfig.CP_CONFIG_FILE_NAME);

        //모델명 및 UI 버전설정
        setUIversionAndModelName();


        //*주의 : JC-9111KE-TP-BC, JC-9511PS-B-PO-BC는 TX 30byte, JC-92C1-7-0P는 TX 29byte 설정 필요..
        if(cpConfig.chargerKind.equals("OP") || cpConfig.chargerKind.equals("CV")){
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_REG23_SIZE, 1, this);
        }
        else if(cpConfig.chargerKind.equals("CL")){
            if(chargeData.is9511)
                dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_REG23_SIZE, 1, this);
            else
                dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, 1, this);
        }
        dspControl.setMeterType(DSPControl2.METER_TYPE_SLOW);
        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);          //미터를 전력량계 통신 프로그램에서 가져온다 (false로 지정해야함)
        dspControl.start();



        if (meterConfig.lcdType.equals("None")) {
            //카모향 UI 인증방법 선택부터 시작
            if(cpConfig.useKakaoNavi) flowState = UIFlowState.UI_SELECT_AUTH;
            else flowState = UIFlowState.UI_READY;
        } else {
            flowState = UIFlowState.UI_CARD_TAG;
        }

        if(cpConfig.useTL3500BS){
            initPaymentInfo(poscoChargerInfo);
            tl3500s = new TL3500S(1,"/dev/ttyS3");
            tl3500s.setListener(this);
            tl3500s.start();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tl3500s.getVersionReq();
                }
            },500);
        }
        else{
            //add by si. 월박스 리비전타입일 경우 RF 리더 ACM 사용, 아닐경우 세한RF 사용
            if (cpConfig.useACMRF) {
                rfidReader = new RfidReaderACM1281S("/dev/ttyS3", RfidReaderACM1281S.RFID_CMD.RFID_TMONEY);
            } else {
                rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
                //rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
            }
//            else rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
            rfidReader.setRfidReaderEvent(this);
            rfidReader.rfidReadRequest();
        }




        //모뎀통신 생성
        poscoModemCommManager = new PoscoModemCommManager(activity.getBaseContext(), "192.168.1.1", 7788, this);
        poscoModemCommManager.start();

        String basePath = Environment.getExternalStorageDirectory() + TypeDefine.REPOSITORY_BASE_PATH;
        String basePath1 = Environment.getExternalStorageDirectory()+"";
        poscoCommManager = new PoscoCommManager(activity.getBaseContext(), cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID, cpConfig.chargerID, chargeData.chargerType, poscoChargerInfo, basePath, 30 * 1000);    //add by si. 차지비 서버재접속 시도 30초간격 설정
        poscoCommManager.setPoscoCommManagerListener(this);

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        //사용자 구분
        // 캐릭터LCD 사용일 경우 MEMBER로 초기화
        // 그외는 NONE
        if (meterConfig.lcdType.equals("None")) chargeData.authType = TypeDefine.AuthType.NONE;
        else chargeData.authType = TypeDefine.AuthType.MEMBER;

        //ocppv1.6 연동
        if(cpConfig.useOcpp){
            initOCPPSession();
        }

        // 1초 타이머 시작
        startPeroidTimerSec();
        initStartState();

        dispMeteringString(new String[]{"Welcome! ChargEV", "Disconnected", "Connecting...", "ID:" + cpConfig.stationID + cpConfig.chargerID});

        setSlowChargerType(config.slowChargerType);
    }

    /***
     * 2022-04-18 추가 : 완속충전기 타입 적용기능
     * UI에 설정된 값으로 202번지 write
     * @param slowChargerType
     */
    public void setSlowChargerType(int slowChargerType){
        dspControl.setSlowChargerType(chargeData.dspChannel,slowChargerType);
    }

    void initOCPPSession() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager = new OCPPSessionManager(mainActivity, chargeData.ocppConnectorCnt, Environment.getExternalStorageDirectory().toString()+TypeDefine.REPOSITORY_BASE_PATH, restartBySoftReset);
        ocppSessionManager.init(newOcppProperty);
        ocppSessionManager.setListener(this);

        //commMonitor Listener 등록
        ocppSessionManager.getOcppStack().setTransportMonitorListener(this);
    }
    public OCPPStackProperty loadOcppStackProperty() {
        OCPPStackProperty newOcppProperty = new OCPPStackProperty();

        newOcppProperty.serverUri = cpConfig.ocpp_serverURI;
        newOcppProperty.cpid = cpConfig.ocpp_chargerID;
        newOcppProperty.authID = cpConfig.httpBasicAuthID;
        newOcppProperty.authPassword = cpConfig.httpBasicAuthPassword;;
        newOcppProperty.chargePointSerialNumber = cpConfig.ocpp_chargePointSerialNumber;;
        newOcppProperty.useBasicAuth = cpConfig.useBasicAuth;
        newOcppProperty.useSSL = cpConfig.useSSL;

        //Bootnoti info
        newOcppProperty.chargePointVender = "JOAS";
        newOcppProperty.chargePointModel = "JC-92C1-7-0P";
        newOcppProperty.chargeBoxSerialNumber = cpConfig.ocpp_chargerID;
        newOcppProperty.firmwareVersion = poscoChargerInfo.chg_sw_version;
        newOcppProperty.meterType = "AC3";

//        //서울시 개발서버 접속 test
//        newOcppProperty.serverUri = "wss://test.kevit.co.kr:21041/daemon/endpoint";
//        newOcppProperty.cpid = "KES-00000000004-001";
//        newOcppProperty.useSSL = true;
//        newOcppProperty.useBasicAuth = true;        //basicauth사용을 해야 아래 ID,password를 사용할 수 있음(websocket ssl접속시 필요)
//        newOcppProperty.authID = "KES-00000000004-001";
//        newOcppProperty.authPassword = "A123456789012345";
//        newOcppProperty.chargePointSerialNumber = "KES-00000000004-001";

//        newOcppProperty.serverUri = cpConfig.ocpp_serverURI;
//        newOcppProperty.cpid = cpConfig.ocpp_chargerID;
//        newOcppProperty.useBasicAuth = false;
//        newOcppProperty.authID = cpConfig.httpBasicAuthID;
//        newOcppProperty.authPassword = cpConfig.httpBasicAuthPassword;
//        newOcppProperty.chargePointSerialNumber = cpConfig.chargerID;

        return newOcppProperty;
    }

    void setUIversionAndModelName(){
        TypeDefine.SW_VERSION = "220516("+cpConfig.chargerKind+")";
//        poscoChargerInfo.chg_sw_version = "UI:1.3 20220204(" + cpConfig.chargerKind + ")     CO:1.3 20220204 ";
        poscoChargerInfo.chg_sw_version = "UI:1.3 20220516(" + cpConfig.chargerKind + ")";
        TypeDefine.SW_RELEASE_DATE = "2022-05-16";
        if(cpConfig.chargerKind.equals("OP") || cpConfig.chargerKind.equals("CV")) {
            TypeDefine.MODEL_NAME = "JC-9111KE-TP-BC";
        }
        else if(cpConfig.chargerKind.equals("CL")){
            if(chargeData.is9511) TypeDefine.MODEL_NAME = "JC-9511PS-BS-PO-BC(New Meter)";
            else TypeDefine.MODEL_NAME = "JC-92C1-7-0P";
        }
    }


    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();
        poscoCommManager.stopManager();

        if(cpConfig.useOcpp) ocppSessionManager.closeManager();

        if(cpConfig.useTL3500BS) tl3500s.stopThread();
        else rfidReader.stopThread();
    }

    /**
     * 현장결제 관련 변수 초기화
     * @param pinfo
     */
    public void initPaymentInfo(PoscoChargerInfo pinfo){
        pinfo.paymentCompany = 0x00;
        pinfo.paymentStoreId  = ""; // 상점번호
        pinfo.paymentPreDealId = ""; // 선결제 거래번호
        pinfo.paymentPreDealSerialNo = ""; // 선결제 거래일련번호(PG결제)
        pinfo.paymentPreDealApprovalNo = ""; // 선결제 승인번호
        pinfo.paymentPreDealApprovalTime = ""; // 선결제 승인일시
        pinfo.paymentPreDealApprovalCost = 0; // 선결제 승인 금액

        pinfo.paymentDealId = ""; // 거래번호
        pinfo.paymentDealSerialNo = ""; // 거래일련번호
        pinfo.paymentDealApprovalNo = ""; // 승인번호
        pinfo.paymentDealApprovalTime = ""; // 승인일시
        pinfo.paymentDealApprovalCost = 0; //결제 승인 금액

        pinfo.paymentDealCancelId = ""; // 취소거래번호
        pinfo.paymentDealCancelNo = ""; // 취소번호
        pinfo.paymentDealCancelTime = ""; // 취소일시
        pinfo.paymentDealCancelApprovalCost = 0;   //취소승인금액
        pinfo.paymentNoMemberPhone = ""; // 비회원전화번호
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }
    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }
    public PoscoChargerInfo getPoscoChargerInfo() { return poscoChargerInfo; }
    /**
     * add by si. control보드 원격 업데이트 플래그 세팅 테스트 함수
     */
    public void setFWRemoteOn(){
        dspControl.setState200(chargeData.dspChannel,DSPTxData2.STATUS200.CH2_LED2,true);
    }
    public void setFWRemoteOff(){
        dspControl.setState200(chargeData.dspChannel,DSPTxData2.STATUS200.CH2_LED2,false);
    }

    //add by si. 200811 - server socket disconnect test button event
    public void doServerDisconnect(){
        //todo : server socket close...
    }

    public void showJoasCommMonitor() {
        pageManger.showJoasCommMonitor();
    }
    public void hideJoasCommMonitor() {
        pageManger.hideJoasCommMonitor();
    }

    public void showDspMonitor() {
        pageManger.showJoasDspMonitor();
    }
    public void hideDspMonitor() {
        pageManger.hideJoasDspMonitor();
    }

    public void showDebugView() {
        pageManger.showJoasDebugView();
    }
    public void hideDebugView() {
        pageManger.hideJoasDebugView();
    }
    public int getDspVersion() { return dspVersion; }
    public EvCommManager getCommManager() { return poscoCommManager; }

    public UIFlowState getUIFlowState() { return flowState; }

    public PoscoChargerInfo getCharevReservInfo(){ return poscoChargerInfo; }
    public TL3500S getTL3500S(){return tl3500s;}
    void setUIFlowState(UIFlowState state) {
        flowState = state;

        processChangeState(flowState );
    }

    void startPeroidTimerSec() {
        timerSec = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerProcessSec();
            }
        });
        timerSec.start();
    }

    /**
     * TL3500s 카드 이벤트 관련 발생시 실행 함수
     */

    public void onTL3500s_ICInputEventOccured(){
        //이벤트 "I" 발생
        if(flowState == UIFlowState.UI_PAYMENT_PREPAY_STAT){
            pageManger.paymentPrepayView.onCardIn();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_REALPAY_STAT){
            pageManger.paymentRealPayView.onCardIn();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_CANCELPAY_STAT){
            pageManger.paymentCancelpayView.onCardIn();
        }
    }

    public void onTL3500s_ICOutputEAventOccured(){
        //이벤트 "O" 발생
        if(flowState == UIFlowState.UI_PAYMENT_PREPAY_STAT){
            pageManger.paymentPrepayView.onCardOut();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_REALPAY_STAT){
            pageManger.paymentRealPayView.onCardOut();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_CANCELPAY_STAT){
            pageManger.paymentCancelpayView.onCardOut();
        }
    }

    public void onTL3500s_FallbackEventOccured(){
        //이벤트 "F" 발생
        if(flowState == UIFlowState.UI_PAYMENT_PREPAY_STAT){
            pageManger.paymentPrepayView.onCardFallback();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_REALPAY_STAT){
            pageManger.paymentRealPayView.onCardFallback();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_CANCELPAY_STAT){
            pageManger.paymentCancelpayView.onCardFallback();
        }
    }

    public void onTL3500s_RFPayEventOccured(){
        //이벤트 "R" 발생(삼성페이, NFC결제)
        if(flowState == UIFlowState.UI_PAYMENT_PREPAY_STAT){
            pageManger.paymentPrepayView.onRFTouch();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_REALPAY_STAT){
            pageManger.paymentRealPayView.onRFTouch();
        }
        else if(flowState == UIFlowState.UI_PAYMENT_CANCELPAY_STAT){
            pageManger.paymentCancelpayView.onRFTouch();
        }
    }


    /**
     * 핸드폰 인증화면에서 OK버튼 이벤트 처리 함수
     */
    public void onCellPhoneNumInputOK(){
        //승인번호 요청(통신)
        poscoChargerInfo.payment_authNum = "";
        poscoCommManager.sendCellphoneAuthReq();
        //승인번호 입력화면 상태로 전환
        setUIFlowState(UIFlowState.UI_AUTH_NUM_INPUT);
        //승인번호 입력 화면으로 전환
        pageManger.changePage(PageID.INPUT_AUTH_NUM);
    }

    /**
     * 비회원 충전완료 후 실결제->선취 혹은 선취 시퀀스 판단 함수
     */
    public void onPaymentCheckFinishCharging() {
        //카카오 향일경우 부분취소 적용
        if(cpConfig.useKakaoNavi){
            //선결제 내역이 있고, 선결제 금액이 실제 사용 금액보다 작을 경우 부분취소
            if ((poscoChargerInfo.paymentPreDealApprovalCost > 0) && (chargeData.chargingCost < poscoChargerInfo.paymentPreDealApprovalCost)) {
                //최소결제금액 100원
                if (((int) chargeData.chargingCost) > 99) {
                    //부분취소 설정
                    chargeData.partialCancelType = 5;
                } else {
                    //무카드 취소(선결제 전체취소)
                    chargeData.partialCancelType = 4;
                }
                onPaymentPartialCancelReq();
            }
            else{
                //선결제 금액만큼 충전이 되었을 경우 바로 플러그 제거로 이동
                //f1 전문 전송
                poscoCommManager.sendFinishCharging();
                //Unplug 안내 상태로 이동
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManger.changePage(PageID.UNPLUG);
            }
        }
        else{
            //선결제 내역이 있고, 선결제 금액이 실제 사용 금액보다 작을 경우 실결제->선취로 이동
            if ((poscoChargerInfo.paymentPreDealApprovalCost > 0) && (chargeData.chargingCost < poscoChargerInfo.paymentPreDealApprovalCost)) {
                //최소결제금액 100원
                if (((int) chargeData.chargingCost) > 99) {
                    //실결제로 이동
                    onPaymentRealPayReq();
                } else {
                    //선결제 취소로 이동
                    onPaymentCancelReq();
                }
            }
            else{
                //선결제 금액만큼 충전이 되었을 경우 바로 플러그 제거로 이동
                //f1 전문 전송
                poscoCommManager.sendFinishCharging();
                //Unplug 안내 상태로 이동
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManger.changePage(PageID.UNPLUG);
            }
        }
    }

    /**
     * 1p응 답에 대해 미처리된 결제건이 있는지 확인
     */
    public void onCheckMissingPayment(){
        //비회원 충전시작 전 예약상태 체크
        if(checkReservation())
        {
            if(poscoChargerInfo.nomemAuthResultFlag == 0x00){
                //정상, 시간설정으로 이동
                doAuthComplete();
            }
            else if(poscoChargerInfo.nomemAuthResultFlag == 0x01){
                //서버에 한도승인 취소건만 없을 경우, 취소화면으로 이동
                onPaymentCancelReq();
            }
            else if(poscoChargerInfo.nomemAuthResultFlag == 0x02){
                //서버에 한도승인건만 있을 경우(실승인,한도승인취소가 없을 경우)
                if(poscoChargerInfo.recvfromserver_usePayReal == 0){
                    //결제 취소로 이동
                    onPaymentCancelReq();
                }
                else if(poscoChargerInfo.recvfromserver_usePayReal != 0){
                    //실결제 이동
                    onPaymentRealPayReq();
                }
            }
        }
        else{
            //예약건으로 인한 인증실패 문구 띄워주기
            if(meterConfig.lcdType.equals("None")){
                chargeData.messageBoxTitle = "인증 실패";
                chargeData.messageBoxContent = "예약이 존재합니다.";
                pageManger.getAuthWaitView().stopTimer();
                pageManger.showMessageBox();
            }
            else dispTempMeteringString(new String[]{"A Reservation", "exist."}, 6000);
            goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
        }
    }

    public void onAdminPasswordOK(String pwd) {
        if ( pwd.equals(cpConfig.settingPassword) == true ) {
            pageManger.hideAdminPasswrodInputView();
            pageManger.showSettingView();
        }
        else {
            Toast.makeText(mainActivity , mainActivity.getResources().getString(R.string.string_password_incorrect), Toast.LENGTH_SHORT).show();
        }
    }

    public String getAlignString(String str, boolean isLeft) {
        if ( str.length() == 16 ) return str;
        if ( str.length() == 0 ) return "                "; // 16 space

        if (isLeft) {
            return String.format("%s%"+(16-str.length())+"s", str, "");
        }
        else {
            return String.format("%16s", str);
        }
    }

    public void dispMeteringString(String[] listStr) {
        if(!meterConfig.lcdType.equals("None")) dispMeteringString(listStr, true);
    }

    public void dispMeteringString(String[] listStr, boolean isBacklightOn) {
        lastMeterStringList = listStr;

        if ( dispMeteringStringTempLock ) return;

        if ( listStr == null ) return;
        try {
            IMeterAidlInterface meterService = mainActivity.getMeterService();

            if ( isBacklightOn ) {
                if (!(meterConfig.lcdType.equals("RW1603"))) meterService.setCharLCDBacklight(true);
                dispChargingMeterBacklightCnt = 0; // 카운터 초기화 타임아웃시 Off함
            }

            String disp1 = getAlignString(listStr[0], true);
            String disp2 = listStr.length > 1 ? getAlignString(listStr[1], true) : "";
            String disp3 = listStr.length > 2 ? getAlignString(listStr[2], true) : "";
            String disp4 = listStr.length > 3 ? getAlignString(listStr[3], true) : "";

            meterService.setCharLCDDisp(listStr.length, disp1, disp2, disp3, disp4);
        }
        catch (Exception e){
            LogWrapper.e(TAG, "dispMeteringString err:"+e.toString());
        }
    }

    public void dispLastMeteringString() {
        if(!meterConfig.lcdType.equals("None")) dispMeteringString(lastMeterStringList);
    }

    /**
     * 캐릭터 LCD에 delayMs 동안 표시
     *
     * 주석 by Lee 20200518
     * @param listStr
     * @param delayMs
     */
    public void dispTempMeteringString(String[] listStr, int delayMs) {
        dispMeteringString(listStr);
        dispMeteringStringTempLock = true;

        final int timeout = delayMs;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dispMeteringStringTempLock = false;
                        dispMeteringString(lastMeterStringList);
                    }
                }, timeout);
            }
        });
    }

    /**
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch ( state ) {
            case UI_READY:
                if(meterConfig.lcdType.equals("None") && !cpConfig.useKakaoNavi) initStartState();
                break;

            case UI_SELECT_AUTH:
                if(meterConfig.lcdType.equals("None") && cpConfig.useKakaoNavi) initStartState();
                break;

            case UI_CARD_TAG:
                if(!meterConfig.lcdType.equals("None")) initStartState();
                break;

            case UI_CONNECTOR_WAIT:
                if(meterConfig.lcdType.equals("None")){
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                else{
                    if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                    }
                }

                break;
            case UI_RUN_CHECK:
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                break;

            case UI_UNPLUG:
                if(meterConfig.lcdType.equals("None")){
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                else{
                    if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                    }
                }

                unplugTimerCnt = 0;
                finishChargingViewCnt = 0;

                break;

            case UI_FINISH_CHARGING:
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);

//                // Ctype인 경우에는 도어를 오픈할 필요가 없음
//                if(chargeData.connectorType == TypeDefine.ConnectorType.BTYPE)
//                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);

//                unplugTimerCnt = 0;
//                finishChargingViewCnt = 0;
                break;
        }
    }

    void initStartState() {
        //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        if ( isConnectorOperative == false ) pageManger.showUnavailableConView();

        // 변수 초기화
        chargeData.measureWh = 0;
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        powerLimit = -1.0d;

        unplugTimerCnt = 0;
        finishChargingViewCnt = 0;

        poscoChargerInfo.isRemoteCharge = false;

        if(cpConfig.useTL3500BS) initPaymentInfo(poscoChargerInfo);
        chargeData.firstpayFlag = false;
        chargeData.realpayflag = false;
    }

    public void onSelectAuthEvent(){
        setUIFlowState(UIFlowState.UI_SELECT_AUTH);
        //사용자 인증방법 선택화면
        pageManger.changePage(PageID.SELECT_AUTH);
    }
    public void onSelectChargevMemberEvent(){
        setUIFlowState(UIFlowState.UI_CARD_TAG);
        pageManger.changePage(PageID.CARD_TAG);
    }
    public void onSelectNoMemberEvent(){
        //핸드폰 번호 인증 화면으로 이동
        if(cpConfig.useKakaoNavi){
            //(카카오향일 경우 바로 시간설정으로 이동, 예약체크 안함)
            doAuthComplete();
        }
        else{
            setUIFlowState(UIFlowState.UI_CELL_NUM_INPUT);
            pageManger.changePage(PageID.INPUT_CELL_NUM);
        }

    }
    public void onSelectKakaoQRCertEvent(){
        //원격충전 대기(Q1)
        setUIFlowState(UIFlowState.UI_QRCERT_WAIT);
        pageManger.changePage(PageID.KAKAO_QRCERT_WAIT);
    }

    /**
     * 최초결제 이후 처음으로 버튼 눌렸을 경우 처리 함수
     */
    public void onCheckPaymentStatusClickHome(){
        if(cpConfig.useKakaoNavi){          //카카오향일 경우 무카드 취소 처리
            if(flowState == UIFlowState.UI_CONNECTOR_WAIT){
                if((chargeData.authType == TypeDefine.AuthType.NOMEMBER) && (chargeData.firstpayFlag)){
                    // 충전 관련 변수를 초기화 한다.
                    initChargingStartValue();
                    //최초승인 결제건에 대한 d1 전문 전송
                    poscoCommManager.sendStartCharging();
                    //최초결제 무카드취소 진행으로 이동
                    chargeData.partialCancelType = 4;
                    onPaymentPartialCancelReq();
                }
            }
            else if(flowState == UIFlowState.UI_PAYMENT_PARTIAL_CANCELPAY_STAT){}
            else{
                onPageStartEvent();
            }
        }
        else{
            if(flowState == UIFlowState.UI_CONNECTOR_WAIT){
                if((chargeData.authType == TypeDefine.AuthType.NOMEMBER) && (chargeData.firstpayFlag)){
                    // 충전 관련 변수를 초기화 한다.
                    initChargingStartValue();
                    //최초승인 결제건에 대한 d1 전문 전송
                    poscoCommManager.sendStartCharging();
                    //최초결제 취소 진행으로 이동
                    onPaymentCancelReq();
                }
            }else if(flowState == UIFlowState.UI_PAYMENT_CANCELPAY_STAT){
                if(poscoChargerInfo.nomemAuthResultFlag == 0x01 || poscoChargerInfo.nomemAuthResultFlag == 0x02){
                    //q1전송 후 홈으로
                    poscoCommManager.sendMissingPaymentCompleteReq();
                    onPageStartEvent();
                }
                else{
                    //f1보내고 선결제 이력이 있으면 언플러그로, 그외는 홈으로
                    poscoCommManager.sendFinishCharging();
                    if(chargeData.firstpayFlag){
                        setUIFlowState(UIFlowState.UI_UNPLUG);
                        pageManger.changePage(PageID.UNPLUG);
                    }
                    else onPageStartEvent();
                }
            }else if(flowState == UIFlowState.UI_PAYMENT_REALPAY_STAT){
                if(poscoChargerInfo.nomemAuthResultFlag == 0x01 || poscoChargerInfo.nomemAuthResultFlag == 0x02){
                    onPageStartEvent();
                }
                else{
                    //f1보내고 선결제 이력이 있으면 언플러그로, 그외는 홈으로
                    poscoCommManager.sendFinishCharging();
                    if(chargeData.firstpayFlag){
                        setUIFlowState(UIFlowState.UI_UNPLUG);
                        pageManger.changePage(PageID.UNPLUG);
                    }
                    else onPageStartEvent();
                }
            }
            else{
                onPageStartEvent();
            }
        }

    }

    public void onPaymentCancelReq(){
        setUIFlowState(UIFlowState.UI_PAYMENT_CANCELPAY_STAT);
        pageManger.changePage(PageID.PAYMENT_CANCEL_PAY);
    }

    public void onPaymentRealPayReq(){
        setUIFlowState(UIFlowState.UI_PAYMENT_REALPAY_STAT);
        pageManger.changePage(PageID.PAYMENT_REAL_PAY);
    }

    public void onPaymentPartialCancelReq(){
        setUIFlowState(UIFlowState.UI_PAYMENT_PARTIAL_CANCELPAY_STAT);
        pageManger.changePage(PageID.PAYMENT_PARTIAL_CANCEL_PAY);
    }

    public void onPageStartEvent() {
        //원격충전일경우 t1 보내는 조건에대해 전송(홈버튼 눌렀을 경우..)
        if(poscoChargerInfo.isRemoteCharge){
            if(getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT || getUIFlowState() == UIFlowState.UI_RUN_CHECK)
                poscoCommManager.sendAbnormalTemination();
        }
        if(meterConfig.lcdType.equals("None")) {
            if(cpConfig.useKakaoNavi){
                setUIFlowState(UIFlowState.UI_SELECT_AUTH);
                pageManger.changePage(PageID.SELECT_AUTH);
            }
            else{
                setUIFlowState(UIFlowState.UI_READY);
                pageManger.changePage(PageID.PAGE_READY);
            }

            //tl3500bs 리더기 대기상태 전환
            if(cpConfig.useTL3500BS) tl3500s.termReadyReq();
            else if(!cpConfig.useACMRF) rfidReader.rfidReadRequest();
        }
        else{
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManger.changePage(PageID.CARD_TAG);
            if(cpConfig.useSehanRF) rfidReader.rfidReadRequest();
        }

        if ( poscoCommManager.isConneted() )
        {
            if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
            else dispMeteringString(new String[] {"Welcome! ChargEV", "Tag Card for EV", "ID:"+cpConfig.stationID+cpConfig.chargerID, "Ver:" + TypeDefine.SW_VERSION});

            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
        }
        else
        {
            if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
            else dispMeteringString(new String[] {"Welcome! ChargEV", "Disconnected", "Connecting...", "ID:"+cpConfig.stationID+cpConfig.chargerID});

            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, false);     //서버통신상태
        }

        if(cpConfig.useOcpp) {
            if ( chargeData.isdspCommError || chargeData.ismeterCommError ) {
                if ( chargeData.ocppStatus != StatusNotification.Status.FAULTED ) {
                    // To.Do.. Error Code..Set
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FAULTED);
                }
                else if ( (chargeData.ocppStatus == StatusNotification.Status.FAULTED)
                        && (chargeData.isdspCommError == false && chargeData.ismeterCommError == false)) {
                    chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                }
            }
            else{
                if(isDspFault == false){
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                    ocppSessionManager.closeSession(chargeData.curConnectorId);
                }
            }

        }
    }

    /**
     * Select화면에서(플러그 타입) 선택되었을때 이벤트 처리부분
     * @param event
     */
    public void onPageSelectEvent(PageEvent event) {
        // Fault인경우에 초기화면으로 돌아감
        if ( isDspFault ) {
            fillFaultMessage();
            pageManger.showFaultBox();
            return;
        }

        switch ( event ) {
            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                break;
            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                break;
            case SELECT_AC3_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.AC3;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_AC3);
                break;
            case SELECT_CHADEMO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCHADEMO);
                break;
            case SELECT_DCCOMBO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCOMBO);
                break;

        }

        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.MANAGE_STOP, false);     //무료모드

        // Session Start

        // Next Flow. Card Tag
        if(cpConfig.isAuthSkip)
        {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, true);     //무료모드

            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            doAuthComplete();
        }else {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, false);     //무료모드

            if(chargeData.authType == TypeDefine.AuthType.NOMEMBER){
                //비회원의 경우 최초결제 안내화면으로 이동 - edit by si.210413
                setUIFlowState(UIFlowState.UI_PAYMENT_PREPAY_STAT);
                pageManger.changePage(PageID.PAYMENT_PREPAY);

            }
            else if(chargeData.authType == TypeDefine.AuthType.MEMBER){
                //회원 충전일 경우 커넥터 연결 대기 화면으로 이동 - edit by si.210413
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                pageManger.changePage(PageID.CONNECTOR_WAIT);
            }

        }
    }

    /***
     * plugtype select 이외의 화면에서 버튼 터치에 대한 이벤트 처리 부분
     * @param event
     */
    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                if((chargeData.authType == TypeDefine.AuthType.MEMBER) && (!cpConfig.useKakaoNavi)) onPageStartEvent();
                else if(chargeData.authType == TypeDefine.AuthType.NOMEMBER) onCheckPaymentStatusClickHome();
                else if(getUIFlowState() == UIFlowState.UI_CHARGING){}
                else onPageStartEvent();
                break;

            case SELECT_CHG_START_CLICK:
                onSelectAuthEvent();
                break;

            case SELECT_QRCERT_CLICK:
                onSelectKakaoQRCertEvent();
                break;

            case SELECT_MEMBER_CLICK:
                chargeData.authType = TypeDefine.AuthType.MEMBER;
                poscoChargerInfo.authType = PoscoChargerInfo.CommAuthType.MEMBER;
                poscoChargerInfo.paymentCompany = 0x00;
                poscoChargerInfo.payMethod = 0x00;
                onSelectChargevMemberEvent();
                break;

            case SELECT_NOMEMBER_CLICK:
                chargeData.authType = TypeDefine.AuthType.NOMEMBER;
                poscoChargerInfo.authType = PoscoChargerInfo.CommAuthType.NOMEMBER;
                poscoChargerInfo.paymentCompany = 0x01;
                if(cpConfig.useKakaoNavi) poscoChargerInfo.payMethod = 0x02;        //카카오향은 부분취소 사용
                else poscoChargerInfo.payMethod = 0x01;
                poscoChargerInfo.cardNum = TypeDefine.NOMEMBER_AUTH_CARD_NUM;
                onSelectNoMemberEvent();
                break;
        }
    }
    public void onPrepaySuccess(){
        //선결제 완료 플래그 설정
        chargeData.firstpayFlag = true;
        //커넥터 연결 대기로 이동
        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
        pageManger.changePage(PageID.CONNECTOR_WAIT);
    }

    public void onPartialCancelPaySuccess(){
        //f1 전문 전송
        poscoCommManager.sendFinishCharging();
        //Unplug 안내 상태로 이동
        setUIFlowState(UIFlowState.UI_UNPLUG);
        pageManger.changePage(PageID.UNPLUG);
    }

    public void onCancelPaySuccess(){
        if(poscoChargerInfo.nomemAuthResultFlag == 0x01 || poscoChargerInfo.nomemAuthResultFlag == 0x02){
            //q1전송
            poscoCommManager.sendMissingPaymentCompleteReq();
            //1q응답에 대해 무시하고 첫화면으로 복귀(다시 폰 인증시 해당정보 내려오므로)
            onPageStartEvent();
        }
        else {
            //f1 전문 전송
            poscoCommManager.sendFinishCharging();
            //Unplug 안내 상태로 이동
            setUIFlowState(UIFlowState.UI_UNPLUG);
            pageManger.changePage(PageID.UNPLUG);
        }
    }
    public void onRealPaySuccess(){
        chargeData.realpayflag = true;
        //선결제 취소로 이동
        onPaymentCancelReq();
    }

    public boolean isLocalAuth = false;

    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            if ( flowState == UIFlowState.UI_CARD_TAG ) {
                poscoChargerInfo.cardNum = tagNum;
                poscoChargerInfo.payMethod = PoscoChargerInfo.PAY_METHOD_BY_SERVER;
                lastCardNum = tagNum;

                setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                dispMeteringString(new String[] {"Card Verifing...", "Wait a second"});

                // 로컬인증 진행
                if ( poscoCommManager.isConneted() == false ) {
                    isLocalAuth = true;
                    if ( poscoCommManager.localAuthentication(tagNum) == true ) {
                        onAuthResultEvent(true, 0, 0);
                    }
                    else {
                        onAuthResultEvent(false, 11, 0); //로컬인증 실패의 경우 실패사유 11번(UID 인증 에러) 고정
                    }
                }
                else {
                    isLocalAuth = false;
                    poscoCommManager.sendAuthReq();
                    // 승인대기 화면 전환
                    pageManger.changePage(PageID.AUTH_WAIT);
//                    dispMeteringString(new String[] {"Card Verifing...", "Wait a second"});
                }
            }
            else if ( flowState == UIFlowState.UI_CHARGING ) {
                if(poscoChargerInfo.isRemoteCharge)
                {
                    //원격 충전으로 시작해도 현장 카드태깅 종료 가능하도록 모니터링
                    if(tagNum.equals(remote_lastCardnum))
                    {
                        LogWrapper.v(TAG, "Stop by User Card Tag");
                        onChargingStop();
                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
                    }
                    else dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
                }
                else {
                    if ( tagNum.equals(lastCardNum) == true ) {
                        LogWrapper.v(TAG, "Stop by User Card Tag");
                        onChargingStop();
                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
                    }
                    else {
                    /*
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                    pageManger.showMessageBox();
                    */

                        dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
                    }
                }
            }
        }
    }

    public void goHomeProcessDelayed(int delayMs) {
        final int timeout = delayMs;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onPageStartEvent();
                    }
                }, timeout);
            }
        });
    }

    public void onAuthResultEvent(boolean isSuccess, int reason) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (isSuccess) {
                //add by si.210202 - 차지비 예약상태 체크
                if(checkReservation())
                {
                    doAuthComplete();
                }
                else{
                    //예약건으로 인한 인증실패 문구 띄워주기
                    if(meterConfig.lcdType.equals("None")){
                        chargeData.messageBoxTitle = "인증 실패";
                        chargeData.messageBoxContent = "예약이 존재합니다.";
                        pageManger.getAuthWaitView().stopTimer();
                        pageManger.showMessageBox();
                    }
                    else dispTempMeteringString(new String[]{"A Reservation", "exist."}, 6000);
                    goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                }
            } else {
                if(meterConfig.lcdType.equals("None")) {
                    // 메시지 박스 내용 채움
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                    pageManger.getAuthWaitView().stopTimer();
                    pageManger.showMessageBox();
                }
                else{
                    if(reason == 17)        //0x11 : UID 인증에러
                        dispTempMeteringString(new String[]{"Card Failed(11)", "Check Card Info"}, 6000);
                    else if(reason == 18)   //0x12 : 충전 Credit 단가 확인 에러
                        dispTempMeteringString(new String[]{"Card Failed(12)", "Check Card Info"}, 6000);
                    else if(reason == 19)   //0x13 : Credit 부족 에러
                        dispTempMeteringString(new String[]{"Card Failed(13)", "No Credit"}, 6000);
                    else if(reason == 20)   //0x14 : 충전시간 에러
                        dispTempMeteringString(new String[]{"Card Failed(14)", "Charging Time","Error"}, 10000);
                    else if (reason == 21)       //0x15 : 예약시간 에러
                        dispTempMeteringString(new String[]{"Card Failed(15)", "Reserv Time Err"}, 6000);
                    else if(reason == 22)       //0x16 : 충전소 에러
                        dispTempMeteringString(new String[]{"Card Failed(16)", "Station Error"}, 6000);
                    else if(reason == 23)       //충전기 에러
                        dispTempMeteringString(new String[]{"Card Failed(17)", "Charger Error"}, 6000);
                    else dispTempMeteringString(new String[]{"Card Failed", "Check Card Info"}, 6000);
                }
                goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                //dispTempMeteringString(new String[]{"Card Failed", "Check Card Info"}, 6000);
            }
        }
    }


     void doAuthComplete() {
        if(poscoChargerInfo.isRemoteCharge) {
            //원격 충전시도일 경우
            LogWrapper.v(TAG, "Remote Auth Complete");

            dispMeteringString(new String[] {"Remote Check OK.", "Connect Cable"});
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            pageManger.changePage(PageID.CONNECTOR_WAIT);
        }
        else {
            LogWrapper.v(TAG, "Auth Complete");
            dispMeteringString(new String[]{"Card Check Done.", "Connect Cable"});
            //캐릭터 LCD일경우 CONNECTOR_WAIT 상태로 전환, 아닐경우 충전방식 및 시간설정 화면으로 이동
            if(meterConfig.lcdType.equals("None")){
                //시간설정 및 타입 선택 화면으로 이동
                setUIFlowState(UIFlowState.UI_SELECT_TYPE_AND_TIME);
                pageManger.changePage(PageID.SELECT_SLOW);
            }
            else {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                pageManger.changePage(PageID.CONNECTOR_WAIT);
            }
        }

        if(cpConfig.useOcpp) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);

//        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
//        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
//            // 이미 Connect 된 상태이라면
//            onConnectedCableEvent(true);
//        } else {
//            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
//            pageManger.changePage(PageID.CONNECTOR_WAIT);
//            // Ctype인 경우에는 도어를 오픈할 필요가 없음
//            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
//
//        }
    }

    public void setMemberCostUnit() {
//        int slot = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
//        chargeData.chargingUnitCost = ((double)poscoChargerInfo.memberCostUnit) / 100.0;
//        poscoChargerInfo.curChargingCostUnit = poscoChargerInfo.memberCostUnit;

        if (cpConfig.useKakaoCost) {
            //카카오 현장단가 사용할 경우
            if (chargeData.authType == TypeDefine.AuthType.MEMBER) {
                //QR인증도 멤버로 설정
                chargeData.chargingUnitCost = (double) cpConfig.kakaoQRCost;
            } else {
                chargeData.chargingUnitCost = (double) cpConfig.kakaoCreditCost;
            }
            LogWrapper.d(TAG, "setMemberCostUnit(kakao):" + chargeData.chargingUnitCost);
        } else {
            if (chargeData.authType == TypeDefine.AuthType.MEMBER) {
                chargeData.chargingUnitCost = ((double) poscoChargerInfo.memberCostUnit) / 100.0;
            } else {
                chargeData.chargingUnitCost = ((double) poscoChargerInfo.nonMemberCostUnit) / 100.0;
            }
            LogWrapper.d(TAG, "setMemberCostUnit:" + poscoChargerInfo.curChargingCostUnit);
        }
    }

    /*
    * Check reservation 함수
    * add by si. 210202
     */
    public boolean checkReservation()
    {
        boolean ret = false;

        if(poscoChargerInfo.rsv_flag == 0) ret = true;      //예약이 없을경우 바로 true 리턴
        else if(poscoChargerInfo.rsv_flag == 1)
        {
            //비예약자만 사용가능(예약시작시간 30분 전까지)
            if(chargeData.authType == TypeDefine.AuthType.NOMEMBER) ret = true;
            else if(!poscoChargerInfo.rsv_uid.equals(poscoChargerInfo.cardNum)) ret = true;
            else ret = false;
        }
        else if(poscoChargerInfo.rsv_flag == 2) ret = false;        //예약자,비예약자 둘다 불가
        else if(poscoChargerInfo.rsv_flag == 3)
        {
            //예약자만 사용가능(예약시작시간 10분 전후)
            if(poscoChargerInfo.rsv_uid.equals(poscoChargerInfo.cardNum)) ret = true;
            else ret = false;
        }

        return ret;
    }

    /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();        //충전 시작시간 설정
        poscoChargerInfo.chargingStartTime = chargeData.chargeStartTime;

        //add by si. 20200528 - 차지비 충전 종료시간 설정
        int usersettingTime = 0;

        if(cpConfig.useKakaoNavi){          //예약기능 사용안함
            if (poscoChargerInfo.isRemoteCharge) {
                usersettingTime = poscoChargerInfo.remoteStartChargingTimeLimit;
            } else {
                //사용자가 설정한 시간으로 설정
                usersettingTime = poscoChargerInfo.userSetTime;
                usersettingTime = usersettingTime * 60;       //초단위로 변경
            }
        }
        else{
            //원격충전일 경우 수신된 충전시간(분) 만큼 설정하고, 원격이아닐 경우 max 시간만큼 충전수행
            if (poscoChargerInfo.isRemoteCharge) {
                usersettingTime = poscoChargerInfo.remoteStartChargingTimeLimit;
            } else if (poscoChargerInfo.rsv_flag == 0) {
                if (meterConfig.lcdType.equals("None")) {
                    //사용자가 설정한 시간으로 설정
                    usersettingTime = poscoChargerInfo.userSetTime;
                } else {
                    //캐릭터 LCD일경우 최대시간으로 설정
                    usersettingTime = Integer.parseInt(poscoChargerInfo.maxsetTime);
                }
                usersettingTime = usersettingTime * 60;       //초단위로 변경
            } else if (poscoChargerInfo.rsv_flag == 1) {
                //비예약자만이 충전가능, 예약시작시간 30분전까지 충전가능
                if (!poscoChargerInfo.rsv_uid.equals(poscoChargerInfo.cardNum)) {
                    if(meterConfig.lcdType.equals("None")){
                        usersettingTime = poscoChargerInfo.userSetTime;
                    }
                    else{
                        usersettingTime = (int) poscoChargerInfo.rsv_leftMin - 30;
                    }
                    usersettingTime = usersettingTime * 60;
                }
            }
            else if (poscoChargerInfo.rsv_flag == 3) {
                //예약자만 충전가능
                if (poscoChargerInfo.rsv_uid.equals(poscoChargerInfo.cardNum)) {
                    usersettingTime = poscoChargerInfo.rsv_chargingTimeMin;
                    usersettingTime = usersettingTime * 60;
                }
            }
        }

        //종료시간 설정
        chargeData.chargeEndTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(chargeData.chargeEndTime);
        cal.add(Calendar.SECOND, usersettingTime);
        chargeData.chargeEndTime = cal.getTime();

        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        meterTimerCnt = 0;

        // 통신 충전 메시지 초기화
        poscoChargerInfo.reqAmoundSel = 0x01; // Full
        poscoChargerInfo.remainTime = usersettingTime;        //d1 - 충전 남은 시간(시작시이므로 설정된 시간값을 넘김.

        //poscoChargerInfo.payMethod = 0x02; // 전기요금 합산

        // 단가 설정
        setMemberCostUnit();

        if(!meterConfig.lcdType.equals("None")) {
            // 바로 Char 디스플레이 카운트 초기화
            dispChargingMeterStrCnt = TypeDefine.DISP_CHARGING_CHARLCD_PERIOD;
            dispChargingMeterBacklightCnt = 0; // 백라이트 카운터 초기화
        }

        //미터량 변화에 따른 충전종료 관련 변수 초기화 - add by si. 201026
        initTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        backupMeterval = 0;
        nonChangeMeterStopFlag = false;

        if(cpConfig.useOcpp) stopReason = StopTransaction.Reason.LOCAL;
    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작 시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {

            LogWrapper.v(TAG, "Start Charging");
            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            // 통신으로 충전시작 메시지를 보낸다.(ocpp)
            if(cpConfig.useOcpp){
                ocppSessionManager.startCharging(chargeData.curConnectorId, (int)lastMeterValue);

                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);
            }


            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);

            if(isDspTestMode || cpConfig.isAuthSkip) {}
            else {
                if (poscoChargerInfo.cpMode == 5) {
                    poscoChargerInfo.cpMode = 9;             //충전중 모드 변경 - add by si.200901
                    poscoChargerInfo.pre_cpMode = poscoChargerInfo.cpMode;
                    poscoCommManager.sendStartCharging();       //d1 전문 전송(자동인증이나 테스트모드가 아닐 경우에만 충전시작전문 전송)
                }
            }

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        dispMeteringString(new String[] {"Connecting to EV", "Wait a second"});
        if ( isConnected ) {
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
                setUIFlowState(UIFlowState.UI_RUN_CHECK);

                // 이미 Run이 된 상태이라면
                if ( isDspChargeRun ) {
                    onDspChargingStartEvent();
                }
                else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManger.changePage(PageID.CONNECT_CAR_WAIT);
                }
            }
            else if (  getUIFlowState() == UIFlowState.UI_CARD_TAG  ) {
                // add by scchoi 자동인증 추가, 20200610
                if ( cpConfig.isAuthSkip == true || isDspTestMode) {
                    setUIFlowState(UIFlowState.UI_RUN_CHECK);

                    // 이미 Run이 된 상태이라면
                    if ( isDspChargeRun ) {
                        onDspChargingStartEvent();
                    }
                    else {
                        // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                        pageManger.changePage(PageID.CONNECT_CAR_WAIT);
                    }
                }
            }
        }
        else {

        }
    }

    public synchronized void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING/* || getUIFlowState() == UIFlowState.UI_EIM_COMM_ERR*/) {

            if(isDspTestMode || cpConfig.isAuthSkip){}
            else {
                if (poscoChargerInfo.cpMode == 9) {
                    poscoChargerInfo.cpMode = 5;
                    poscoChargerInfo.pre_cpMode = poscoChargerInfo.cpMode;
//                    poscoCommManager.sendFinishCharging();
                }
            }

            //회원일 경우 언플러그 화면으로 이동 - edit by si. 210421
            if(chargeData.authType == TypeDefine.AuthType.MEMBER){
                poscoCommManager.sendFinishCharging();      //충전완료 f1 전송
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManger.changePage(PageID.UNPLUG);
            }else if(chargeData.authType == TypeDefine.AuthType.NOMEMBER){
                //취소결제로 가야하는지 실결제->취소로 가야하는지 판단 필요
                onPaymentCheckFinishCharging();
            }

            if(cpConfig.useOcpp) {
                //통신으로 종료 패킷을 보낸다.
                ocppSessionManager.stopCharging(chargeData.curConnectorId, (int)lastMeterValue, stopReason);

                // 커넥터 상태를 충전 종료중으로 바꾼다.(Status 메시지 보냄)
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);
            }

            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

            //Fault가 아닐 시, 정상 충전 완료
            if (!isDspFault) {
//                dispMeteringString(new String[]{"Finished.",
//                        "Unplug Cable",
//                        String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
//                        String.format("Cost: %dWon", poscoChargerInfo.curChargingCost)});

                LogWrapper.v(TAG, "Finish Charging:" + String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0) +
                        ", " + String.format("Cost: %dWon", poscoChargerInfo.curChargingCost));
            }
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);

    }

    // 1초에 한번씩 WatchDog 타이머를 수행한다.
    public void watchDogTimerProcess() {
        // 설정에 WatchDog를 사용하지 않거나 HardReset메시지를 받는경우에는 Skip함
        if ( cpConfig.useWatchDogTimer == false || isHardResetEvent == true ) return;

        if ( watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
            if ( watchDogTimerStartCnt == TypeDefine.WATCHDOG_START_TIMEOUT) {
                // WatchDog 타이머 시작(open과 함께 시작)
                watchDogTimer.openAndStart(WatchDogTimer.WATCHDOG_MAX_UPDATE_TIMEOUT);
                watchDogTimerStartCnt++; // 이후버터는 update만 실행
            }
            else {
                //Watch Dog Timer 갱신
                watchDogTimer.update();
                //Log.v(TAG, "WatchDog Update..");
            }
        }
        else {
            watchDogTimerStartCnt++;
        }
    }

    public void doInstallFirmware() {
        String updatePath = Environment.getExternalStorageDirectory()+TypeDefine.UPDATE_PATH;
        RemoteUpdater updater = new RemoteUpdater(mainActivity, updatePath, "update.apk");
        updater.doUpdateFromApk("com.joas.smartcharger");
    }

    /**
     * add by si. 20200604 차지비 예약정보 체크(타이머)
     */
    public void reserveUpdateInfo()
    {
        poscoCommManager.getReservInfo();

        //String val1 = poscoChargerInfo.rsv_orderNum;
    }

    /**
     * 업데이트 체크한다. isFWUpdateReady 가 true이고 UI가 초기화면인 경우에 업데이트 및 리셋 실시
     */
    void checkUpdateReady() {
        if ( isFWUpdateReady == true ) {
            if(meterConfig.lcdType.equals("None")) {
                if(cpConfig.useKakaoNavi){
                    if(getUIFlowState() == UIFlowState.UI_SELECT_AUTH){
                        doInstallFirmware();
                    }
                }
                else{
                    if (getUIFlowState() == UIFlowState.UI_READY) {
                        doInstallFirmware();
                    }
                }

            }
            else{
                if (getUIFlowState() == UIFlowState.UI_CARD_TAG) {
                    doInstallFirmware();
                }
            }
        }
    }

    /**
     * 월박스 리비전타입 RW1603 캐릭터LCD 밝기 조절관련 함수
     * add by si.210317
     * 0 : 최대밝기 , 100: 최소 밝기
     */
    private static int BRIGHTNESS_DEFAULT = 0;
    private int brightness;

    public void backlightDimming(int value)
    {
        brightness = value;
        setBrightness();
    }

    private void setBrightness() {
        Window w = mainActivity.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = (float)brightness/100;
        if (lp.screenBrightness<.01f) lp.screenBrightness=.01f;
        w.setAttributes(lp);
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */

    public void timerProcessSec() {
        //WatchDog 수행
        watchDogTimerProcess();

        //Fault 함수 수행
        onFaultEventProcess();

        // 날짜 변화 체크
        checkTransitionDay();

        //Update 검사
        checkUpdateReady();

        //add by ksi. 200604 예약정보 모니터링(카카오향은 예약시스템 안씀)
        if(!cpConfig.useKakaoNavi) reserveUpdateInfo();

        //전력량계 값 가져오기
        // meterVal = 전력량계
        //meter volt
        if ( mainActivity.getMeterService() != null ) {
            try {
                long meterVal = mainActivity.getMeterService().readMeter();
                double meterVolt = mainActivity.getMeterService().readMeterVoltage();
                double meterCurrent = mainActivity.getMeterService().readMeterCurrent();
                int meterseqnum = mainActivity.getMeterService().readSeqNumber();
                String meter_version = mainActivity.getMeterService().readMeterVersion();

                if(chargeData.meter_version.equals("")) {
                    chargeData.meter_version = meter_version;
                    poscoChargerInfo.chg_sw_version += " M:"+chargeData.meter_version;
                    poscoCommManager.SendInstallationInfo();
                }

                poscoChargerInfo.meterVal = (int) meterVal;

                Log.d(TAG, "Meter:" + meterVal + ", Volt:" + meterVolt + ", current:" + meterCurrent + ", m_seqnum:" + meterseqnum+", version:"+chargeData.meter_version);
                //add by si - 21.12.09 - MeterReadError상태 감지 추가
                MeterStatusMonitoring(meterVal);

                //add by si - 211130 - meter view program seqnum 증가상태 감지(없을시 UI재부팅)
                MeterviewSeqnumMonitor(meterseqnum);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;
                /**
                 * add by ksi
                 * 컨트롤 보드로 전압,전류,미터값 실시간 전송
                 */
                dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);
                dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);


                if (meterVal >= 0) {
                    dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);
                    if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                        if (lastMeterValue > 0) {
                            int gapMeter = (int) (meterVal - lastMeterValue);

                            if (gapMeter > 0) {
                                //gapMeter 최대 증가폭 1초당 0.5kw를 넘지못함. - add by si. 200831
                                if (gapMeter > 500) {
                                    gapMeter = 500;
                                }
                                chargeData.measureWh += gapMeter;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                } else {
                    // Meter Error !!!
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);
                    poscoChargerInfo.meterVal = (int) lastMeterValue;
                }
            } catch (Exception e) {
                LogWrapper.d(TAG, "Meter Err:" + e.toString());
            }
        }

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        dspVersion = rxData.version; // DSP 버전 정보 저장

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            //충전 시간
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();
            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            //충전 남은 시간
            chargeData.chargingRemainTime = chargeData.chargeEndTime.getTime() - (new Date()).getTime();
            int remianTimeSec = (int)(chargeData.chargingRemainTime/1000);


            //시간 및 충전관련 전달정보 저장
            poscoChargerInfo.remainTime = remianTimeSec;
            poscoChargerInfo.chargingTime = chargingTimeSec;
            poscoChargerInfo.curChargingKwh = (int)(chargeData.measureWh/10);
            //poscoChargerInfo.curChargingCost = (int)chargeData.chargingCost;      //delete by si. 20200528, 차지비는 충전종료 후 1f의 요금정보로 표시, 상시체크 X

            if(meterConfig.lcdType.equals("None")) {
                if (chargeData.authType == TypeDefine.AuthType.NOMEMBER) {
                    if (cpConfig.useKakaoCost)
                        chargeData.chargingCost = (int) ((double) (poscoChargerInfo.curChargingKwh * 0.01) * chargeData.chargingUnitCost);
                    else
                        chargeData.chargingCost = (int) ((double) (poscoChargerInfo.curChargingKwh * 0.01) * (double) poscoChargerInfo.nonMemberCostUnit * 0.01);
                    poscoChargerInfo.curChargingCost = (int) chargeData.chargingCost;      //비회원일경우 f1에 충전요금 올려줘야 함
                    if (poscoChargerInfo.paymentPreDealApprovalCost > 0) {
                        if (chargeData.chargingCost >= poscoChargerInfo.paymentPreDealApprovalCost) {
                            chargeData.chargingCost = poscoChargerInfo.paymentPreDealApprovalCost;
                            onChargingStop();
                        }
                    }
                }
            }else {
                //캐릭터LCD표시
                dispChargingMeterStrCnt++;
                if (dispChargingMeterStrCnt > TypeDefine.DISP_CHARGING_CHARLCD_PERIOD) { // 8초주기 2초*4개 문구
                    dispChargingMeterStrCnt = 0;

                    //add by si. 20200528 : 차지비 충전중 표시는 사용량, 현재충전시간, 잔여시간 표시로 한다.
                    dispMeteringString(new String[]{"Charging...",
                            String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
                            String.format("Elapse:%02d:%02d:%02d", chargingTimeSec / 3600, (chargingTimeSec % 3600) / 60, (chargingTimeSec % 60)),
                            String.format("Remain:%02d:%02d:%02d", remianTimeSec / 3600, (remianTimeSec % 3600) / 60, (remianTimeSec % 60))}, false);
                }
            }

            //차지비 충전종료 모니터링
            if(remianTimeSec < 1) onChargingStop();     //add by si. 20200529 : cv, 충전설정 잔여시간이 1초 미만일 경우 충전 종료 요청
            //충전량 변화 모니터링 종료시퀀스 - add by si. 20201026
            nonChangeMeterStopFlag = getMeterChargingStopFlag();
            if(nonChangeMeterStopFlag) onChargingStop();     //add by si. 201026 - 충전량 변화가 3분동안 없을경우 충전종료 요청

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            if (chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD) == 0) {
                if(isDspTestMode || cpConfig.isAuthSkip){}
                else poscoCommManager.sendChargingStatus();
            }

            if ( rxData.get400Reg(DSPRxData2.STATUS400.FINISH_CHARGE) == true ) {
                onFinishChargingEvent();
            }
        }

        // Event에서 poll로 바꿈.
        if ( rxData.get400Reg(DSPRxData2.STATUS400.STATE_DOOR) == false ) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true ) {
            onConnectedCableEvent(true);
        }

        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
//        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
        if ( getUIFlowState() == UIFlowState.UI_UNPLUG ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++ > 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
                if(isDspTestMode || cpConfig.isAuthSkip){}
                else poscoCommManager.sendUnplug();

                if(meterConfig.lcdType.equals("None")){
                    //충전 완료로 이동
                    setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
                    pageManger.changePage(PageID.FINISH_CHARGING);
                }else{
                    //캐릭터LCD일경우 바로 대기화면으로 이동
                    onPageCommonEvent(PageEvent.GO_HOME);
                }
            } else if(finishChargingViewCnt++ == 5) {
                //Fault가 아닐 시, 정상 충전 완료
                dispMeteringString(new String[]{"Finished.", "Unplug Cable",
                        String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
                        String.format("Cost: %dWon", poscoChargerInfo.curChargingCost)});
            }
        }

        if(!meterConfig.lcdType.equals("None")) {
            if (meterConfig.lcdType.equals("RW1603")) {
                //RW1603 charlcd backlight on monitor
                if (dispChargingMeterBacklightCnt == 0) {
                    backlightDimming(BRIGHTNESS_DEFAULT);
                    isBacklightOn = true;
                }
                // CHARLCD Backlight Off Timeout
                if (dispChargingMeterBacklightCnt > TypeDefine.DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT) {
                    try {
                        if (isBacklightOn) {
                            backlightDimming(90);   //밝기 어둡게
                            isBacklightOn = false;
                        }
                    } catch (Exception e) {
                        dispChargingMeterBacklightCnt = 0;
                    }
                } else {
                    dispChargingMeterBacklightCnt++;
                }
            } else {
                // CHARLCD Backlight Off Timeout
                if (dispChargingMeterBacklightCnt > TypeDefine.DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT) {
                    try {
                        IMeterAidlInterface meterService = mainActivity.getMeterService();
                        meterService.setCharLCDBacklight(false);
                    } catch (Exception e) {
                        dispChargingMeterBacklightCnt = 0;
                    }
                } else {
                    dispChargingMeterBacklightCnt++;
                }
            }
        }

        //add by si.200729 - 407번지 1bit Testmode 모니터링
        isDspTestMode = rxData.get407Reg(DSPRxData2.STATUS407.TEST_MODE);

        if(isDspTestMode!=backupDspTestModeFlag) {

            if (isDspTestMode)
                dispMeteringString(new String[]{"Welcome! ChargEV", "Test Mode", "Connect Cable"});
            else
                dispMeteringString(new String[]{"Welcome! ChargEV", "Disconnected", "Connecting...", "ID:" + cpConfig.stationID + cpConfig.chargerID});

            backupDspTestModeFlag = isDspTestMode;
        }

    }
    //add by si.201026 - 충전중 충전량 변화에 따른 충전종료 플래그 리턴 함수
    public boolean getMeterChargingStopFlag(){
        boolean retval = false;
        try {
            if (poscoChargerInfo.curChargingKwh != backupMeterval) {
                backupMeterval = poscoChargerInfo.curChargingKwh;
                initTime = System.currentTimeMillis();
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초
            }
            else if(poscoChargerInfo.curChargingKwh == backupMeterval){
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초

                if(distanceTime == TypeDefine.METERING_CHANGE_TIMEOUT){
                    retval = true;
                }
                else retval = false;
            }
        }catch (Exception e ) {

        }

        return retval;
    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if ( curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue ) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);

            poscoCommManager.checkChangeDay(); // 오래된 로그를 지우는 부분
        }
    }


    /**
     * 계량기 프로그램 동작여부 판단
     * seqnum 변화없을 경우 충전중이 아닐때 UI 재부팅
     * @param seqnum 계량기프로그램에서 0~255까지 증가되는 값을 모니터링
     */
    int meter_seqnum_backup = 0;
    int meter_seqnum = 0;
    int meter_comm_errcnt = 0;

    public void MeterviewSeqnumMonitor(int seqnum){
        //1초마다 실행됨
        meter_seqnum = seqnum;
        if((meter_seqnum == meter_seqnum_backup) || (meter_seqnum == 0)){
            meter_comm_errcnt++;
        }
        else {
            meter_seqnum_backup = meter_seqnum;
            meter_comm_errcnt = 0;
        }

        //err count 감지
        if(meter_comm_errcnt >= TypeDefine.COMM_METER_TIMEOUT){
            //충전중이 아닐경우에 리셋 진행
            if(getUIFlowState() != UIFlowState.UI_CHARGING) {
                if(getUIFlowState() == UIFlowState.UI_SELECT_AUTH ||
                getUIFlowState() == UIFlowState.UI_READY ||
                getUIFlowState() == UIFlowState.UI_CARD_TAG){
                    //UI 리셋
                    meter_comm_errcnt = 0;
                    resetRequest(true);
                }
            }
        }

    }

    /***
     * 계량기 통신 오류시 200-7 bit로 계량기 통신 오류 폴트 알림
     */
    //add by si.201209 - 전력량계 오류상태 모니터링함수
    boolean isMeterCommErr = false;
    boolean isMeterCommErr_backup = false;
    public void MeterStatusMonitoring(long m_meterVal) {
        try {
            if (m_meterVal == -1) isMeterCommErr = true;
            else isMeterCommErr = false;

            if (isMeterCommErr != isMeterCommErr_backup) {
                if (isMeterCommErr) {
                    chargeData.ismeterCommError = true;
                    //충전중 발생했을 경우 충전 중지.
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
                        onChargingStop();
                    }
                    else{
                        if(cpConfig.useKakaoNavi){
                            if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                                    getUIFlowState() != UIFlowState.UI_SELECT_AUTH)  {
                                onPageStartEvent();
                            }
                        }
                        else{
                            if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                                    getUIFlowState() != UIFlowState.UI_READY)  {
                                onPageStartEvent();
                            }
                        }
                    }
                    //Meter Read error일 경우
                    //dsp로 에러신호 전송
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, true);
                    LogWrapper.v(TAG, "MeterError occured!");

                    //fault messagebox 띄우기
                    chargeData.faultBoxContent = "";
                    chargeData.faultBoxContent += "[20007] 전력량계 통신오류 발생(" + String.valueOf(chargeData.dspChannel)+")";
                    chargeData.faultBoxContent += "\r\n";
                    pageManger.showFaultBox();



                } else if (!isMeterCommErr) {
                    chargeData.ismeterCommError = false;
                    //미터기 상태 정상일 경우
                    //dsp 미터에러신호 복구 및 기타변수 초기화
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, false);
                    LogWrapper.v(TAG, "MeterError Restored!");
                    pageManger.hideFaultBox();
                }
                isMeterCommErr_backup = isMeterCommErr;
            }
        } catch (Exception e) {

        }
    }

    public void stopWatdogTimer() {
        watchDogTimerStartCnt = 0;
        watchDogTimer.stopAndClose();
    }

    /**
     * App을 재시작한다.
     */
    public void runSoftReset(int timeout) {
        //Soft Reset인 경우 화면을 초기화 한다.
        TimeoutTimer timer = new TimeoutTimer(timeout, new TimeoutHandler() {
            @Override
            public void run() {
                // App 재시작
                Intent i = mainActivity.getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(mainActivity.getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("RestartReason", "SoftReset");
                mainActivity.startActivity(i);
            }
        });
        timer.startOnce();
    }

    void rebootSystem() {
        try {
            PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
            pm.reboot("force");

        } catch (Exception e ) {

        }
    }

    public void resetRequest(boolean isHard) {
        if ( isHard == true ) {
            isHardResetEvent = true;

            // 충전중이라면 충전을 중지한다.
            if (  flowState == UIFlowState.UI_CHARGING ) {
                onChargingStop();
            }

            // 약 15초뒤에 Reset됨
            if ( watchDogTimer.isStarted == false ) watchDogTimer.openAndStart(15);
            else watchDogTimer.update();

            //add by si. 200624 캐릭터 LCD reboot 표시
            dispMeteringString(new String[] {"Reboot System..", "Wait a second"});

            // 메시지 박스를 띄운다.
            chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_hard_reset_title);
            chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_hard_reset_content);
            chargeData.messageBoxTimeout = 15; // WatchDog 시간 최대가 16초)
            pageManger.showMessageBox();

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            rebootSystem();
                        }
                    }, 10000);
                }
            });

        }
        else { // Soft Reset
            // 충전중이라면 충전을 중지한다.
            if (flowState == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }

            // 메시지 박스를 띄운다.
            chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_soft_reset_title);
            chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_soft_reset_content);
            pageManger.showMessageBox();

            runSoftReset(chargeData.messageBoxTimeout*1000);
        }
    }

    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
    }

    public void onFinishApp() {
        if ( cpConfig.useWatchDogTimer == true ) {
            if ( watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
                watchDogTimer.stop();
                watchDogTimer.close();
                timerSec.cancel();
            }
        }

        mainActivity.finish();
    }

    /**
     *  DSP에서 오는 이벤트를 처리한다.
     * @param channel 해당 채널값
     * @param idx 상태값 Index
     * @param val
     */
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
        if ( channel == chargeData.dspChannel ) {

            LogWrapper.v(TAG, "DSP Status Change:"+ idx.name()+" is "+val);

            switch (idx) {
                case READY:
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

                    isDspReady = val;
                    break;

                case AVAL_CHARGE:
                    isDspAvalCharge = val;
                    break;

                case STATE_PLUG:
                    isDspPlug = val;
//                    onConnectedCableEvent(true);
                    onConnectedCableEvent(isDspPlug);
                    break;

                case STATE_DOOR:
                    isDspDoor = val;
                    if ( isDspDoor == false ) { // 도어 오픈
                        //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
                    }
                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if ( val == true ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
                        onDspChargingStartEvent();
                    }
                    break;

                case CG_STARTSTOPBT:
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
                        dispTempMeteringString(new String[]{"Stopping...", "Wait a second.."}, 6000);
                    }
                    break;

                case FINISH_CHARGE:
                    isDspChargeFinish = val;
                    if ( val == true ) onFinishChargingEvent();
                    break;

                case FAULT:
                    isDspFault = val;
                    onFaultEventProcess();
                    break;

                case STATE_RESET:
                    break;

                case CONNECTOR_LOCK_A:
                    break;
            }
        }
    }

    int faultCode = 0;      //알람 자료 전송용 장애코드(DSP)
    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if (fInfo.isRepair == false) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";

                faultCode = dispDspErrorString(fInfo.errorCode);

//                poscoCommManager.SendAlarm(faultCode, AlarmCode.STATE_OCCUR);      //알람 자료 전송
                poscoCommManager.SendFaultStat(faultCode, AlarmCode.STATE_OCCUR);        //add by si. 200605 - 차지비 폴트알람 b1
            }
        }

        try {

            int i = 0;
            for (FaultInfo fInfo : faultList) {
                if (fInfo.isRepair) {
                    faultList.remove(i);
                } else {
                    i++;
                }
            }
        }
        catch (Exception e)
        {}
    }


    /**
     * DSP Fault  캐릭터 LCD 처리
     *
     * by Lee 20200518
     * @param errorCode
     */
    private int dispDspErrorString(int errorCode)
    {
        String usage = "";
        String cost = "";
        if (getUIFlowState() == UIFlowState.UI_CHARGING)
        {
            usage = String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0);
            cost = String.format("Cost: %dWon", poscoChargerInfo.curChargingCost);
        }

        boolean isCharging = false;
        if (usage.length() > 0) {
            isCharging = true;
        }


        int address = Integer.parseInt((Integer.toString(errorCode)).substring(0, 3));
        int bit = Integer.parseInt((Integer.toString(errorCode)).substring(3, 5));
        int faultCode = 0;

        switch (address)
        {
            case 423:
                faultCode = dispDspErrorString423(isCharging, bit, usage, cost);
                break;
            case 424:
                break;
            case 425:
                faultCode = dispDspErrorString425(isCharging, bit, usage, cost);
                break;
        }

        return faultCode;
    }

    /**
     * 423번지 Fault 메시지 처리 (캐릭터 LCD)
     * Fault에 대한 장애코드 데이터 리턴(서버 알람자료 전송)
     *
     * by Lee 20200518
     * @param bit
     */
    private int dispDspErrorString423(boolean isCharging, int bit, String usage, String cost)
    {
        int faultCode = AlarmCode.ETC_ERR;      // 알람자료 전송용 장애코드
        switch (bit) {
            case 0:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_0, usage, cost);        //비상정지
                faultCode = AlarmCode.EMERGENCY;
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_1, usage, cost);
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_2, usage, cost);
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_3, usage, cost);
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_4, usage, cost);        //오류코드 : 9, 전자접촉기/릴레이 융착
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_5, usage, cost);        //오류코드 : 9, 전자접촉기/릴레이 융착
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_6, usage, cost);        //오류코드 : 1, 전자접촉기/릴레이 이상
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 7:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_7, usage, cost);        //오류코드 : 1, 전자접촉기/릴레이 이상
                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 8:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_8, usage, cost);
                break;
            case 9:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_9, usage, cost);        //오류코드 : 6
                faultCode = AlarmCode.ERR_CODE_6;
                break;
            case 10:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_10, usage, cost);
                faultCode = AlarmCode.CHGR_INDOOR_TEMP_ERR;
                break;
            case 11:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_11, usage, cost);
                //faultCode = AlarmCode.CHGR_INDOOR_COMM_ERR;
                break;
            case 12:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_12, usage, cost);
                //faultCode = AlarmCode.CHGR_INDOOR_COMM_ERR;
                break;
            case 13:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_13, usage, cost);       //오류코드 : 12
                faultCode = AlarmCode.ERR_CODE_12;
                break;
            case 14:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_14, usage, cost);
//                faultCode = AlarmCode.METER_COMM_ERR;
                break;
            case 15:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_15, usage, cost);       //오류코드 : 5
                faultCode = AlarmCode.ERR_CODE_5;
                break;
        }
        LogWrapper.v(TAG, "DSP ERROR-423-" + Integer.toString(bit));

        return faultCode;
    }


    /**
     * 충전 중 Fault 발생 시, 충전량, 충전요금 포함하여 보여준다
     * 아닐 시, 기본 Fault 메시지만 보여준다.
     *
     * by Lee 20200518
     * @param isCharging
     * @param errorMsg
     * @param usage
     * @param cost
     */
    private void SetDispErrorString(boolean isCharging, String errorMsg, String usage, String cost)
    {
        //충전 중 Fault 시, Fault 메시지와 충전완료 데이터 (충전량, 충전요금)을 LCD에 표시한다.
        if (isCharging)
            dispTempMeteringString(new String[]{"Error DSP", "ERRCODE:" + errorMsg, usage, cost}, 3000);
        else
            dispTempMeteringString(new String[]{"Error DSP", "ERRCODE:" + errorMsg}, 3000);
    }

    /**
     * 425번지 Fault 메시지 처리 (캐릭터 LCD)
     *
     * by Lee 20200518
     * @param bit
     */
    private int dispDspErrorString425(boolean isCharging, int bit, String usage, String cost)
    {
        int faultData = AlarmCode.ETC_ERR;

        switch (bit) {
            case 0:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_0, usage, cost);        //errcode : 7
                faultData = AlarmCode.ERR_CODE_7;
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_1, usage, cost);
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_2, usage, cost);        //errcode : 2
                faultData = AlarmCode.ERR_CODE_2;
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_3, usage, cost);
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_4, usage, cost);        //errcode : 8
                faultData = AlarmCode.ERR_CODE_8;
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_5, usage, cost);        //errcode : 3
                faultData = AlarmCode.ERR_CODE_3;
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_6, usage, cost);        //errcode : 10
                faultData = AlarmCode.ERR_CODE_10;
                break;
        }
        LogWrapper.v(TAG, "DSP ERROR-425-" + Integer.toString(bit));

        return faultData;
    }


    public synchronized void onFaultEventProcess() {
        Vector<FaultInfo> fList = faultManager.scanFaultV2(chargeData.dspChannel);
        boolean isEmergency = faultManager.isFaultEmergency(chargeData.dspChannel);

        if ( fList.size() > 0 ) {
            boolean isContain = false;
            for (FaultInfo fInfo : fList) {
                for (FaultInfo fInfoCur: faultList) {
                    if ( fInfoCur.id == fInfo.id ) {
                        isContain = true;
                        fInfoCur.isRepair = fInfo.isRepair;
                    }
                }
                // 새로운 이벤트인경우
                if ( isContain == false ) {
                    FaultInfo newInfo = new FaultInfo(fInfo.id, fInfo.errorCode, fInfo.errorMsg, fInfo.isRepair);
                    faultList.add(newInfo);
                }
            }
        }

        if ( isPreDspFault != isDspFault ) {
            if ( isDspFault == true ) {
                // 충전충이라면 충전을 중지한다.
                if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                    if(cpConfig.useOcpp){
                        if (isEmergency == true) stopReason = StopTransaction.Reason.EMERGENCY_STOP;
                        else stopReason = StopTransaction.Reason.OTHER;
                    }
                    onChargingStop();
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                        (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState()!=UIFlowState.UI_READY || getUIFlowState()!=UIFlowState.UI_SELECT_AUTH))  {
                    onPageStartEvent();
                }

                fillFaultMessage();
                pageManger.showFaultBox();
            }
            else {
                if (faultCode != 0)     //알람해제 중복 처리
//                    poscoCommManager.SendAlarm(faultCode, AlarmCode.STATE_RESTORE);      //알람 자료 전송
                    poscoCommManager.SendFaultStat(faultCode,AlarmCode.STATE_RESTORE);      //add by si. 200605 차지비 폴트알림 b1전송


                faultCode = 0;
                pageManger.hideFaultBox();
                onCheckHideFault();

            }
            isPreDspFault = isDspFault;
        }

        if(cpConfig.useOcpp){
            if(!chargeData.isdspCommError && !chargeData.ismeterCommError){
                if ( isDspFault == true ) {
                    if ( chargeData.ocppStatus != StatusNotification.Status.FAULTED ) {
                        // To.Do.. Error Code..Set
                        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FAULTED);
                    }
                }
                else if ( chargeData.ocppStatus == StatusNotification.Status.FAULTED ) {
                    chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                }
            }
        }
        // 긴급버턴 이벤트 발생
        if ( isEmergencyPressed != isEmergency ) {
            if (isEmergency == true) {
                pageManger.showEmergencyBox();
            } else { // 긴급 버턴 해제
                pageManger.hideEmergencyBox();
                onCheckHideFault();
            }
            isEmergencyPressed = isEmergency;
        }
    }

    /**
     * Fault 에서 정상으로 돌아왔을 시, 시퀀스에 맞는 캐릭터 LCD 문자열 표시
     *
     * by Lee 20200518
     */
    private void onCheckHideFault()
    {
        if (getUIFlowState() == UIFlowState.UI_CARD_TAG) {
            if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
            else dispMeteringString(new String[]{"Welcome! ChargEV", "Tag Card for EV", "ID:" + cpConfig.stationID + cpConfig.chargerID, "Ver:" + TypeDefine.SW_VERSION});
        }
        else if (getUIFlowState() == UIFlowState.UI_AUTH_WAIT)
            dispMeteringString(new String[] {"Card Verifing...", "Wait a second"});
        else if (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT)
            dispMeteringString(new String[] {"Card Check Done.", "Connect Cable"});
        else if (getUIFlowState() == UIFlowState.UI_RUN_CHECK)
            dispMeteringString(new String[] {"Start Charging"});
        else if (getUIFlowState() == UIFlowState.UI_CHARGING)
            dispMeteringString(new String[]{"Charging...",
                    String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
                    String.format("Elapse:%02d:%02d:%02d", poscoChargerInfo.chargingTime / 3600, (poscoChargerInfo.chargingTime % 3600) / 60, (poscoChargerInfo.chargingTime % 60) ),
                    String.format("Remain:%02d:%02d:%02d", poscoChargerInfo.remainTime / 3600, (poscoChargerInfo.remainTime % 3600) / 60, (poscoChargerInfo.remainTime % 60) )}, false);
        else if (getUIFlowState() == UIFlowState.UI_FINISH_CHARGING) {
            dispMeteringString(new String[]{"Finished.",
                    "Unplug Cable",
                    String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
                    String.format("Cost: %dWon", poscoChargerInfo.curChargingCost)});

            LogWrapper.v(TAG, "Finish Charging:" + String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0) +
                    ", " + String.format("Cost: %dWon", poscoChargerInfo.curChargingCost));
        }

    }

    //http download test method
    String fileName = "1.zip";
    String fileURL = "http://joascharger.co.kr:8888/data/1048576/1";            //웹서버 쪽 파일이 있는 경로

    HttpDownloadThread dThread;
    public void startHttpDownload() {
        String savePath = "";       //파일 저장경로
        if (poscoChargerInfo.destFlag2 == 0x01) {
            //fw download일 경우 경로 설정
            savePath = Environment.getExternalStorageDirectory() + TypeDefine.UPDATE_PATH;
        }
        else if(poscoChargerInfo.destFlag2 == 0x05 || poscoChargerInfo.destFlag2 == 0x06 || poscoChargerInfo.destFlag2 == 0x07){
            savePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_PATH;
        }
        File dir = new File(savePath);
        //해당경로의 폴더가 존재하지 않을경우 생성
        if (!dir.exists()) {
            dir.mkdir();
        }

        dThread = new HttpDownloadThread(poscoChargerInfo.remoteURL, savePath, poscoChargerInfo.destFlag2, this);
        dThread.start();
    }
    public void completeHttpDownload() {
        LogWrapper.d("HTTP_FOTA", " HTTP download complete!!");
        dThread.interrupt();
        LogWrapper.d("HTTP_FOTA", " HTTP download thread stopped..");

        String filePath = "";
        String filename = "";
        //서버로 다운로드 완료 알람 전송
        poscoCommManager.SendAlarm(AlarmCode.DOWNLOAD_COMPLETE, AlarmCode.STATE_OCCUR);

        if (poscoChargerInfo.destFlag2 == 0x01) {
            filePath = Environment.getExternalStorageDirectory() + TypeDefine.UPDATE_PATH;
            onhttpFirmwarDownloadComplete(filePath);
        } else if (poscoChargerInfo.destFlag2 == 0x05 || poscoChargerInfo.destFlag2 == 0x06 || poscoChargerInfo.destFlag2 == 0x07) {
            //먼저 기존 멤버 정보 모두 지우기
            if(poscoChargerInfo.destFlag2 == 0x05) poscoCommManager.doRemoveAllMember();

            //추가할 멤버파일 읽기 및 db작업
            doMemberFileRead(poscoChargerInfo.destFlag2);
        }

    }

    public void doMemberFileRead(byte destflag) {
//        poscoCommManager.doRemoveAllMember();       //test
        int listcnt = 0;
        String fileContent = "";
        String filePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_PATH;
        try {
            StringBuffer sb = new StringBuffer();
            InputStream is = null;
            if (destflag == 0x05) is = new FileInputStream(filePath + "/psmember.txt");
            else if (destflag == 0x06) is = new FileInputStream(filePath + "/psmemadd.txt");
            else if (destflag == 0x07) is = new FileInputStream(filePath + "/psmemdel.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.length() >= 32) line = line.substring(0, 32);
                sb.append(line + "\n");
            }
            br.close();
            is.close();

            fileContent = sb.toString();
            String[] cardList = fileContent.split("\n");
            listcnt = cardList.length;

            if (destflag == 0x05 || destflag == 0x06) memberAddThread(cardList, listcnt, destflag);
            else if (destflag == 0x07) poscoCommManager.doDelMember(cardList, listcnt);

        } catch (Exception e) {
            LogWrapper.e("doMemberFileRead", "MemberFileReadError : " + e.getMessage());
        }
    }

    AddMemThread addMemThread;
    public void memberAddThread(String[] cardlist, int cnt, byte destflag) {

        addMemThread = new AddMemThread(this, cnt, cardlist);
        addMemThread.start();
        if (destflag == 0x05) LogWrapper.d("memberAddThread", " TOTAL MEM ADD Thread Start..");
        else LogWrapper.d("memberAddThread", " ADD MEM Thread Start..");
    }
    public void completeMemAdd(){
        addMemThread.interrupt();
        LogWrapper.d("memberAddThread", " Add Member Complete!..Thread stopped.");
    }

    public void onhttpFirmwarDownloadComplete(String path) {
        //unzip
        try {
            ZipUtils.unzip(path + "/update.zip", path, false);
            // 성공시 처리
            LogWrapper.v(TAG, "Firmware Unzip Successed");

            // Update 완료 Flag 시작(충전시가 아닐때 업데이트시작)
            isFWUpdateReady = true;
        } catch (Exception e) {
            LogWrapper.v(TAG, "Firmware Unzip Failed");
        }
    }


    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 사용안함
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {
        chargeData.isdspCommError = isError;

        if (isError == true) {
            // 충전충이라면 충전을 중지한다.
            if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                onChargingStop();
            }
            else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState()!=UIFlowState.UI_READY || getUIFlowState()!=UIFlowState.UI_SELECT_AUTH))  {
                onPageStartEvent();
            }

            pageManger.showDspCommErrView();
            dispMeteringString(new String[]{"INTER COMM ERR.", "ERROR CODE:11"});

            //UI 상태 내부통신장애로 변경
            if(!cpConfig.useTL3500BS){
                if(cpConfig.useSehanRF) rfidReader.rfidReadRelease();   //rf리더기 오토스캔 잠금 전환
            }
//            setUIFlowState(UIFlowState.UI_EIM_COMM_ERR);
            poscoCommManager.SendFaultStat(AlarmCode.ERR_CODE_11, AlarmCode.STATE_OCCUR);        //add by si. 200605 - 차지비 폴트알람 b1 전송(발생)
            LogWrapper.e(TAG, "DSP-UI Comm Error!!");
        } else {
            pageManger.hideDspCommErrView();
            poscoCommManager.SendFaultStat(AlarmCode.ERR_CODE_11, AlarmCode.STATE_RESTORE);        //add by si. 200605 - 차지비 폴트알람 b1 전송(복구)
//            onPageStartEvent();
            if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                onChargingStop();
            }
            else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState()!=UIFlowState.UI_READY || getUIFlowState()!=UIFlowState.UI_SELECT_AUTH))  {
                onPageStartEvent();
            }
            LogWrapper.e(TAG, "DSP-UI Comm Recovery.");
        }


    }

    /**
     * add by si. 모뎀 통신메시지 수신(우선 인포마크모뎀 부터 적용, 추후 모뎀별 기능추가 필요)
     */
    public void onRecvModemMDNInfo(String pnum)
    {
        String recv_data = pnum;

        try {
            //전화번호 부분 분리
            String[] parseList = recv_data.split("\\+");
            String mdn = "0" + parseList[2].substring(2, 12);
            poscoChargerInfo.mtoPhoneNumber = mdn;

            LogWrapper.v("MDN", poscoChargerInfo.mtoPhoneNumber);
        }catch (Exception e)
        {
            LogWrapper.v(TAG, e.toString());
        }

        if(poscoChargerInfo.h1_mdnNumIsSpace) {
            poscoCommManager.SendInstallationInfo();
        }
    }
    public void onRecvModemRSSiInfo(String rssi)
    {
        try {
            String raw_data = rssi;
            String[] parseList = raw_data.split(",");
            String[] rssi_list = parseList[22].split(":");
            String tmp_modem_rssi = rssi_list[1].substring(1);
            String[] splitval = tmp_modem_rssi.split("\\.");
            String modem_rssi = splitval[0];
            poscoChargerInfo.mtomRssi = modem_rssi + "00";

            LogWrapper.v("RSSI", poscoChargerInfo.mtomRssi);

        }catch (Exception e)
        {
            LogWrapper.v(TAG, e.toString());
        }
    }

    //================================================
    // 통신 메시지 수신
    //================================================

    /***
     * 서버로부터 수신받은 단가정보 저장(G1)
     * @param pinfo : 단가정보
     */
    @Override
    public void onRecvCostInfo(PoscoChargerInfo pinfo) {
        poscoChargerInfo.saveCostInfo(mainActivity.getBaseContext(), Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, TypeDefine.COST_INFO_FILENAME);
    }

    /**
     * 정보 변경 요청
     * @param kind 0x01: 충전 정보시스템 IP, 0x02: 충전시간 설정 범위
     * @param value
     */
    @Override
    public void onChargerInfoChangeReq(int kind, String value) {
        String value_N1 = value.trim();

        if(!value_N1.equals(""))
        {
            if(kind == 0x01)
            {
                //충전기 서버IP 설정값 변경 및 적용
                getCpConfig().serverIP = value_N1;
                getCpConfig().saveConfig(mainActivity);
                //리셋진행
                resetRequest(true);
            }
            else if(kind == 0x02)
            {
                //충전기 설정시간 관련 값 저장
                String[] setTimeVal = value_N1.split(",");
                poscoChargerInfo.settimeInterval = setTimeVal[0];
                poscoChargerInfo.minsetTime = setTimeVal[1];
                poscoChargerInfo.maxsetTime = setTimeVal[2];
                poscoChargerInfo.saveCpSetTimeInfo(mainActivity.getBaseContext(),Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH,TypeDefine.CHGSETTIME_INFO_FILENAME);

                //add by ksi. 20200529 - b1전문에 포함된 충전기 설정시간 전송값 저장
                poscoChargerInfo.chargingAvalTime = value;
            }
            poscoCommManager.sendChargerStatus();
        }
    }

    /**
     * 서버에서 충전시작(d1)에 대한 응답이 nack이면 시작된 충전을 종료시켜야 한다.
     * 종료후에는 f1을 전송해줘야 함.
     * @param rspCode 0x06 충전가능, 0x15 충전불가
     * @param rspReason 거절사유 : (0x11 : UID인증에러, 0x12 : 충전Credit단가 확인에러, 0x13:Credit부족에러, 0x14:충전시간 에러, 0x15:예약시간 에러, 0x16:충전소에러, 0x17:충전기에러)
     */
    public void onRecvStartChargingResp(byte rspCode, byte rspReason)
    {
        if(rspCode == 0x06 || isLocalAuth)  //로컬인증일 경우 1d응답 상관없이 진행행
       {
            //1d 정상응답일 경우 아무것도 하지 않음
        }
        else if(rspCode == 0x15) {
            if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }
        }
    }

    /**
     * 차지비 비회원 핸드폰 인증 요청(p1)에 대한 응답 이벤트 처리 함수
     * @param authNum : 승인번호(2자리)
     * @param authResult 승인결과
     *                   0x00:정상
     *                   0x01:한도승인 취소만 없음(실충전은 진행하지 않았으며, 한도승인 취소결제건이 서버로 전달되지 못했을 경우)
     *                   0x02:한도승인 취소 및 실승인 없음(충전이 이루어지고, 실결제 내역과 선결제 취소 내역이 서버에 없을 경우)
     *                   0x03:부분취소 없음(부분취소 결제시퀀스 사용할때만 적용, 현재 차지비 사용X)
     * @param prepayauthnum : 한도승인 승인번호(한도승인 취소시 사용됨)
     * @param prepayDatetime : 한도승인 승인일시(한도승인 취소시 사용됨)
     * @param prepayPrice : 한도승인 승인금액(한도승인 취소시 사용됨)
     * @param usePayReal : 실제 고객이 사용한 금액(authResult 가 0x02일때 사용됨)
     */
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal) {
        try {
            poscoChargerInfo.payment_authNum = authNum;
            poscoChargerInfo.nomemAuthResultFlag = authResult;
            poscoChargerInfo.recvfromserver_prepayauthnum = prepayauthnum.substring(0, 8);
            poscoChargerInfo.recvfromserver_prepayDatetime = prepayDatetime;
            poscoChargerInfo.recvfromserver_prepayPrice = prepayPrice;
            poscoChargerInfo.recvfromserver_usePayReal = usePayReal;
        } catch (Exception ex) {
            LogWrapper.d(TAG, ", onRecvCellAuthResp: " + ex.toString());
        }
    }

    /**
     * 미처리 결제 완료 요청(q1)에 대항 응답 처리 부분
     * @param authResult
     * @param recvPrepayApprovalNum
     * @param recvPrepayDatetime
     * @param recvPrepayPrice
     * @param recvusePrice
     */
    public void onRecvMissingPaymentCompleteResp(byte authResult, String recvPrepayApprovalNum, String recvPrepayDatetime,int recvPrepayPrice, int recvusePrice) {
        try {
            poscoChargerInfo.nomemAuthResultFlag = authResult;
            poscoChargerInfo.recvfromserver_prepayauthnum = recvPrepayApprovalNum.substring(0, 8);
            poscoChargerInfo.recvfromserver_prepayDatetime = recvPrepayDatetime;
            poscoChargerInfo.recvfromserver_prepayPrice = recvPrepayPrice;
            poscoChargerInfo.recvfromserver_usePayReal = recvusePrice;
        } catch (Exception ex) {
            LogWrapper.d(TAG, ", onRecvMissingPaymentCompleteResp: " + ex.toString());
        }
    }


    /**
     * 서버에서 충전 종료 메시지를 받음
     * @param cost 계산된 실제 금액
     */
    public void onRecvFinishChargingResp(int cost) {
        //1f응답에 담긴 충전요금 저장
        poscoChargerInfo.curChargingCost = cost;
    }

    /**
     * add by ksi. 20200529
     * 1r로 응답된 버전이 충전기 버전과 다르면 서버에 update요청 진행
     * @param version
     */
    @Override
    public void onRecvVersionResp(String version) {
        String serverVersion = version.replaceAll("\\p{Z}", "");
        String chargerVersion = poscoChargerInfo.chg_sw_version.replaceAll("\\p{Z}", "");

        //서버에서 받은 버전과 충전기 버전이 다르다면 업데이트 요청(m1)
        if (!chargerVersion.equals(serverVersion)) poscoCommManager.sendFirmwareDownReq();

    }

    /**
     * 업데이트 이미지 전송 완료시에 처리
     * @param fwDownData 펌웨어 데이터
     */
    @Override
    public void onFirmwareDownCompleted(byte[] fwDownData) {

        String updatePath = Environment.getExternalStorageDirectory()+TypeDefine.UPDATE_PATH;

        //기존 업데이트 파일 삭제
        try {
            File file = new File(updatePath,"update.zip");
            file.delete();
        }catch (Exception e) {}

        try {
            File file = new File(updatePath,"update.apk");
            file.delete();
        }catch (Exception e) {}

        FileUtil.bufferToFile(updatePath, "update.zip", fwDownData, false);

        //unzip
        try {
            ZipUtils.unzip(updatePath+"/update.zip", updatePath, false);
            // 성공시 처리
            LogWrapper.v(TAG, "Firmware Unzip Successed");

            // Update 완료 Flag 시작(충전시가 아닐때 업데이트시작)
            isFWUpdateReady = true;
        } catch(Exception e) {
            LogWrapper.v(TAG, "Firmware Unzip Failed");
        }
    }

    /**
     *
     * @param dest1 0x01: 충전기
     * @param dest2 0x01:프로그램다운로드, 0x05:회원정보 전체, 0x06:회원정보 추가, 0x07: 회원정보 삭제
     * @param url http download를 위한 파일명 포함된 원격 URL
     */
    @Override
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url) {
        if(!url.equals("")) {
            //http download start
            poscoChargerInfo.destFlag1 = dest1;
            poscoChargerInfo.destFlag2 = dest2;
            poscoChargerInfo.remoteURL = url;

            startHttpDownload();
            return true;
        }
        else return false;
    }


    /**
     * 서버에서 리셋 요청이 들어왔을 때 처리
     * @param kind 0x01 : 충전기. 0x02: M2M, 0x03: 카드단말기, 0x04: 전체
     */
    @Override
    public void onResetRequest(int kind) {
        // 충전기 리셋
        if ( kind == 0x01 || kind == 0x04 ) {
            resetRequest(true);
        }
    }

    /**
     * 원격 시작 및 종료
     * @param cmd 0x01: 완속 CType | 급속 AC3상, 0x02: 완속 Btype | 급속 DC차데모, 0x03: 종료, 0x04: 급속 DC콤보, 0x05: 즉시종료
     * @return 성공여부
     */
    @Override
    public boolean onRemoteStartStop(int cmd) {
        boolean ret = false;

        if(cmd == 0x05) {
            ret = true;     //예약취소일경우, 충전중,대기중 상관없이 무조건 1Q true 리턴
        }
        else {
            if(cpConfig.useKakaoNavi){
                if(getUIFlowState() == UIFlowState.UI_QRCERT_WAIT || getUIFlowState() == UIFlowState.UI_SELECT_AUTH){
                    if (cmd == 0x01 || cmd == 0x02) {
                        remote_lastCardnum = poscoChargerInfo.remote_cardNum;
                        poscoChargerInfo.payMethod = PoscoChargerInfo.PAY_METHOD_BY_SERVER;
                        chargeData.authType = TypeDefine.AuthType.MEMBER;
                        poscoChargerInfo.authType = PoscoChargerInfo.CommAuthType.MEMBER;
                        poscoChargerInfo.paymentCompany = 0x00;
                        poscoChargerInfo.payMethod = 0x00;
                        poscoChargerInfo.isRemoteCharge = true;     //add by si. 211006 : 카드태깅,원격 UID가 혼용되어 올라가는 부분 수정
//                    setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                        if(cmd == 0x01) {
                            chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                            dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                        }
                        else if(cmd == 0x02){
                            chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                            dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                        }
                        doAuthComplete();
                        LogWrapper.v(TAG, "Remote Charging Request Event:" + poscoChargerInfo.remote_cardNum);

                        ret = true;
                    } else ret = false;
                }
                else {
                    if (cmd == 0x03) {
                        if (remote_lastCardnum.equals(poscoChargerInfo.remote_cardNum)) {
                            LogWrapper.v(TAG, "Stop by Remote Signal");
                            if(getUIFlowState() == UIFlowState.UI_CHARGING) onChargingStop();
                            else onPageStartEvent();
                            dispMeteringString(new String[]{"Stoping...", "Wait a second"});
                            ret = true;
                        } else {
                            LogWrapper.v(TAG, "Stop by Remote Signal : Card Not Match");
                            dispTempMeteringString(new String[]{"Card Not Match"}, 4000);
                            ret = false;
                        }
                    } else ret = false;
                }
            }
            else{
                if ((meterConfig.lcdType.equals("None") == false && getUIFlowState() == UIFlowState.UI_CARD_TAG)
                        || (meterConfig.lcdType.equals("None") && getUIFlowState() == UIFlowState.UI_READY))        //카드태깅화면(대기중)일때만 원격신호 받고 원격충전 진행하도록
                {
                    if (cmd == 0x01 || cmd == 0x02) {
                        remote_lastCardnum = poscoChargerInfo.remote_cardNum;
                        poscoChargerInfo.payMethod = PoscoChargerInfo.PAY_METHOD_BY_SERVER;
                        chargeData.authType = TypeDefine.AuthType.MEMBER;
                        poscoChargerInfo.authType = PoscoChargerInfo.CommAuthType.MEMBER;
                        poscoChargerInfo.paymentCompany = 0x00;
                        poscoChargerInfo.payMethod = 0x00;
                        poscoChargerInfo.isRemoteCharge = true;     //add by si. 211006 : 카드태깅,원격 UID가 혼용되어 올라가는 부분 수정
//                    setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                        if(cmd == 0x01) {
                            chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                            dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                        }
                        else if(cmd == 0x02){
                            chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                            dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                        }
                        doAuthComplete();
                        LogWrapper.v(TAG, "Remote Charging Request Event:" + poscoChargerInfo.remote_cardNum);

                        ret = true;
                    } else ret = false;
                } else {
                    if (cmd == 0x03) {
                        if (remote_lastCardnum.equals(poscoChargerInfo.remote_cardNum)) {
                            LogWrapper.v(TAG, "Stop by Remote Signal");
                            if(getUIFlowState() == UIFlowState.UI_CHARGING) onChargingStop();
                            else onPageStartEvent();
                            dispMeteringString(new String[]{"Stoping...", "Wait a second"});
                            ret = true;
                        } else {
                            LogWrapper.v(TAG, "Stop by Remote Signal : Card Not Match");
                            dispTempMeteringString(new String[]{"Card Not Match"}, 4000);
                            ret = false;
                        }
                    } else ret = false;
                }
            }

        }
        return ret;
    }

    /**
     * add by si. 20200605 - 차지비 충전기 모드변경 메시지 수신
     * @param mode : 17(점검중), 2(운영중지), 5(정상)
     */
    public void onChangeChargerMode(int mode){
        poscoChargerInfo.cpMode = mode;
        if(poscoChargerInfo.cpMode!=poscoChargerInfo.pre_cpMode){
            if(poscoChargerInfo.cpMode == 17 || poscoChargerInfo.cpMode == 2) {
                //점검중, 혹은 운영중지를 수신받은 시점이 충전중이라면 충전 종료후 모드전환
                if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                    onChargingStop();
                }
                if (!meterConfig.lcdType.equals("None"))
                    dispMeteringString(new String[]{"Service Stopped.", "Charging Not", "Available.."});
                else {
                    //운영중지, 점검중 창 띄우기
                    pageManger.showUnavailableConView();
                }

                //UI 상태 운영중지로 변환
                if (!cpConfig.useTL3500BS) {
                    if (cpConfig.useSehanRF)
                        rfidReader.rfidReadRelease();   //rf리더기 오토스캔 잠금 전환
                }
                setUIFlowState(UIFlowState.UI_SERVICE_STOP);
                LogWrapper.v(TAG, "ModeChanged : Service Stopped");
            }
            else if(poscoChargerInfo.cpMode == 5){
                if(meterConfig.lcdType.equals("None")) pageManger.hideUnavailableConView();
                else onCheckHideFault();
                onPageStartEvent();
                LogWrapper.v(TAG, "ModeChanged : Normal");
            }
            poscoChargerInfo.pre_cpMode = poscoChargerInfo.cpMode;
        }
    }


    @Override
    public void onSystemTimeUpdate(Date syncTime) {
        Date curTime = new Date();
        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);

        // 현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTime() - syncTime.getTime()) > TypeDefine.TIME_SYNC_GAP_MS) {
            am.setTime(syncTime.getTime());
            LogWrapper.v(TAG, "TimeSync : " + syncTime.toString());
        }
    }

    public void onCommConnected() {
        mainActivity.setCommConnStatus(true);

        if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
        else dispMeteringString(new String[]{"Welcome! ChargEV", "Tag Card for EV", "ID:"+cpConfig.stationID+cpConfig.chargerID, "Ver:" + TypeDefine.SW_VERSION});
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
        LogWrapper.v(TAG, "Server Connected!!");

    }

    public void onCommDisconnected() {
        mainActivity.setCommConnStatus(false);
        if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
        else dispMeteringString(new String[] {"Welcome! ChargEV", "Disconnected SVR", "Connecting...", "ID:"+cpConfig.stationID+cpConfig.chargerID});
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, false);     //서버통신상태
        LogWrapper.v(TAG, "Server Disconnected!!");
    }

    @Override
    public void onAuthResultEvent(boolean isSuccess, int reason, int costUnit) {
        onAuthResultEvent(isSuccess, reason);
    }

    //================================================
    // RFI+D 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if(!isMeterCommErr){
            if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
                LogWrapper.v(TAG, "Card Tag Event:"+rfid);

                onCardTagEvent(rfid, true);
            }
        }
        else{
            LogWrapper.v(TAG, "RFID : Meter Read Error."+rfid);
        }

    }

    //================================================
    // 디버깅 이벤트 수신
    //================================================
    /**
     * LogWrapper Debug Msg receive
     * @param packet
     */
    @Override
    public void onRecvDebugMsg(LogWrapperMsg packet) {
        if ( pageManger != null ) pageManger.getJoasDebugMsgView().addPacket(packet);
    }
    //================================================
    // 초기화(팩토리리셋)
    //================================================
    public void doFactoryReset() {
        // IP 세팅
        NetUtil.configurationStaticIP("192.168.0.230", "255.255.255.0", "192.168.0.1", "8.8.8.8");

        // BaseDirectory 아래 데이터 모두 삭제.
        String deleteCmd = "rm -rf " + Environment.getExternalStorageDirectory().toString()+TypeDefine.REPOSITORY_BASE_PATH+"/";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException e) { }
        resetRequest(true);
    }
}
