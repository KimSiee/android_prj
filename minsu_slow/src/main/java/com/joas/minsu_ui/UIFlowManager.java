/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvCommManager;
import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.minsu_comm.MinsuChargerInfo;
import com.joas.minsu_comm.MinsuCommManager;
import com.joas.minsu_comm.MinsuCommManagerListener;
import com.joas.minsu_ui.page.PageEvent;
import com.joas.minsu_ui.page.PageID;
import com.joas.utils.LogWrapper;
import com.joas.utils.LogWrapperListener;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControl2Listener,
        UpdateManagerListener, LogWrapperListener, MinsuCommManagerListener {
    public static final String TAG = "UIFlowManager";

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

    @Override
    public void onCommConnected() {
        mainActivity.setCommConnStatus(true);
//        if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
//        else dispMeteringString(new String[]{"Welcome! ChargEV", "Tag Card for EV", "ID:"+cpConfig.stationID+cpConfig.chargerID, "Ver:" + TypeDefine.SW_VERSION});
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
        LogWrapper.v(TAG, "Server Connected!!");
    }

    @Override
    public void onCommDisconnected() {
        mainActivity.setCommConnStatus(false);
//        if(isDspTestMode)dispMeteringString(new String[] {"Welcome! ChargEV", "Test Mode", "Connect Cable"});
//        else dispMeteringString(new String[] {"Welcome! ChargEV", "Disconnected SVR", "Connecting...", "ID:"+cpConfig.stationID+cpConfig.chargerID});
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, false);     //서버통신상태
        LogWrapper.v(TAG, "Server Disconnected!!");
    }

    @Override
    public void onAuthResultEventFromServer(boolean isSuccess, int reason) {
        onAuthResultEvent(isSuccess);
        LogWrapper.v(TAG, "receved 1a, result:" + isSuccess + ",reason:" + reason);
    }

    @Override
    public void onResetRequest(int kind) {

    }

    @Override
    public void onChargerInfoChangeReq(int kind, String value) {

    }

    @Override
    public boolean onRemoteStartStop(int cmd) {
        return false;
    }

    @Override
    public void onRecvStartChargingResp(byte rspCode, byte rspReason) {
        //아무것도 하지않음
        LogWrapper.v(TAG, "receved 1d, respcode:" + rspCode + ",rspReason:" + rspReason);
    }

    @Override
    public void onRecvFinishChargingResp(int respCode, int reasonCode) {
        //아무것도 하지않음
        LogWrapper.v(TAG, "receved 1f, respcode:" + respCode + ",rspReason:" + reasonCode);
    }

    @Override
    public void onRecvVersionResp(String version) {

    }

    @Override
    public void onFirmwareDownCompleted(byte[] fwDownData) {

    }

    @Override
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url) {
        return false;
    }

    @Override
    public void onChangeChargerMode(int mode) {

    }

    @Override
    public void onRecvCostInfo(MinsuChargerInfo pinfo) {
        //단가정보 저장
        minsuChargerInfo.saveCostInfo(mainActivity.getBaseContext(), Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, TypeDefine.COST_INFO_FILENAME);

        //단가정보 로드
        minsuChargerInfo.loadCostInfo(mainActivity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + TypeDefine.COST_INFO_FILENAME);
    }

    @Override
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }

    @Override
    public void onRecvMissingPaymentCompleteResp(byte authResult, String prepayApprovalnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }

    public enum UIFlowState {
        UI_SELECT,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_UNPLUG_CHECK
    }

    PageManger pageManger;

    MinsuUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_SELECT;
    ChargeData chargeData;
    CPConfig cpConfig;

    MinsuChargerInfo minsuChargerInfo;
    MinsuCommManager minsuCommManager;

    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    // DSP 관련 Attr
//    DSPControl dspControl;
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
    int lastDateValue = 0;

    int meterTimerCnt = 0;

    boolean isRemoteStarted = false;
    boolean isHardResetEvent = false;
    boolean isConnectorOperative = true;
    boolean restartBySoftReset = false;

    FaultManager faultManager;

    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    //RFID Reader
    RfidReader rfidReader;

    UpdateManager updateManager;
    int firmwareInstallCounter = 0;
    double powerLimit = -1;

    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();

    int unplugTimerCnt = 0;

    public UIFlowManager(MinsuUIActivity activity, ChargeData data, CPConfig config, String restartReason) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        LogWrapper.setLogWrapperListener(this);

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        //단가정보 로드
        minsuChargerInfo = new MinsuChargerInfo();
        minsuChargerInfo.loadCostInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + TypeDefine.COST_INFO_FILENAME);

        //si.로드된 충전기 설치정보를 poscoChargerInfo 객체로 전달
        minsuChargerInfo.loadCpConfigInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + CPConfig.CP_CONFIG_FILE_NAME);



        if ( cpConfig.isFastCharger == true ) {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_V28_SIZE, 1,  this);
            dspControl.setMeterType(DSPControl2.METER_TYPE_FAST);
        }
        else {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE,1, this);
            dspControl.setMeterType(DSPControl.METER_TYPE_SLOW);
        }
        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);
        dspControl.start();

        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        chargeData.rfid_cmd = RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY;
