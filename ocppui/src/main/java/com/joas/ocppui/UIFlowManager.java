/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 3:50
 *
 */

package com.joas.ocppui;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderACM1281S;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.metercertviewer.IMeterAidlInterface;
import com.joas.ocpp.chargepoint.OCPPSession;
import com.joas.ocpp.chargepoint.OCPPSessionManager;
import com.joas.ocpp.chargepoint.OCPPSessionManagerListener;
import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.FirmwareStatusNotification;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.MeterValue;
import com.joas.ocpp.msg.MeterValues;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.SampledValue;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.ocpp.msg.TriggerMessage;
import com.joas.ocpp.stack.OCPPConfiguration;
import com.joas.ocpp.stack.OCPPDiagnosticManager;
import com.joas.ocpp.stack.OCPPMessage;
import com.joas.ocpp.stack.OCPPStackProperty;
import com.joas.ocpp.stack.OCPPTransportMonitorListener;
import com.joas.ocpp.stack.Transceiver;
import com.joas.ocppui.page.PageEvent;
import com.joas.ocppui.page.PageID;
import com.joas.utils.LogWrapper;
import com.joas.utils.LogWrapperListener;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;


public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, OCPPSessionManagerListener,
        UpdateManagerListener, OCPPTransportMonitorListener, LogWrapperListener {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_SELECT,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_SERVICE_STOP,
    }

    PageManger pageManger;

    OCPPUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_SELECT;
    ChargeData chargeData;
    CPConfig cpConfig;
    MeterConfig mconfig;

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

    boolean isDspFault = false;
    boolean isPreDspFault = false;
    boolean isEmergencyPressed = false;

    long lastMeterValue = -1;
    long lastClockedMeterValue = -1;

    TimeoutTimer timerSec = null;
    String lastCardNum = "";
    String lastParentIdTag = "";
    int lastDateValue = 0;

    int meterTimerCnt = 0;

    boolean isRemoteStarted = false;
    boolean isHardResetEvent = false;
    boolean isSoftResetEvent = false;
    boolean isConnectorOperative = true;
    boolean restartBySoftReset = false;
    boolean isStopByCard = false;
    boolean isStopByStartTransactionInvalid = false;
    boolean isStopbySuspendedEVSE = false;

    OCPPSessionManager ocppSessionManager;
    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    //RFID Reader
    RfidReader rfidReader;

    StopTransaction.Reason stopReason = StopTransaction.Reason.LOCAL;
    UpdateManager updateManager;
    int firmwareInstallCounter = 0;
    double powerLimit = -1;

    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();

    int unplugTimerCnt = 0;
    int dspVersion = 0;
    int finishWaitCnt = 0;

    String[] lastMeterStringList = null;
    boolean dispMeteringStringTempLock = false;
    int dispChargingMeterStrCnt = 0;
    int dispChargingMeterBacklightCnt = 0;
    boolean isBacklightOn = false;

    public UIFlowManager(OCPPUIActivity activity, ChargeData data, CPConfig config, MeterConfig mconfig, String restartReason) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;
        this.mconfig = mconfig;

        LogWrapper.setLogWrapperListener(this);

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        if ( cpConfig.isFastCharger == true ) {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_V28_SIZE, 10,  this);
//            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, 10,  this);       //test
            dspControl.setMeterType(DSPControl2.METER_TYPE_FAST);
        }
        else {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE,1, this);
            dspControl.setMeterType(DSPControl2.METER_TYPE_SLOW);
        }
        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);
        dspControl.start();

        rfidReader = new RfidReaderACM1281S("/dev/ttyS3", RfidReaderACM1281S.RFID_CMD.RFID_TMONEY);
//        rfidReader = new RfidReaderACM1281S("/dev/ttyS3", RfidReaderACM1281S.RFID_CMD.RFID_MYFARE);

//        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
//        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
        rfidReader.setRfidReaderEvent(this);
        rfidReader.rfidReadRequest();

        if (mconfig.lcdType.equals("None")) {
            flowState = UIFlowState.UI_SELECT;
        } else {
            flowState = UIFlowState.UI_CARD_TAG;
        }

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        // UpdateManager를 생성한다.
        updateManager = new UpdateManager(mainActivity, this, OCPPUIActivity.uiVersion);

        chargeData.serverStat = true;

        initOCPPSession();

        // 1초 타이머 시작
        startPeroidTimerSec();
        initStartState();

        dispMeteringString(new String[]{"Welcome! EV", "Disconnected", "Connecting..."});
    }

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();

        rfidReader.stopThread();
        ocppSessionManager.closeManager();
        updateManager.closeManager();
    }

    /**
     * 펌웨어 업데이트 상태
     */


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

        newOcppProperty.serverUri = cpConfig.serverURI;
        newOcppProperty.cpid = cpConfig.chargerID;
        newOcppProperty.useBasicAuth = cpConfig.useHttpBasicAuth;
        newOcppProperty.authID = cpConfig.httpBasicAuthID;
        newOcppProperty.authPassword = cpConfig.httpBasicAuthPassword;

        newOcppProperty.chargePointSerialNumber = cpConfig.chargerID;
        newOcppProperty.firmwareVersion = TypeDefine.SW_VER + "(" + TypeDefine.SW_RELEASE_DATE + ")";

        return newOcppProperty;
    }

    public OCPPSessionManager getOcppSessionManager() { return ocppSessionManager; }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }

    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }
    public int getDspVersion() { return dspVersion; }

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

    public int getOcppConfigConnectorTimeout() {
        return ocppSessionManager.getOcppConfiguration().ConnectionTimeOut;
    }

    public UIFlowState getUIFlowState() { return flowState; }

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

    public void onAdminPasswordOK(String pwd) {
        if ( pwd.equals(cpConfig.settingPassword) == true ) {
            pageManger.hideAdminPasswrodInputView();
            pageManger.showSettingView();
        }
        else {
            Toast.makeText(mainActivity , mainActivity.getResources().getString(R.string.string_password_incorrect), Toast.LENGTH_SHORT).show();
        }
    }

    //region 원격 업데이트 (Firmware Update) 처리
    /**
     * 업데이트가 진행될때 이벤트 발생(UpdateManager로부터 발생되는 이벤트)
     * @param state
     */
    public void onUpdateStatus(UpdateManager.UpdateState state) {

        switch ( state ) {
            case None:
            case Waiting:
//                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.IDLE);
                break;
            case Started:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADING);
                break;
            case Error:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOAD_FAILED);
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                break;
            case Retrying:
                break;
            case Finished:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADED);
                updateManager.setState(UpdateManager.UpdateState.Installing);
                break;
            case Installing:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLING);
                updateManager.setState(UpdateManager.UpdateState.Installed);
                break;
            case Installed:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLED);
                break;
            case InstallFailed:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLATION_FAILED);
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
                break;


        }
    }

    /**
     * 1초에 한번식 수행 만약 파일 다운로드 상태라고 하면 충전중이 아닐때 수행됨
     */
    void firmwareInstallProcess() {
        if ( updateManager.getStatus() == UpdateManager.UpdateState.Installed) {
            if ( flowState != UIFlowState.UI_CHARGING) {
                firmwareInstallCounter++;
                if ( firmwareInstallCounter == TypeDefine.FIRMWARE_UPDATE_COUNTER  ) {
//                    ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLING);
                    updateManager.doInstallFirmware(OCPPUIActivity.uiVersion);
                }
            }
        }
    }
    //endregion

    /**
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch ( state ) {
            case UI_SELECT:
                initStartState();
                break;
            case UI_CARD_TAG:
                if(!mconfig.lcdType.equals("None")) initStartState();
                break;
            case UI_CONNECTOR_WAIT:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                break;

            case UI_RUN_CHECK:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                break;

            case UI_FINISH_CHARGING:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);

                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                unplugTimerCnt = 0;
                break;
        }
    }

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
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
        finishWaitCnt = 0;

//        if(mconfig.lcdType.equals("None")) rfidReader.rfidReadRelease();
//        else rfidReader.rfidReadRequest();

    }

    public void onPageStartEvent() {
        if(mconfig.lcdType.equals("None")){
            setUIFlowState(UIFlowState.UI_SELECT);
            if ( cpConfig.isFastCharger ) {
                pageManger.changePage(PageID.SELECT_FAST);
            }
            else {
                pageManger.changePage(PageID.SELECT_SLOW);
            }
        }
        else{
//            rfidReader.rfidReadRequest();

            dispMeteringString(new String[] {"Welcome! EV", "Tag Card for EV", "Ver:" + TypeDefine.SW_VER});
            //tag card
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManger.changePage(PageID.CARD_TAG);
        }


        //fault status check
        if ( chargeData.isdspCommError || chargeData.ismeterCommError ) {
            if ( chargeData.ocppStatus != StatusNotification.Status.FAULTED ) {
                // To.Do.. Error Code..Set
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FAULTED);
            }
            else if ( (chargeData.ocppStatus == StatusNotification.Status.FAULTED)
                    && (chargeData.isdspCommError == false && chargeData.ismeterCommError == false)) {
                chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
                if(isConnectorOperative) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
            }
        }
        else{
            if(isDspFault == false){
                if(isConnectorOperative) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
//                ocppSessionManager.closeSession(chargeData.curConnectorId);
            }
        }
        ocppSessionManager.closeSession(chargeData.curConnectorId);

//        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
//        ocppSessionManager.closeSession(chargeData.curConnectorId);

        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;
        isStopByCard = false;
        isStopByStartTransactionInvalid = false;
        isStopbySuspendedEVSE = false;
        Transceiver transceiver = ocppSessionManager.getOcppStack().getTransceiver();
        transceiver.setTransactionMonitorStat(false);
    }

    /**
     * Select화면에서 선택되었을때 이벤트 발생
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

        if(cpConfig.isAuthSkip)
        {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, true);     //무료모드

            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            doAuthComplete();
        }
        else{
            if ( isRemoteStarted ) {
                // 원격 시작이면 이미 session은 시작되었음. startSession을 따로 하지 않는다.
                // 바로 커넥터 연결화면으로 간다.
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                // 이미 Connect 된 상태이라면
                if ( isDspPlug ) {
                    onConnectedCableEvent(true);
                } else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManger.changePage(PageID.CONNECTOR_WAIT);
                }
            }
            else {
                // OCPP Session Start
                ocppSessionManager.startSession(chargeData.curConnectorId);

                // Next Flow. Card Tag
                setUIFlowState(UIFlowState.UI_CARD_TAG);
                pageManger.changePage(PageID.CARD_TAG);

//            rfidReader.rfidReadRequest();
            }
        }


//        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
    }


    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                onPageStartEvent();
                break;
        }
    }

    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            if (flowState == UIFlowState.UI_CARD_TAG) {


                //캐릭터LCD일 경우 초기화면이 태깅 대기상태이므로 태깅이벤트 수신시 startSession및 status Preparing 전송
                if (!mconfig.lcdType.equals("None")) {
                    dispMeteringString(new String[]{"Card Verifing...", "Wait a second"});
                    ocppSessionManager.startSession(chargeData.curConnectorId);
//                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
                }

                /**
                 * 예약중이라면 예약된 사용자가 아니면 인증 하지 않음
                 */
                if (reserveInfo.reservationId > 0) {
                    boolean reserveAuth = false;
                    if (tagNum.compareTo(reserveInfo.idTag) == 0) {
                        reserveAuth = true;
                    } else if (reserveInfo.parentIdTag != null) {
                        if (tagNum.compareTo(reserveInfo.parentIdTag) == 0) reserveAuth = true;
                    }

                    if (reserveAuth == false) {
                        chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content_reserved);
                        pageManger.showMessageBox();

                        dispTempMeteringString(new String[]{"Auth Failed", "No reserved card", "Chk reserve info"}, 6000);
                        goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                    } else {
                        reserveInfo.init();
                        mainActivity.setReservedVisible(View.INVISIBLE);
                        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                        onAuthResultEvent(true, 100);
                    }
                } else {
                    lastCardNum = tagNum;
                    // 승인대기 화면 전환
                    setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                    pageManger.changePage(PageID.AUTH_WAIT);

                    // Tag Data 전송
                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);

                }
            } else if (flowState == UIFlowState.UI_CHARGING) {
                if(chargeData.serverStat){
                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);
                }
                else{
                    if ( tagNum.equals(lastCardNum) || chargeData.startransaction_parentID.equals(chargeData.pidNum) ) {
                        if(chargeData.startransaction_parentID.equals(chargeData.pidNum)){
                            OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
                            session.setAuthTag(tagNum);
                        }
                        LogWrapper.v(TAG, "Stop by User Card Tag");
                        isStopByCard = true;
                        isRemoteStarted = false;
                        isStopByStartTransactionInvalid = false;
                        isStopbySuspendedEVSE = false;
                        onChargingStop();
                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
                    }
                    else {

                        chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                        pageManger.showMessageBox();


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

    /**
     * 로컬 인증과 같이 바로 응답이 오는경우 Delay를 해야함.(UI변경 타임)
     * @param isSuccess
     * @param delayMs
     */
    public void onAuthResultEvent(final boolean isSuccess, final int delayMs) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onAuthResultEvent(isSuccess);
                    }
                }, delayMs);
            }
        });
    }

    public void onAuthResultEvent(boolean isSuccess) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
            if (isSuccess) {
                //인증요청된 카드의 parentTag정보 저장
                OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
                chargeData.pidNum = session.getParentTag();

                dispMeteringString(new String[]{"Card Check Done.", "Connect Cable"});
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);

//                // 일반적인 충전 시퀀스에 따라 인증후 Charging상태를 보냄
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);

                doAuthComplete();

            } else {

                dispTempMeteringString(new String[]{"Auth Failed", "Check Card info"}, 4000);

                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManger.getAuthWaitView().stopTimer();
                pageManger.showMessageBox();

                goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
            }
        }
        else if ( flowState == UIFlowState.UI_CHARGING ) {
            if(isSuccess){
                stopReason = StopTransaction.Reason.LOCAL;
                onChargingStop();

//                // 일반적인 충전 시퀀스에 따라 인증후 Finish 상태를 보냄
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);

                dispMeteringString(new String[] {"Stoping...", "Wait a second"});
            }
            else {
                // To Do.
                // 메시지 박스!!!
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManger.showMessageBox();

                dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
            }

        }

    }


    void doAuthComplete() {
        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
            // 이미 Connect 된 상태이라면
            onConnectedCableEvent(true);
        } else {
            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
            pageManger.changePage(PageID.CONNECTOR_WAIT);
            // Ctype인 경우에는 도어를 오픈할 필요가 없음
            if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
            }
        }
    }

    /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        meterTimerCnt = 0;

        stopReason = StopTransaction.Reason.LOCAL;

        //캐릭터LCD관련
        // 바로 Char 디스플레이 카운트 초기화
        dispChargingMeterStrCnt = TypeDefine.DISP_CHARGING_CHARLCD_PERIOD;
        dispChargingMeterBacklightCnt = 0; // 백라이트 카운터 초기화
    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

//            // edit by si. 220221 : 커넥터 연결후 RUN신호 받고 충전시작전 charging상태로 변환
//            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);

            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            // 통신으로 충전시작 메시지를 보낸다.
            ocppSessionManager.startCharging(chargeData.curConnectorId, (int)lastMeterValue);

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);

            // RFID (충전 중지)