//        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
//        chargeData.rfid_cmd = RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE;
        rfidReader.setRfidReaderEvent(this);



        String basePath = Environment.getExternalStorageDirectory() + TypeDefine.REPOSITORY_BASE_PATH;
        minsuCommManager = new MinsuCommManager(activity.getBaseContext(), cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID, cpConfig.chargerID, chargeData.chargerType, minsuChargerInfo, basePath, 30 * 1000);    //add by si. 차지비 서버재접속 시도 30초간격 설정
        minsuCommManager.setMinsuCommManagerListener(this);

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        // UpdateManager를 생성한다.
        updateManager = new UpdateManager(mainActivity, this, "1");

        setChargeCostUnit();
        // 1초 타이머 시작
        startPeroidTimerSec();
        initStartState();
    }

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();

        rfidReader.stopThread();
        updateManager.closeManager();
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public PageManger getPageManager(){return pageManger;}
    public DSPControl2 getDspControl() { return dspControl; }

    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }
    public MinsuChargerInfo getMinsuChargerInfo(){return minsuChargerInfo;}

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

    public EvCommManager getCommManager() { return minsuCommManager; }
    public UIFlowState getUIFlowState() { return flowState; }

    void setUIFlowState(UIFlowState state) {
        flowState = state;

        processChangeState(flowState );
    }

    void startPeroidTimerSec() {
        timerSec = new TimeoutTimer(200, new TimeoutHandler() {
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
                break;
            case Started:
                break;
            case Error:
                break;
            case Retrying:
                break;
            case Finished:
                break;
        }
    }

    /**
     * 1초에 한번식 수행 만약 파일 다운로드 상태라고 하면 충전중이 아닐때 수행됨
     */
    void firmwareInstallProcess() {
        if ( updateManager.getStatus() == UpdateManager.UpdateState.Finished) {
            if ( flowState != UIFlowState.UI_CHARGING) {
                firmwareInstallCounter++;
                if ( firmwareInstallCounter == TypeDefine.FIRMWARE_UPDATE_COUNTER  ) {
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

//        // 읽기 해제
//        rfidReader.rfidReadRelease();
    }

    public void onPageStartEvent() {
        setUIFlowState(UIFlowState.UI_SELECT);

        if ( cpConfig.isFastCharger ) {
            pageManger.changePage(PageID.SELECT_FAST);
        }
        else {
            pageManger.changePage(PageID.SELECT_SLOW);
        }

        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;
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

        if ( isRemoteStarted || cpConfig.isAuthSkip) {
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
            // Session Start

            // Next Flow. Card Tag
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManger.changePage(PageID.CARD_TAG);

            rfidReader.rfidReadRequest();
        }

    }

    public void onPageUnplugCheckEvent(){
        setUIFlowState(UIFlowState.UI_UNPLUG_CHECK);
        pageManger.changePage(PageID.UNPLUG_CHECK);
    }
    public void onMoveToTagCardStatus(){
        setUIFlowState(UIFlowState.UI_CARD_TAG);
        pageManger.changePage(PageID.CARD_TAG);
    }
    public void onMoveToConnectorWait(){
        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
        pageManger.changePage(PageID.CONNECTOR_WAIT);
    }
    public void onMoveToCharging(){
        setUIFlowState(UIFlowState.UI_CHARGING);
        pageManger.changePage(PageID.CHARGING);
    }


    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                onPageStartEvent();
                break;
            case GO_UNPLUG:
                onPageUnplugCheckEvent();
                break;
            case GO_PREV_CONNECTOR_WAIT_TO_TAG_CARD:            //전시회용
                onMoveToTagCardStatus();
                break;
            case GO_PREV_CHARGING_TO_CONNECTOR_WAIT:            //전시회용
                onMoveToConnectorWait();
                break;
            case GO_PREV_CHG_COMPLTETE_TO_CHARGING:             //전시회용
                onMoveToCharging();
                break;
        }
    }

    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            if ( flowState == UIFlowState.UI_CARD_TAG ) {

                /**
                 * 예약중이라면 예약된 사용자가 아니면 인증 하지 않음
                 */
                if ( reserveInfo.reservationId > 0 ) {
                    boolean reserveAuth = false;
                    if ( tagNum.compareTo(reserveInfo.idTag) == 0 ) {
                        reserveAuth = true;
                    }
                    else if ( reserveInfo.parentIdTag != null) {
                        if ( tagNum.compareTo(reserveInfo.parentIdTag) == 0 ) reserveAuth = true;
                    }

                    if ( reserveAuth == false) {
                        chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                        chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content_reserved);
                        pageManger.showMessageBox();

                        goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
                    }
                    else {
                        reserveInfo.init();
                        mainActivity.setReservedVisible(View.INVISIBLE);
                        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                        onAuthResultEventLocal(true, 100);
                    }
                }
                else {
                    if(chargeData.rfid_cmd == RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE){
                        minsuChargerInfo.cardNum = "FFFFFFFF" + tagNum;
                        lastCardNum = "FFFFFFFF" + tagNum;
                    }
                    else{
                        minsuChargerInfo.cardNum = tagNum;
                        lastCardNum = tagNum;
                    }

                    if (cpConfig.isTestMode) {
                        //바로 인증 완료 화면으로 이동
                        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                        doAuthComplete();
                    } else {
                        //인증 요청 전문 전송
                        minsuCommManager.sendAuthReq();
                        // 승인대기 화면 전환
                        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                        pageManger.changePage(PageID.AUTH_WAIT);
                    }
                }
            }
            else if ( flowState == UIFlowState.UI_CHARGING ) {
                if ( tagNum.equals(lastCardNum) == true ) {
                    onChargingStop();

                }
                else {
                    // To Do.
                    // 메시지 박스!!!
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                    pageManger.showMessageBox();
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
    public void onAuthResultEventLocal(final boolean isSuccess, final int delayMs) {
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
            if (isSuccess) {
//                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                doAuthComplete();

            } else {
                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManger.getAuthWaitView().stopTimer();
                pageManger.showMessageBox();

                goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
            }
        }
    }


    void doAuthComplete() {
        pageManger.changePage(PageID.CONNECTOR_WAIT);
        // Ctype인 경우에는 도어를 오픈할 필요가 없음
        if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
        }
//        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
//        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
//            // 이미 Connect 된 상태이라면
//            onConnectedCableEvent(true);
//        } else {
//            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
//            pageManger.changePage(PageID.CONNECTOR_WAIT);
//            // Ctype인 경우에는 도어를 오픈할 필요가 없음
//            if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
//                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
//            }
//        }
    }

    /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();
        //충전시작시간 서버로 전송
        minsuChargerInfo.chargingStartTime = new Date();
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        meterTimerCnt = 0;

        minsuChargerInfo.payMethod = 0x01;      //결제방법(결제카드)
        minsuChargerInfo.socBatt = 0;
    }
    public void setChargeCostUnit(){
        try{
            Calendar rightnow = Calendar.getInstance();
            int curr_hour = rightnow.get(Calendar.HOUR_OF_DAY);
            chargeData.chargingUnitCost = minsuChargerInfo.costxx[curr_hour];
        }catch (Exception e){
            LogWrapper.e(TAG,e.toString());
        }
    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            LogWrapper.v(TAG, "Start Charging");

            lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

            //단가정보 로드
            setChargeCostUnit();

            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();


            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);

            if(cpConfig.isTestMode || cpConfig.isAuthSkip) {}
            else {
                if (minsuChargerInfo.cpMode == 5) {
                    minsuChargerInfo.cpMode = 9;             //충전중 모드 변경 - add by si.200901
                    minsuChargerInfo.pre_cpMode = minsuChargerInfo.cpMode;
                    minsuCommManager.sendStartCharging();       //d1 전문 전송(자동인증이나 테스트모드가 아닐 경우에만 충전시작전문 전송)
                }
            }

            // RFID (충전 중지)
            rfidReader.rfidReadRequest();

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        }
    }

    public void setUIConnectorWaitStat(){
        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
        pageManger.changePage(PageID.CONNECT_CAR_WAIT);
    }

    public void onConnectedCableEvent(boolean isConnected) {
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
        }
        else {

        }
    }

    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

            if(cpConfig.isTestMode || cpConfig.isAuthSkip){}
            else {
                if (minsuChargerInfo.cpMode == 9) {
                    minsuChargerInfo.cpMode = 5;
                    minsuChargerInfo.pre_cpMode = minsuChargerInfo.cpMode;
                    //종료 soc값 저장
                    minsuChargerInfo.finish_socBatt = (byte)chargeData.soc;
                    minsuCommManager.sendFinishCharging();
                }
            }

            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManger.changePage(PageID.FINISH_CHARGING);


            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    public void onChargingStop() {
        if(cpConfig.isTestMode){
            onFinishChargingEvent();        //종료신호 주고받는것 없이 바로 충전완료화면으로 이동
        }
        else{
            //DSP에 STOP 신호를 보낸다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
            pageManger.getChargingView().onChargingStop();
        }
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

        // 날짜 변화 체크
        checkTransitionDay();

        //Reserve Check
        if ( reserveInfo.expiryCheck() == true ) mainActivity.setReservedVisible(View.INVISIBLE);

        //전력량계 모니터링
        getMeterValueProcess();

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();

            int chargingTimeSec = (int) (chargeData.chargingTime / 1000);

            // 충전중 상태 정보 주기에 따라 값을 보낸다.(e1)
            if (chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD) == 0) {
                if(cpConfig.isTestMode || cpConfig.isAuthSkip){}
                else{
                    minsuCommManager.sendChargingStatus();
                }

            }
            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;

            //서버로 전송
            minsuChargerInfo.remainTime = chargeData.remainTime;
            minsuChargerInfo.socBatt = (byte)chargeData.soc;
            minsuChargerInfo.chargingTime = chargingTimeSec;

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