//            rfidReader.rfidReadRequest();

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
            sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_BEGIN, true);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        dispMeteringString(new String[] {"Connecting to EV", "Wait a second"});

        if ( isConnected ) {
//            dispMeteringString(new String[] {"Connecting to EV", "Wait a second"});
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
        }
        else {

        }
    }

    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManger.changePage(PageID.FINISH_CHARGING);

            Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();
            //transaciton monitor stat true
            trans.setTransactionMonitorStat(false);
            //충전중 모니터링시 생성된 트랜젝션 패킷 db에서 제거
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());


            // 커넥터 상태를 충전 종료중으로 바꾼다.(Status 메시지 보냄)
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
            sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_END, true);

            //통신으로 종료 패킷을 보낸다.

            if(isHardResetEvent) stopReason = StopTransaction.Reason.HARD_RESET;
            else if(isSoftResetEvent) stopReason = StopTransaction.Reason.SOFT_RESET;
            else if(isStopbySuspendedEVSE) stopReason = StopTransaction.Reason.EV_DISCONNECTED;
            else if(isRemoteStarted) stopReason = StopTransaction.Reason.REMOTE;
            else if(isStopByCard) stopReason = StopTransaction.Reason.LOCAL;
            else if(isStopByStartTransactionInvalid) stopReason = StopTransaction.Reason.DE_AUTHORIZED;

            ocppSessionManager.stopCharging(chargeData.curConnectorId, (int)lastMeterValue, stopReason);

            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

            dispMeteringString(new String[]{"Finished.", "Unplug Cable",
                    String.format("Usage:%.2f kWh", (double)(chargeData.measureWh / 1000.0))});
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    /**
     * 충전중 전원이 강제로 꺼졌을 경우에 대비해 Stoptransaction 및 Metervalue END 패킷을  db에 저장
     * @param rxData
     */

    public void onStopchargingMonitoring(DSPRxData2 rxData) {
        Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();

        //MeterValue
        OCPPMessage metervalueMsg = makeMetervalueFinishMsg();

        //StopTransaction
        OCPPMessage stoptransMsg = makeStopTransactionMsg();

        if(trans.getTransactionMonitorStat() == false){
            stopReason = StopTransaction.Reason.POWER_LOSS;

            trans.setTransactionMonitorStat(true);
            trans.setLastUniqeId_Metervalue(metervalueMsg.getId());
            trans.setLastUniqeId_StopTransaction(stoptransMsg.getId());

            trans.saveTransactionMessage(metervalueMsg);
            trans.saveTransactionMessage(stoptransMsg);
        }
        else{
            //먼저 저장된 트랜젝션 패킷 지우고
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());

            //새로운 트랜잭션 패킷 저장
            trans.setLastUniqeId_Metervalue(metervalueMsg.getId());
            trans.saveTransactionMessage(metervalueMsg);

            trans.setLastUniqeId_StopTransaction(stoptransMsg.getId());
            trans.saveTransactionMessage(stoptransMsg);
        }
    }

    public OCPPMessage makeMetervalueFinishMsg(){
        List<MeterValue> listValue = new ArrayList<MeterValue>();

        MeterValue meterValue = new MeterValue();
        meterValue.setTimestamp(Calendar.getInstance());

        List<SampledValue> listSample = new ArrayList<SampledValue>();
        SampledValue sampledValue = new SampledValue();
        sampledValue.setValue(""+lastMeterValue);
        sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_REGISTER);
        sampledValue.setUnit(SampledValue.Unit.W);
        sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
        listSample.add(sampledValue);

//        if ( meterValInterval >= 0 && measurand.contains("Energy.Active.Import.Interval") ) {
//            sampledValue = new SampledValue();
//            sampledValue.setValue("" + meterValInterval);
//            sampledValue.setMeasurand(SampledValue.Measurand.ENERGY_ACTIVE_IMPORT_INTERVAL);
//            sampledValue.setUnit(SampledValue.Unit.W);
//            setContextSampleValue(sampledValue, context);
//            listSample.add(sampledValue);
//        }