//        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
//        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
//            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
//            if (unplugTimerCnt++> 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
//                if(cpConfig.isTestMode || cpConfig.isAuthSkip){}
//                else minsuCommManager.sendUnplug();
//                onPageCommonEvent(PageEvent.GO_HOME);
//            }
//        }
        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
        if ( getUIFlowState() == UIFlowState.UI_UNPLUG_CHECK ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++> 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
                if(cpConfig.isTestMode || cpConfig.isAuthSkip){}
                else minsuCommManager.sendUnplug();
                onPageCommonEvent(PageEvent.GO_HOME);
            }
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

                minsuChargerInfo.meterVal = (int)meterVal;

                //TODO : meter 통신 오류 체크 구현 필요

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
                                minsuChargerInfo.curChargingKwh = (int)chargeData.measureWh;
                                minsuChargerInfo.curChargingCost = (int)chargeData.chargingCost;
                                minsuChargerInfo.curChargingCostUnit = chargeData.chargingUnitCost;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                } else {
                    // Meter Error !!!
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);
                    minsuChargerInfo.meterVal = (int)lastMeterValue;
                }
            }catch(Exception e){
                LogWrapper.e(TAG, "Meter Err:"+e.toString());
            }
        }

    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if ( curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue ) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);
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
    public void onDspStatusChange(int channel, DSPRxData.STATUS400 idx, boolean val) {
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
                    onConnectedCableEvent(true);
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

    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false ) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";

                //서버로 폴트알람 전송
                chargeData.faultcode = fInfo.alarmCode;
                minsuCommManager.SendAlarm(chargeData.faultcode, AlarmCode.STATE_OCCUR);
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
                    FaultInfo newInfo = new FaultInfo(fInfo.id, fInfo.errorCode, fInfo.errorMsg, fInfo.isRepair, fInfo.alarmCode);
                    faultList.add(newInfo);
                }
            }
        }

        if ( isPreDspFault != isDspFault ) {
            if ( isDspFault == true ) {
                // 충전충이라면 충전을 중지한다.
                if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

                    onChargingStop();
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_SELECT )  {
                    onPageStartEvent();
                }

                fillFaultMessage();
                pageManger.showFaultBox();
            }
            else {
                if (chargeData.faultcode != 0)
                    minsuCommManager.SendAlarm(chargeData.faultcode, AlarmCode.STATE_RESTORE);
                pageManger.hideFaultBox();
            }
            isPreDspFault = isDspFault;
        }

        if ( isDspFault == true ) {
        }

        // 긴급버턴 이벤트 발생
        if ( isEmergencyPressed != isEmergency ) {
            if (isEmergency == true) {
                pageManger.showEmergencyBox();
            } else { // 긴급 버턴 해제
                pageManger.hideEmergencyBox();
            }
            isEmergencyPressed = isEmergency;
        }
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
                    onConnectedCableEvent(true);
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
        if(cpConfig.isTestMode){
            //dsp comm err 표시안함(전시회버전)
        }
        else{
            if (isError == true) {
                pageManger.showDspCommErrView();
            }
            else {
                pageManger.hideDspCommErrView();
            }
        }

    }

    public void onAuthSuccess(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEventLocal(true, 1000);
            }
        }
    }

    public void onAuthFailed(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEventLocal(false, 1000);
            }
        }
    }

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


    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
            onCardTagEvent(rfid, true);
        }
        LogWrapper.v(TAG, "Card Tag Event:"+rfid);

        rfidReader.rfidReadRelease();
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
}