//        if ( soc >= 0 && measurand.contains("SoC")) {
//            sampledValue = new SampledValue();
//            sampledValue.setValue("" + soc);
//            sampledValue.setMeasurand(SampledValue.Measurand.SO_C);
//            sampledValue.setUnit(SampledValue.Unit.PERCENT);
//            setContextSampleValue(sampledValue, context);
//            listSample.add(sampledValue);
//        }

        //(int)chargeData.outputCurr, (int)(chargeData.outputCurr*chargeData.outputVoltage

        String measurand = ocppSessionManager.getOcppConfiguration().MeterValuesSampledData;
        int current = (int)chargeData.outputCurr;

        if ( current >= 0 && measurand.contains("Current.Import") ) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + current);
            sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_IMPORT);
            sampledValue.setUnit(SampledValue.Unit.A);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        int curPower = (int)(chargeData.outputCurr*chargeData.outputVoltage);
        if ( curPower >= 0 && measurand.contains("Power.Active.Export")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + curPower);
            sampledValue.setMeasurand(SampledValue.Measurand.POWER_ACTIVE_EXPORT);
            sampledValue.setUnit(SampledValue.Unit.W);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        int currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
        int powerOffered = TypeDefine.CP_AC_POWER_OFFERED;

        if ( currentOffered >= 0 && measurand.contains("Current.Offered")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + currentOffered);
            sampledValue.setMeasurand(SampledValue.Measurand.CURRENT_OFFERED);
            sampledValue.setUnit(SampledValue.Unit.A);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        if ( powerOffered >= 0 && measurand.contains("Power.Offered")) {
            sampledValue = new SampledValue();
            sampledValue.setValue("" + powerOffered);
            sampledValue.setMeasurand(SampledValue.Measurand.POWER_OFFERED);
            sampledValue.setUnit(SampledValue.Unit.W);
            sampledValue.setContext(SampledValue.Context.TRANSACTION_END);
            listSample.add(sampledValue);
        }

        meterValue.setSampledValue(listSample);
        listValue.add(meterValue);

        MeterValues meterValues = new MeterValues();
        meterValues.setConnectorId(chargeData.curConnectorId);
        meterValues.setMeterValue(listValue);

        OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
        boolean isInTransaction = true;
        if ( isInTransaction ) meterValues.setTransactionId(session.getCurTransactionId());

        OCPPMessage message = new OCPPMessage("MeterValues", meterValues);

        // 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
        message.transactionConnectorId = chargeData.curConnectorId;

        Calendar startTime = session.getChargingStartTime();

        if(startTime!=null) message.setTransactionStartTime(startTime);

        return message;
    }

    public OCPPMessage makeStopTransactionMsg(){
        StopTransaction stopTransaction = new StopTransaction();
        stopTransaction.setIdTag(lastCardNum);
        stopTransaction.setMeterStop((int) lastMeterValue);
        stopTransaction.setTimestamp(Calendar.getInstance());
        stopTransaction.setReason(stopReason);
        OCPPSession session = ocppSessionManager.getOcppSesstion(chargeData.curConnectorId);
        stopTransaction.setTransactionId(session.getCurTransactionId());

        OCPPMessage message = new OCPPMessage("StopTransaction", stopTransaction);

        // 추후에 메시지 처리(재전송, ACK 처리등)을 위해 Connectorid를 저장한다.
        message.transactionConnectorId = chargeData.curConnectorId;
        message.setTransactionStartTime(session.getChargingStartTime());

        return message;
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
        pageManger.getChargingView().onChargingStop();
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

    public void powerLimitProcess() {
        double newPowerLimit = ocppSessionManager.getSmartChargerManager().getCurPowerLimit(chargeData.curConnectorId);

        if ( newPowerLimit > 0 && powerLimit != newPowerLimit ) {
            powerLimit = newPowerLimit;
            //TODO 전력 제한 관련 DSP 제어 통신
        }
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {
        //WatchDog 수행
        watchDogTimerProcess();

        //FirmwareInstall 체크
        firmwareInstallProcess();

        //Fault 함수 수행
        onFaultEventProcess();

        //PowerLimit 함수 수행
        powerLimitProcess();

        // 날짜 변화 체크
        checkTransitionDay();

        //운영모드 상태에 따른 statusnoti 전송 모니터링
        checkAvailability();

        //Reserve Check
        if ( reserveInfo.expiryCheck() == true ) mainActivity.setReservedVisible(View.INVISIBLE);

        //전력량계 모니터링
        getMeterValueProcess();

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        dspVersion = rxData.version; // DSP 버전 정보 저장

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();

            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            /*
            if ( chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD ) == 0) {
                //sendCommChargeState();
            }
            */

            // 충전중 미터값을 주기에 따라 보낸다.
            int meterInterval = ocppSessionManager.getOcppConfiguration().MeterValueSampleInterval;
            meterTimerCnt++;
            if (meterTimerCnt >= meterInterval && meterInterval > 0) {
                sendOcppMeterValues(rxData, SampledValue.Context.SAMPLE_PERIODIC, true);
                meterTimerCnt = 0;
            }

            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;

            //캐릭터LCD표시
            dispChargingMeterStrCnt++;
            if (dispChargingMeterStrCnt > TypeDefine.DISP_CHARGING_CHARLCD_PERIOD) { // 8초주기 2초*4개 문구
                dispChargingMeterStrCnt = 0;

                dispMeteringString(new String[]{"Charging...",String.format("Usage:%.2f kWh", (double)(chargeData.measureWh / 1000.0)),
                        String.format("Elapse:%02d:%02d:%02d", chargingTimeSec / 3600, (chargingTimeSec % 3600) / 60, (chargingTimeSec % 60) )}, false);

            }

            //충전중 커넥터 제거시 종료사유 추가
            if(isDspPlug == false){
                if(chargeData.ocppStatus!=StatusNotification.Status.SUSPENDED_EVSE && finishWaitCnt == 0){
                    stopReason = StopTransaction.Reason.EV_DISCONNECTED;
                }
            }

            if(chargeData.serverStat) onStopchargingMonitoring(rxData);       //add by si. 220318
        }

        //충전중 상태이고 플러그가 false이면 , stopReason이 EV_DISCONNECT이면 statusnoti SUSPENDED_EVSE상태 전송
        if(getUIFlowState() == UIFlowState.UI_CHARGING){
            if(isDspPlug == false && stopReason == StopTransaction.Reason.EV_DISCONNECTED){
                if(finishWaitCnt == TypeDefine.SUSPENDED_EVSE_CHECK_TIMEOUT){
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);
                    isStopbySuspendedEVSE = true;
                    isStopByCard = false;
                    isStopByStartTransactionInvalid = false;


                    finishWaitCnt++;
                }
                else finishWaitCnt++;
            }
        }

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

        // ClockedAlign MeterValue 를 보낸다.
        processClockAlignMeterValue(rxData);

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
        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
//            if (unplugTimerCnt++> 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
            if (unplugTimerCnt++> 5 && isDspPlug == false) {
                onPageCommonEvent(PageEvent.GO_HOME);
            }
        }

        //회원인증을 안하고 커넥터를 먼저 연결할 시, 카드 태그 메시지를 출력한다.
        // 자동인증이 아닌 경우에만 적용
        //by Lee 20200526
        if(getUIFlowState() == UIFlowState.UI_CARD_TAG){
            if ((!cpConfig.isAuthSkip) && ( lastMeterStringList[0].equals("Connecting to EV")))
            {
                dispMeteringString(new String[] {"Welcome! EV", "Tag Card for EV", "Ver:" + TypeDefine.SW_VER});
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
                        if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                                ((getUIFlowState() != UIFlowState.UI_SELECT) || (getUIFlowState() != UIFlowState.UI_CARD_TAG)))  {
                            onPageStartEvent();
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

    void getMeterValueProcess() {
        //전력량계 값 가져오기
        // meterVal = 전력량계
        //meter volt
        if ( mainActivity.getMeterService() != null ) {
            try {
                long meterVal = mainActivity.getMeterService().readMeter(); //w 단위
                double meterVolt = mainActivity.getMeterService().readMeterVoltage();
                double meterCurrent = mainActivity.getMeterService().readMeterCurrent();

//                minsuChargerInfo.meterVal = (int)meterVal;

                //add by si - 21.12.09 - MeterReadError상태 감지 추가
                MeterStatusMonitoring(meterVal);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                if(cpConfig.isFastCharger){
                    dspControl.setOutputVoltageDC(chargeData.dspChannel,fMeterVolt);
                    dspControl.setOutputAmpareDC(chargeData.dspChannel, fMeterCurrent);
                    //load test용
                    chargeData.outputVoltage = Math.round(fMeterVolt*100)/100.0;
                    chargeData.outputCurr = Math.round(fMeterCurrent*100)/1000.0;
                }
                else{
                    dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);
                    dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);
                    //load test용
                    chargeData.outputVoltage = Math.round(fMeterVolt*100)/100.0;
                    chargeData.outputCurr = Math.round(fMeterCurrent*100)/100.0;
                }



//                LogWrapper.d(TAG, "Meter:"+chargeData.meterVal+", Volt:"+chargeData.outputVoltage+", current:"+chargeData.outputCurr);
//                LogWrapper.d(TAG, "Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if (meterVal >= 0) {
                    if(cpConfig.isFastCharger) dspControl.setMeterDC(chargeData.dspChannel, fMeterVal);
                    else dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);
                    chargeData.meterVal = meterVal;
                    if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                        if (lastMeterValue > 0) {
                            int gapMeter = (int) (meterVal - lastMeterValue);
                            if (gapMeter > 0) {
                                //gapMeter 최대 증가폭 1초당 0.5kw를 넘지못함. - add by si. 200831
                                if (gapMeter > 500) {
                                    gapMeter = 500;
                                }
                                chargeData.measureWh += gapMeter;
                                chargeData.chargingCost = ((double) chargeData.measureWh / 1000.0) * (double) chargeData.chargingUnitCost;
                                //서버로 값 전송
//                                minsuChargerInfo.curChargingKwh = (int)chargeData.measureWh;
//                                minsuChargerInfo.curChargingCost = (int)chargeData.chargingCost;
//                                minsuChargerInfo.curChargingCostUnit = chargeData.chargingUnitCost;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                } else {
                    // Meter Error !!!
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);
//                    minsuChargerInfo.meterVal = (int)lastMeterValue;
                }
            }catch(Exception e){
                LogWrapper.e(TAG, "Meter Err:"+e.toString());
            }
        }

    }

    boolean chkAvailabilityStartFlag = true;
    protected void checkAvailability(){
        if(chkAvailabilityStartFlag){
            int _connectorid = Integer.parseInt(cpConfig.opModeConnectorID);
            if(cpConfig.isAvailable){
//                setOcppStatus(_connectorid, StatusNotification.Status.AVAILABLE);
            }
            else{
//                setOcppStatus(_connectorid, StatusNotification.Status.UNAVAILABLE);

                // 사용불가 화면 첫화면에서만 동작하도록 함
                if ((mconfig.lcdType.equals("None")) && (flowState == UIFlowState.UI_SELECT) ) pageManger.showUnavailableConView();
                else if ((!mconfig.lcdType.equals("None")) && (flowState == UIFlowState.UI_CARD_TAG) ) {
                    pageManger.showUnavailableConView();
                    dispMeteringString(new String[] {"Service Stopped."});
//                    if(mconfig.lcdType.equals("None")) rfidReader.rfidReadRelease();
                    setUIFlowState(UIFlowState.UI_SERVICE_STOP);
                }
            }
            isConnectorOperative = cpConfig.isAvailable;
            chkAvailabilityStartFlag = false;
        }
    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if ( curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue ) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);

            // 날짜가 봐뀔때 과거 로그를 지운다.
            ocppSessionManager.getOcppStack().getOcppDiagnosticManager().removePastLog();
        }
    }

    /**
     * 1초마다 호출, Meter Interval 마다 Sample List들을 보냄
     */
    protected void processClockAlignMeterValue(DSPRxData2 rxData) {
        int clockAlignedDataInterval = ocppSessionManager.getOcppConfiguration().ClockAlignedDataInterval;
        if ( clockAlignedDataInterval == 0 ) return;

        // 00:00:00 기준으로 Interval 계산함
        // 현재시간 초로 나타냄
        Calendar curTime = Calendar.getInstance();
        int secTime = curTime.get(Calendar.HOUR_OF_DAY) * 3600 + curTime.get(Calendar.MINUTE) * 60 + curTime.get(Calendar.SECOND);

        if ((secTime % clockAlignedDataInterval ) == 0) {
            int gapMeter = 0;
            if ( lastClockedMeterValue > 0 ) {
                gapMeter = (int)(lastMeterValue - lastClockedMeterValue);
            }
            lastClockedMeterValue = lastMeterValue;

            // TODO 전력제한 과 연동 필요!!
            int currentOffered = -1;
            int powerOffered = -1;
            int soc = -1;

            if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                if (chargeData.connectorType == TypeDefine.ConnectorType.AC3) {
                    currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
                    powerOffered = TypeDefine.CP_AC_POWER_OFFERED;
                } else {
                    currentOffered = TypeDefine.CP_DC_CURRENT_OFFERED;
                    powerOffered = TypeDefine.CP_DC_POWER_OFFERED;
                    soc = rxData.batterySOC;
                }
            }

            ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, lastMeterValue, gapMeter, (int)chargeData.outputCurr, (int)(chargeData.outputCurr*chargeData.outputVoltage),
                    currentOffered, powerOffered, soc, SampledValue.Context.SAMPLE_CLOCK,
                    ocppSessionManager.getOcppConfiguration().MeterValuesAlignedData, getUIFlowState() == UIFlowState.UI_CHARGING);
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

    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager.restartManager(newOcppProperty);
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

//    /**
//     *  DSP에서 오는 이벤트를 처리한다.
//     * @param channel 해당 채널값
//     * @param idx 상태값 Index
//     * @param val
//     */
//    public void onDspStatusChange(int channel, DSPRxData.STATUS400 idx, boolean val) {
//        if ( channel == chargeData.dspChannel ) {
//
//            LogWrapper.v(TAG, "DSP Status Change:"+ idx.name()+" is "+val);
//
//            switch (idx) {
//                case READY:
//                    isDspReady = val;
//                    break;
//
//                case AVAL_CHARGE:
//                    isDspAvalCharge = val;
//                    break;
//
//                case STATE_PLUG:
//                    isDspPlug = val;
//                    onConnectedCableEvent(val);
//                    break;
//
//                case STATE_DOOR:
//                    isDspDoor = val;
//                    if ( isDspDoor == false ) { // 도어 오픈
//                        //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
//                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
//                    }
//                    break;
//
//                case CHARGE_RUN:
//                    isDspChargeRun = val;
//                    if ( val == true ) {
//                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
//                        onDspChargingStartEvent();
//                    }
//                    break;
//
//                case FINISH_CHARGE:
//                    isDspChargeFinish = val;
//                    if ( val == true ) onFinishChargingEvent();
//                    break;
//
//                case FAULT:
//                    isDspFault = val;
//                    onFaultEventProcess();
//                    break;
//
//                case STATE_RESET:
//                    break;
//
//                case CONNECTOR_LOCK_A:
//                    break;
//            }
//        }
//    }

    int faultCode = 0;      //알람 자료 전송용 장애코드(DSP)
    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false ) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";

                faultCode = dispDspErrorString(fInfo.errorCode);
            }
        }
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
                    if (isEmergency == true) stopReason = StopTransaction.Reason.EMERGENCY_STOP;
                    else stopReason = StopTransaction.Reason.OTHER;
                    onChargingStop();
                }
                else {
                    if(mconfig.lcdType.equals("None")){
                        if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING && getUIFlowState() != UIFlowState.UI_SELECT )  {
                            onPageStartEvent();
                        }
                    }
                    else{
                        if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING && getUIFlowState() != UIFlowState.UI_CARD_TAG )  {
                            onPageStartEvent();
                        }
                    }
                }
                fillFaultMessage();
                pageManger.showFaultBox();
            }
            else {
                pageManger.hideFaultBox();
                onCheckHideFault();
            }
            isPreDspFault = isDspFault;
        }

        if(!chargeData.isdspCommError && !chargeData.ismeterCommError){
            if ( isDspFault == true ) {
                if ( chargeData.ocppStatus != StatusNotification.Status.FAULTED ) {
                    // To.Do.. Error Code..Set
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FAULTED);
                }
            }
            else if ( chargeData.ocppStatus == StatusNotification.Status.FAULTED ) {
                chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
                if(isConnectorOperative) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
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
            usage = String.format("Usage: %.2f kWh", (double)(chargeData.measureWh / 1000.0) );
//            cost = String.format("Cost: %dWon", poscoChargerInfo.curChargingCost);
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
                dispDspErrorString423(isCharging, bit, usage, cost);
                break;
            case 424:
                break;
            case 425:
                dispDspErrorString425(isCharging, bit, usage, cost);
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
    private void dispDspErrorString423(boolean isCharging, int bit, String usage, String cost)
    {
//        int faultCode = AlarmCode.ETC_ERR;      // 알람자료 전송용 장애코드
        switch (bit) {
            case 0:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_0, usage, cost);        //비상정지
//                faultCode = AlarmCode.EMERGENCY;
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_1, usage, cost);
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_2, usage, cost);
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_3, usage, cost);
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_4, usage, cost);        //오류코드 : 9, 전자접촉기/릴레이 융착
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_5, usage, cost);        //오류코드 : 9, 전자접촉기/릴레이 융착
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_6, usage, cost);        //오류코드 : 1, 전자접촉기/릴레이 이상
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 7:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_7, usage, cost);        //오류코드 : 1, 전자접촉기/릴레이 이상
//                faultCode = AlarmCode.ERR_CODE_9;
                break;
            case 8:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_8, usage, cost);
                break;
            case 9:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_9, usage, cost);        //오류코드 : 6
//                faultCode = AlarmCode.ERR_CODE_6;
                break;
            case 10:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_10, usage, cost);
//                faultCode = AlarmCode.CHGR_INDOOR_TEMP_ERR;
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
//                faultCode = AlarmCode.ERR_CODE_12;
                break;
            case 14:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_14, usage, cost);
//                faultCode = AlarmCode.METER_COMM_ERR;
                break;
            case 15:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_15, usage, cost);       //오류코드 : 5
//                faultCode = AlarmCode.ERR_CODE_5;
                break;
        }
        LogWrapper.v(TAG, "DSP ERROR-423-" + Integer.toString(bit));

//        return faultCode;
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
    private void dispDspErrorString425(boolean isCharging, int bit, String usage, String cost)
    {
//        int faultData = AlarmCode.ETC_ERR;

        switch (bit) {
            case 0:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_0, usage, cost);        //errcode : 7
//                faultData = AlarmCode.ERR_CODE_7;
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_1, usage, cost);
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_2, usage, cost);        //errcode : 2
//                faultData = AlarmCode.ERR_CODE_2;
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_3, usage, cost);
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_4, usage, cost);        //errcode : 8
//                faultData = AlarmCode.ERR_CODE_8;
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_5, usage, cost);        //errcode : 3
//                faultData = AlarmCode.ERR_CODE_3;
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_6, usage, cost);        //errcode : 10
//                faultData = AlarmCode.ERR_CODE_10;
                break;
            case 7:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_9, usage, cost);        //errcode : 10
                break;
        }
        LogWrapper.v(TAG, "DSP ERROR-425-" + Integer.toString(bit));

//        return faultData;
    }

    @Override
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
        if ( channel == chargeData.dspChannel ) {

            LogWrapper.v(TAG, "DSP Status Change:"+ idx.name()+" is "+val);

            switch (idx) {
                case READY:
                    isDspReady = val;
                    break;

                case AVAL_CHARGE:
                    isDspAvalCharge = val;
                    break;

                case STATE_PLUG:
                    isDspPlug = val;
                    if(isDspPlug && isConnectorOperative && getUIFlowState() == UIFlowState.UI_CARD_TAG) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);      //add by si. 220316
                    else {
                        if(getUIFlowState() == UIFlowState.UI_CARD_TAG) setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);      //add by si. 220316
                    }
                    onConnectedCableEvent(val);
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

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            if ( lastMeterValue < 0) lastMeterValue = meterVal;
            int gapMeter = (int)(meterVal - lastMeterValue);
            if ( gapMeter < 0) gapMeter = 0;
            chargeData.measureWh += gapMeter;
            chargeData.chargingCost += ((double)gapMeter/1000.0)*(double)chargeData.chargingUnitCost;
        }
        lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {
        chargeData.isdspCommError = isError;

        if (isError == true) {
            if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                onChargingStop();
            }
            else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState()!=UIFlowState.UI_SELECT))  {
                onPageStartEvent();
            }
            pageManger.showDspCommErrView();
            dispMeteringString(new String[]{"INTER COMM ERR.", "ERROR CODE:11"});
            LogWrapper.e(TAG, "DSP-UI Comm Error!!");
        }
        else {
            pageManger.hideDspCommErrView();
            onCheckHideFault();
            LogWrapper.e(TAG, "DSP-UI Comm Recovery.");
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
        ocppSessionManager.SendStatusNotificationRequest(connectorId, chargeData.ocppStatus, chargeData.ocppStatusError);
    }

    /*
    public void setOcppError(int connectorId, StatusNotification.ErrorCode error) {
        StatusNotification.ErrorCode oldError = chargeData.ocppStatusError;
        if ( oldError != error ) {
            chargeData.ocppStatusError = error;
            ocppSessionManager.SendStatusNotificationRequest(connectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
        }
    }
    */

    /**
     * 전체 시스템 상태(모든 커넥터)를 보낸다.
     * 오직 Available, Unavailable and Faulted 만 보낼 수 있다.(Spec 4.9 Status Notification)
     */
    public void sendNotificationStatusOfSystem() {
        // To. Do// 현재 Fault인지 아닌지 구별하여 보내야함..
        int conid = Integer.parseInt(cpConfig.opModeConnectorID);
        if(cpConfig.isAvailable){
            if(conid == 0){
                //모든커넥터 available
                ocppSessionManager.SendStatusNotificationRequest(0, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
                ocppSessionManager.SendStatusNotificationRequest(1, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
            }
            else{
                ocppSessionManager.SendStatusNotificationRequest(conid, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
            }
//            ocppSessionManager.SendStatusNotificationRequest(0, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
//
//            // 각 Connector의 상태값을 보낸다.
//            ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
        }
        else{

            if(conid == 0){
                ocppSessionManager.SendStatusNotificationRequest(conid, StatusNotification.Status.UNAVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
                // 각 Connector의 상태값을 보낸다.
                ocppSessionManager.SendStatusNotificationRequest(conid+1, StatusNotification.Status.UNAVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
            }
            else{
                ocppSessionManager.SendStatusNotificationRequest(0, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );
                // 각 Connector의 상태값을 보낸다.
                ocppSessionManager.SendStatusNotificationRequest(conid, StatusNotification.Status.UNAVAILABLE, chargeData.ocppStatusError );
            }


        }

    }

    // 미터값, SOC 등 값을 전달한다.
    public void sendOcppMeterValues(DSPRxData2 rxData, SampledValue.Context context, boolean isInTransaction) {
        int currentOffered = TypeDefine.CP_DC_CURRENT_OFFERED;
        int powerOffered = TypeDefine.CP_DC_POWER_OFFERED;
        int soc = -1;

        if ( chargeData.connectorType == TypeDefine.ConnectorType.AC3 ) {
            currentOffered = TypeDefine.CP_AC_CURRENT_OFFERED;
            powerOffered = TypeDefine.CP_AC_POWER_OFFERED;
        }
        else {
            soc = rxData.batterySOC;
        }

        ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, lastMeterValue, -1, (int)chargeData.outputCurr, (int)(chargeData.outputCurr*chargeData.outputVoltage),
                currentOffered, powerOffered, soc, context,
                ocppSessionManager.getOcppConfiguration().MeterValuesSampledData, isInTransaction);
    }

    @Override
    public void onAuthSuccess(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT || flowState == UIFlowState.UI_CHARGING ) {
            if (connectorId == chargeData.curConnectorId) {

                onAuthResultEvent(true, 1000);
            }
        }
    }

    @Override
    public void onAuthFailed(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT || flowState == UIFlowState.UI_CHARGING) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEvent(false, 1000);
            }
        }
    }

    @Override
    public void onChangeState(int connectorId, OCPPSession.SessionState state) {
        final int cid = connectorId-1;
        final OCPPSession.SessionState chgState = state;
        /*
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStatus[cid].setText(chgState.name());
            }
        });
        */
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        CancelReservationResponse.Status ret =CancelReservationResponse.Status.REJECTED;
        if ( reserveInfo.reservationId == reservationId) {
            reserveInfo.init();
            mainActivity.setReservedVisible(View.INVISIBLE);
            ret = CancelReservationResponse.Status.ACCEPTED;
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
        }
        return ret;
    }

    @Override
    public void onBootNotificationResponse(boolean success) {
        if ( success ) {
            // 처음 접속이후에 StatusNotification을 보낸다.
            sendNotificationStatusOfSystem();

            // 펌웨어 업데이트가 되었다면 메시지를 보내고 펌웨어 업데이트 필드를 초기화한다.
            if ( updateManager.newFirmwareUpdateed ) {
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLED);
                updateManager.onUpdateCompleteMsgSent();
            }
        }
    }

    /**
     * 원격에서 충전을 중지시키는 이벤트가 발생했을때 처리
     * @param connectorId
     */
    @Override
    public void onRemoteStopTransaction(int connectorId) {
        if (chargeData.curConnectorId == connectorId) {
            if (  flowState == UIFlowState.UI_CHARGING ) {
                stopReason = StopTransaction.Reason.REMOTE;
                onChargingStop();
                LogWrapper.e(TAG, "RemoteStopTransaction Received..");
            }
        }
    }

    /**
     * 원격에서 충전을 시작시키는 이벤트가 발생했을 때 처리     *
     * @param connectorId
     * @param idTag
     * @param chargingProfile
     * @return 성공여부
     */
    @Override
    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile) {
        if(chargeData.isdspCommError || chargeData.ismeterCommError || isDspFault){
            return false;
        }
        else{
            if (  flowState == UIFlowState.UI_SELECT ||
                    flowState == UIFlowState.UI_CARD_TAG ) {


                mainActivity.setRemoteStartedVisible(View.VISIBLE);
                isRemoteStarted = true;
                isStopByCard = false;
                isStopByStartTransactionInvalid = false;
                isStopbySuspendedEVSE = false;
                lastCardNum = idTag;
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
                dispMeteringString(new String[]{"Remote Auth Ok.", "Connect Cable"});
                LogWrapper.e(TAG, "RemoteStartTransaction Received..");
                ocppSessionManager.setLastAuthConnectorID(chargeData.curConnectorId);

                if ( flowState == UIFlowState.UI_CARD_TAG ) {
                    // 카드 테깅 화면이면 인증을 넘어간다.
                    setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);

                    // 이미 Connect 된 상태이라면
                    if ( isDspPlug ) {
                        onConnectedCableEvent(true);
                    } else {
                        // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                        pageManger.changePage(PageID.CONNECTOR_WAIT);
                    }

                }
                else {
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * StartTranscation의 IdTagInfo 상태를 받았을 때처리함.
     * @param connectorId
     * @param tagInfo
     */
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo) {
        if (chargeData.curConnectorId == connectorId) {
            if (  flowState == UIFlowState.UI_CHARGING ) {
                if ( tagInfo.getStatus() == IdTagInfo.Status.ACCEPTED ) {
                    // 커넥터 상태를 충전중으로 바꾼다.(Status 메시지 보냄)
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);
                    chargeData.startransaction_parentID = tagInfo.getParentIdTag();
                }
                else {
                    // 커넥터 상태를 SUSPENDED_EVSE 로 바꾸고 충전을 중지한다.
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);

                    OCPPConfiguration ocppConfiguration = ocppSessionManager.getOcppConfiguration();
                    // 만약 StopTransactionOnInvalidId 가 true 이면 충전을 중지한다.
                    if ( ocppConfiguration.StopTransactionOnInvalidId == true) {
                        stopReason = StopTransaction.Reason.DE_AUTHORIZED;
                        isStopByStartTransactionInvalid = true;
                        isRemoteStarted = false;
                        isStopByCard = false;
                        isStopbySuspendedEVSE = false;
                        onChargingStop();
                    }
                }
            }
        }
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        ReserveNowResponse.Status ret = ReserveNowResponse.Status.REJECTED;

        /** 5.13 Reserve Now
         * If the reservationId does not match any reservation in the Charge Point, then the Charge Point SHALL
         return the status value ‘Accepted’ if it succeeds in reserving a connector. The Charge Point SHALL
         return ‘Occupied’ if the Charge Point or the specified connector are occupied. The Charge Point SHALL
         also return ‘Occupied’ when the Charge Point or connector has been reserved for the same or another
         idTag. The Charge Point SHALL return ‘Faulted’ if the Charge Point or the connector are in the Faulted
         state. The Charge Point SHALL return ‘Unavailable’ if the Charge Point or connector are in the
         Unavailable state. The Charge Point SHALL return ‘Rejected’ if it is configured not to accept
         reservations.
         */

        if ( isConnectorOperative == false ) ret = ReserveNowResponse.Status.UNAVAILABLE;
        else if ( (mconfig.lcdType.equals("None")) && (flowState != UIFlowState.UI_SELECT)) ret = ReserveNowResponse.Status.OCCUPIED;
        else if ( (!mconfig.lcdType.equals("None")) && (flowState != UIFlowState.UI_CARD_TAG)) ret = ReserveNowResponse.Status.OCCUPIED;
        else if ( isDspFault ) ret = ReserveNowResponse.Status.FAULTED;
        else if ( reserveInfo.reservationId > 0 && reserveInfo.reservationId != reservationId ) ret = ReserveNowResponse.Status.REJECTED;
        else {
            reserveInfo.setInfo(reservationId, idTag, parentIdTag, expiryDate);
            mainActivity.setReservedVisible(View.VISIBLE);
            ret = ReserveNowResponse.Status.ACCEPTED;
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.RESERVED);
        }
        return ret;
    }

    /**
     * Reset 명령을 원격에서 내려 받았을 떄 처리
     * 충전중인경우 충전을 중지한 다음에 Reset을 한다.
     * @param isHard true 인경우 system reset을 하고 false인 경우에는 충전 중지 이후에 초기화만 진행한다.
     */
    @Override
    public void onResetRequest(boolean isHard) {
        if ( isHard == true ) {
            isHardResetEvent = true;

            // 충전중이라면 충전을 중지한다.
            if (  flowState == UIFlowState.UI_CHARGING ) {
                stopReason = StopTransaction.Reason.HARD_RESET;
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

        }
        else { // Soft Reset
            // 충전중이라면 충전을 중지한다.
            isSoftResetEvent = true;
            if (flowState == UIFlowState.UI_CHARGING) {
                stopReason = StopTransaction.Reason.SOFT_RESET;
                onChargingStop();
            }

//            // 약 15초뒤에 Reset됨
//            if ( watchDogTimer.isStarted == false ) watchDogTimer.openAndStart(15);
//            else watchDogTimer.update();

            //add by si. 200624 캐릭터 LCD reboot 표시
            dispMeteringString(new String[] {"Reboot System..", "Wait a second"});

            // 메시지 박스를 띄운다.
            chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_soft_reset_title);
            chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_soft_reset_content);
            pageManger.showMessageBox();

//            runSoftReset(chargeData.messageBoxTimeout*1000*15);
            runSoftReset(1000*15);
        }
    }

    @Override
    public void onTriggerMessage(TriggerMessage message) {
        switch( message.getRequestedMessage().toString() ) {
            case "DiagnosticsStatusNotification":
                break;
            case "FirmwareStatusNotification":
                onUpdateStatus(updateManager.getStatus());
                break;
            case "MeterValues":
                DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
                sendOcppMeterValues(rxData, SampledValue.Context.TRIGGER, false);
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
        if ( connectorId == chargeData.curConnectorId || connectorId == 0) {

            if ( type == ChangeAvailability.Type.OPERATIVE ) {
                isConnectorOperative = true;
                if(connectorId == 0){
                    setOcppStatus(connectorId, StatusNotification.Status.AVAILABLE);
                    setOcppStatus(connectorId+1, StatusNotification.Status.AVAILABLE);
                }
                else setOcppStatus(connectorId, StatusNotification.Status.AVAILABLE);

                // 사용불가 화면 숨김
                pageManger.hideUnavailableConView();
                onCheckHideFault();
//                rfidReader.rfidReadRequest();
                if(mconfig.lcdType.equals("None"))setUIFlowState(UIFlowState.UI_SELECT);
                else setUIFlowState(UIFlowState.UI_CARD_TAG);
                dispMeteringString(new String[] {"Welcome! EV", "Tag Card for EV", "Ver:" + TypeDefine.SW_VER});
            }
            else {
                isConnectorOperative = false;
                if(connectorId == 0){
                    setOcppStatus(connectorId, StatusNotification.Status.UNAVAILABLE);
                    setOcppStatus(connectorId+1, StatusNotification.Status.UNAVAILABLE);
                }
                else setOcppStatus(connectorId, StatusNotification.Status.UNAVAILABLE);

                // 사용불가 화면 첫화면에서만 동작하도록 함
                if ((mconfig.lcdType.equals("None")) && (flowState == UIFlowState.UI_SELECT) ) pageManger.showUnavailableConView();
                else if ((!mconfig.lcdType.equals("None")) && (flowState == UIFlowState.UI_CARD_TAG) ) {
                    pageManger.showUnavailableConView();
                    dispMeteringString(new String[] {"Service Stopped."});
//                    if(mconfig.lcdType.equals("None")) rfidReader.rfidReadRelease();
                    setUIFlowState(UIFlowState.UI_SERVICE_STOP);
                }
            }

            //운영모드 설정값 저장
            cpConfig.isAvailable = isConnectorOperative;
            cpConfig.opModeConnectorID = ""+connectorId;
            cpConfig.saveConfig(this.mainActivity);
            chkAvailabilityStartFlag = true;
        }

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

    /**
     *  원격 업데이트 이벤트를 받을때 처리한다.
     * @param location URI 주소(FTP, HTTP)
     * @param retry 최대 재시도횟수
     * @param retrieveDate 업데이트 시도 시간(이후)
     * @param retryInterval 업데이트 시도 간격
     */
    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {
        LogWrapper.d(TAG, "Firmware Update Request: "+location.toString());

        updateManager.setUpdateInfo(location, retry, retrieveDate, retryInterval);
    }

    // 통신 연결 상태 관리
    public void onChangeOcppServerConnectStatus(boolean status) {
        // 타이밍상 Listener 생성 보다 connection이 더 빠른경우에 호출이 안되는 경우가 있음
        // bootNotification에서 연결 상태 Update 할 필요가 있음(위에 TimeSync에서 수행)
        mainActivity.setCommConnStatus(status);
    }

    // 통신 이벤트 발생 처리(UI 표시)
    public void onOcppMessageTransportEvent() {
        mainActivity.setCommConnActive();
    }

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        //dspfault가 아닐때 수행
        if(!isDspFault && !chargeData.isdspCommError && !chargeData.ismeterCommError){
            if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
                onCardTagEvent(rfid, true);
            }

//            if(mconfig.lcdType.equals("None")) rfidReader.rfidReadRelease();
        }

//        rfidReader.rfidReadRelease();
    }


    //================================================
    // 통신 이벤트 수신
    //================================================
    @Override
    public void onOCPPTransportRecvRaw(String data) {
        pageManger.getCommMonitorView().addOCPPRawMsg("RX", data);
        onOcppMessageTransportEvent();

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
        pageManger.getCommMonitorView().addOCPPRawMsg("TX", data);
        onOcppMessageTransportEvent();

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
        onChangeOcppServerConnectStatus(true);

        chargeData.serverStat = true;
        dispMeteringString(new String[] {"Welcome! EV", "Tag Card for EV", "Ver:" + TypeDefine.SW_VER});
    }

    @Override
    public void onOCPPTransportDisconnected() {
        chargeData.serverStat = false;
        Transceiver trans = ocppSessionManager.getOcppStack().getTransceiver();
        //transaciton monitor stat true
        if(trans.getTransactionMonitorStat()) {
            trans.setTransactionMonitorStat(false);
            //충전중 모니터링시 생성된 트랜젝션 패킷 db에서 제거
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Metervalue());
            trans.removeSaveTransactionMessage(trans.getLastUniqeId_Stoptransaction());
        }
        onChangeOcppServerConnectStatus(false);

        dispMeteringString(new String[] {"Welcome! EV", "Server Disconn", "Connecting...", "Ver:" + TypeDefine.SW_VER});
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

        // Add to Diagnostic Log
        if ( ocppSessionManager != null ) {
            if (ocppSessionManager.getOcppStack().getOcppDiagnosticManager() != null ) {
                OCPPDiagnosticManager.DiagnosticType type;
                switch (packet.level) {
                    case Log.ERROR: type = OCPPDiagnosticManager.DiagnosticType.Error; break;
                    case Log.DEBUG: type = OCPPDiagnosticManager.DiagnosticType.Debug; break;
                    case Log.VERBOSE: type = OCPPDiagnosticManager.DiagnosticType.Verbose; break;
                    default: type = OCPPDiagnosticManager.DiagnosticType.Debug; break;
                }
                ocppSessionManager.getOcppStack().getOcppDiagnosticManager().addLog(packet.time, type, packet.TAG, packet.msg);
            }
        }
    }

    //================================================
    // 초기화(팩토리리셋)
    //================================================
    public void doFactoryReset() {
        // IP 세팅
        NetUtil.configurationStaticIP("192.168.0.230", "255.255.255.0", "192.168.0.1", "8.8.8.8");
        // 데이터베이스 Close
        ocppSessionManager.getOcppStack().closeOcpp();
        ocppSessionManager.getOcppStack().getDbOpenHelper().close();

        // BaseDirectory 아래 데이터 모두 삭제.
        String deleteCmd = "rm -rf " + Environment.getExternalStorageDirectory().toString()+TypeDefine.REPOSITORY_BASE_PATH+"/";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(deleteCmd);
        } catch (IOException e) { }
        onResetRequest(true);
    }

    //=================================================
    //캐릭터 LCD 표시관련 함수
    //=================================================
    public void dispLastMeteringString() {
        dispMeteringString(lastMeterStringList);
    }

    public void dispMeteringString(String[] listStr) {
//        if(!mconfig.lcdType.equals("None")){
//
//        }
        dispMeteringString(listStr, true);
    }

    public void dispMeteringString(String[] listStr, boolean isBacklightOn) {
        lastMeterStringList = listStr;

        if ( dispMeteringStringTempLock ) return;

        if ( listStr == null ) return;
        try {
            IMeterAidlInterface meterService = mainActivity.getMeterService();

            if ( isBacklightOn ) {
//                if (!(meterConfig.lcdType.equals("RW1603"))) meterService.setCharLCDBacklight(true);
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
     * Fault 에서 정상으로 돌아왔을 시, 시퀀스에 맞는 캐릭터 LCD 문자열 표시
     *
     * by Lee 20200518
     */
    private void onCheckHideFault()
    {
        if (getUIFlowState() == UIFlowState.UI_CARD_TAG) {
//            if(isDspTestMode)dispMeteringString(new String[] {"Welcome! EV", "Test Mode", "Connect Cable"});
//            else dispMeteringString(new String[]{"Welcome! ChargEV", "Tag Card for EV", "ID:" + cpConfig.stationID + cpConfig.chargerID, "Ver:" + TypeDefine.SW_VERSION});
            dispMeteringString(new String[]{"Welcome! EV", "Tag Card for EV", "Ver:" + TypeDefine.SW_VER});
        }
        else if (getUIFlowState() == UIFlowState.UI_AUTH_WAIT)
            dispMeteringString(new String[] {"Card Verifing...", "Wait a second"});
        else if (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT)
            dispMeteringString(new String[] {"Card Check Done.", "Connect Cable"});
        else if (getUIFlowState() == UIFlowState.UI_RUN_CHECK)
            dispMeteringString(new String[] {"Start Charging"});
        else if (getUIFlowState() == UIFlowState.UI_CHARGING)
            dispMeteringString(new String[]{"Charging...",String.format("Usage:%.2f kWh", (double)(chargeData.measureWh / 1000.0)),
                    String.format("Elapse:%02d:%02d:%02d", ((int)(chargeData.chargingTime/1000))/ 3600, (((int)(chargeData.chargingTime/1000)) % 3600) / 60, (((int)(chargeData.chargingTime/1000)) % 60) )}, false);
        else if (getUIFlowState() == UIFlowState.UI_FINISH_CHARGING) {
            dispMeteringString(new String[]{"Finished.","Unplug Cable",String.format("Usage:%.2f", (double) (chargeData.measureWh / 1000.0))});

            LogWrapper.v(TAG, "Finish Charging:" + String.format("Usage: %.2fkWh", ((double) (chargeData.measureWh / 1000.0))));
        }

        int chargingTimeSec = (int)(chargeData.chargingTime/1000);

    }
}
