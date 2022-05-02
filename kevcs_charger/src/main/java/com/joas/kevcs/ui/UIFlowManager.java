/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:37
 *
 */

package com.joas.kevcs.ui;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.kevcs.ui.page.PageEvent;
import com.joas.kevcs.ui.page.PageID;
import com.joas.kevcscomm.FaultInfo;
import com.joas.kevcscomm.KevcsChargeCtl;
import com.joas.kevcscomm.KevcsChargerInfo;
import com.joas.kevcscomm.KevcsComm;
import com.joas.kevcscomm.KevcsCommListener;
import com.joas.kevcscomm.KevcsCostManager;
import com.joas.kevcscomm.KevcsProtocol;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;
import com.joas.utils.ZipUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, KevcsCommListener {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_START,
        UI_SELECT,
        UI_SELECT_AUTH,
        UI_AUTH_MEMBER_CARD,
        UI_INPUT_NUMBER,
        UI_AUTH_WAIT_NUMBER,
        UI_INPUT_PASSWORD,
        UI_AUTH_WAIT_PASSWD,
        UI_SELECT_AMOUNT_TYPE,
        UI_INPUT_AMOUNT_COST,
        UI_INPUT_AMOUNT_KWH,
        UI_CREDIT_CARD_PAY,
        UI_AUTH_WAIT_CARD,
        UI_PREPAY_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_FINISH_CHARGING_ERROR,
        UI_CREDIT_REAL_PAY,
        UI_CREDIT_REAL_PAY_WAIT,
        UI_CREDIT_CANCEL_PREPAY,
        UI_CREDIT_CANCEL_PREPAY_WAIT,
        UI_UNPLUG

    }

    public static final int SLOW_METER_SCALE_VAL = 10;
    public static final int FAST_METER_SCALE_VAL = 10;

    PageManger pageManger;

    KevcsUIActivity mainActivity;
    StatusBarControl statusBarControl;

    UIFlowState flowState = UIFlowState.UI_START;
    ChargeData chargeData;
    CPConfig cpConfig;

    //RFID Reader
    RfidReader rfidReader;

    //통신 모듈
    KevcsComm kevcsComm;

    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    // DSP 관련 Attr
    DSPControl2 dspControl;
    int dspVersion = 0;
    boolean isDspReady = false;
    boolean isDspAvalCharge = false;
    boolean isDspDoor = false;
    boolean isDspPlug = false;
    boolean isDspChargeRun = false;
    boolean isDspChargeFinish = false;

    boolean isDspFault = false;
    boolean isPreDspFault = false;
    boolean isEmergencyPressed = false;

    long lastMeterValue = 0;
    long oldLastMeterValue = 0;
    int meterNoChangeCnt = 0;

    TimeoutTimer timerSec = null;
    String lastCardNum = "";

    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    // Reset Control
    KevcsChargeCtl resetCtl = null;
    boolean resetFlag = false;
    boolean enterResetMode = false;
    int resetCount = 0;

    //Update Flag
    boolean updateFinishFlag = false;

    // UI Timeout 처리 카운터
    int uiHomeTimerCnt = 0;

    // 플러그 원위치 플래그
    boolean isPlugOrgPosition = true;
    boolean isPlugOrgPositionError = false;
    int isPlugOrgPositionErrorCnt = 0;
    int plugOrgPositionAlarmCnt = TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX;

    // 사운드관련
    SoundManager soundManager;

    // 통신 미연결 카운터
    int commNotConnectCnt = 0;

    // 도어 오픈 에러
    int doorOpenErrCnt = 0;
    boolean isDoorOpenError = false;
    boolean isDoorOpenErrorCType = false;

    // 결제단말기 에러
    boolean isPayTeminalError = false;
    KevcsComm.PayTerminalKind payTerminalKind = KevcsComm.PayTerminalKind.TL3500S;

    String[] lastMeterStringList = null;
    boolean dispMeteringStringTempLock = false;
    int dispChargingMeterStrCnt = 0;
    int dispChargingMeterBacklightCnt = 0;

    boolean isCreditCardPrePayed = false;

    public UIFlowManager(KevcsUIActivity activity, ChargeData data, CPConfig config) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        soundManager = new SoundManager(activity, config);

        statusBarControl = new StatusBarControl(activity, this);
        statusBarControl.setChargerID(cpConfig.chargerID);

        if ( cpConfig.isFastCharger == true ) {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG23_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, FAST_METER_SCALE_VAL, this);
        }
        else {
            dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE, SLOW_METER_SCALE_VAL, this);
        }
        dspControl.start();

        if ( !cpConfig.usePayTerminal ) {
            rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
            rfidReader.setRfidReaderEvent(this);
        }

        kevcsComm = new KevcsComm(
                activity,
                TypeDefine.SW_VERSION,
                cpConfig.serverIP,
                cpConfig.serverPort,
                cpConfig.chargerID,
                cpConfig.serverAuthKey,
                Environment.getExternalStorageDirectory()+TypeDefine.KEVCS_COMM_PATH,
                "/dev/ttyS3",
                cpConfig.usePayTerminal,
                payTerminalKind,
                cpConfig.usePayTerminal);
        kevcsComm.setListener(this);

        initConfigValue();

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel, cpConfig.isFastCharger);
        faultList = faultManager.loadFaultStatus(Environment.getExternalStorageDirectory() + TypeDefine.REPOSITORY_BASE_PATH, TypeDefine.FAULT_INFO_FILENAME);
        for (FaultInfo fInfo: faultList) {
            //계량기 및 외함개방 처음 부팅시에 에러관련 화면 표시안함(Fault시 화면 표시, 복구를 위한 패킷은 서버 연결시 적용)
            faultStatusInit(fInfo, false);
        }
        // 1초 타이머 시작
        startPeroidTimerSec();

        kevcsComm.startComm();
    }

    public void showInitDisplay() {
        showMessageBox("충전기가 준비중입니다. 잠시만 기다려주세요.", 30, false, false);
    }

    public void loadResetInfo() {
        try {
            File file = new File(Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH + "/" + TypeDefine.RESET_INFO_FILENAME);

            if (file.exists()) {
                KevcsChargeCtl ctrl = new KevcsChargeCtl();
                ctrl.loagFromFile(Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH, TypeDefine.RESET_INFO_FILENAME);
                ctrl.state = KevcsChargeCtl.State.FINISHED;
                ctrl.end_datetime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
                kevcsComm.sendGlobChargeCtlCompt_15(ctrl, true);
                file.delete();
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "loadResetInfo err:"+e.toString());
        }
    }

    void faultStatusInit(FaultInfo fInfo, boolean isDisplay) {
        switch ( fInfo.id ) {
            // 전력계량기 오류
            case 41610: if ( isDisplay ) statusBarControl.setMeterStatusError(!fInfo.isRepair);
                break;

            // 외함 열림
            case 40603: if ( isDisplay ) statusBarControl.setOpenEnclosure(!fInfo.isRepair);
                break;

            // 커넥터 Lock 오류
            case 5902:
                isDoorOpenError = true;
                break;
            case 6902:
                isDoorOpenErrorCType = true;
                break;
            // 결제모듈 오류
            case 5905:
                isPayTeminalError = true;
                kevcsComm.setPayStatError(true);
                break;
            // 위치센서 오류
            case 5903:
                isPlugOrgPositionError = true;
                break;
        }

    }

    public void closeManager() {
        stopWatdogTimer();
        dspControl.interrupt();
        dspControl.stopThread();
        kevcsComm.stopComm();
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }
    public SoundManager getSoundManager() { return soundManager; }

    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }

    public KevcsComm getKevcsComm() { return kevcsComm; }

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

    public UIFlowState getUIFlowState() { return flowState; }

    public boolean getIsDspPlug() { return isDspPlug; }
    public long getLastMeterValue() { return lastMeterValue; }
    public boolean getIsPlugOrgPosition() { return isPlugOrgPosition; }

    void setUIFlowState(UIFlowState state) {
        setUIFlowState(state, true);
    }

    void setUIFlowState(UIFlowState state, boolean isPlaySound) {
        boolean changeState = (flowState != state);
        flowState = state;

        processChangeState(flowState);
        setStatusBarControl(flowState);
        if ( isPlaySound && changeState ) playVoiceState(state);
    }

    void initConfigValue() {
        dspControl.setMeterUse(true);
        if (cpConfig.isFastCharger == true ) {
            dspControl.setMeterType(DSPControl2.METER_TYPE_FAST);
            kevcsComm.getChargerInfo().outlet_type = KevcsProtocol.OutletType.DC_COMBO;
            kevcsComm.getChargerInfo().charge_tp = KevcsProtocol.CHARGER_TYPE_FAST; // 급속
            kevcsComm.getChargerInfo().cp_tp = 7; // 급속 콤보
            kevcsComm.getChargerInfo().charge_ability = 50; // 50kw
            kevcsComm.getChargerInfo().model_nm = TypeDefine.FAST_MODEL_NAME;
        }
        else {
            dspControl.setMeterType(DSPControl2.METER_TYPE_SLOW);
            kevcsComm.getChargerInfo().outlet_type = KevcsProtocol.OutletType.AC_SLOW_5PIN;
            kevcsComm.getChargerInfo().charge_tp = KevcsProtocol.CHARGER_TYPE_SLOW; // 완속
            kevcsComm.getChargerInfo().cp_tp = 3; // BC 5핀
            kevcsComm.getChargerInfo().charge_ability = 7; /// 7kW
            kevcsComm.getChargerInfo().model_nm = TypeDefine.SLOW_MODEL_NAME;
        }

        statusBarControl.setChargerID(cpConfig.chargerID);

        kevcsComm.setChargerID(cpConfig.chargerID);
        kevcsComm.setServerAuthKey(cpConfig.serverAuthKey);
        kevcsComm.setServerAddr(cpConfig.serverIP, cpConfig.serverPort);

        kevcsComm.getChargerInfo().cp_stat = KevcsProtocol.CPStat.READY;
        kevcsComm.getChargerInfo().mng_pass = cpConfig.settingPassword;
        kevcsComm.getChargerInfo().cp_ip = NetUtil.getLocalIpAddress();

        kevcsComm.getChargerInfo().cp_firmware_ver = TypeDefine.SW_VERSION;
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
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련, UI 타이머(첫화면) 처리
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */

    void processChangeState(UIFlowState state) {
        uiHomeTimerCnt = 0;

        switch ( state ) {
            case UI_START:
                initStartState();
                break;

            case UI_SELECT:
                isPlugOrgPositionErrorCnt = 0; // 충전시작시 초기화
                // 한전 시작을 누르고 첫화면 다시 갈때 알람 울리지 않음
                plugOrgPositionAlarmCnt = TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX;
            case UI_SELECT_AUTH:
            case UI_AUTH_MEMBER_CARD:
            case UI_INPUT_NUMBER:
            case UI_INPUT_PASSWORD:
            case UI_SELECT_AMOUNT_TYPE:
            case UI_INPUT_AMOUNT_COST:
            case UI_INPUT_AMOUNT_KWH:
            case UI_CREDIT_CARD_PAY:
                uiHomeTimerCnt = TypeDefine.UI_DEFAULT_HOME_TIMEOUT;
                break;

            case UI_CONNECTOR_WAIT:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_CONNECT);
                if (isDspPlug == false) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                uiHomeTimerCnt = TypeDefine.UI_CONNECTOR_WAIT_HOME_TIMEOUT; // 결제시 시퀀스 변경 필요!
                break;

            case UI_RUN_CHECK:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                break;

            case UI_FINISH_CHARGING:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);

                uiHomeTimerCnt = TypeDefine.UI_DEFAULT_HOME_TIMEOUT;
                plugOrgPositionAlarmCnt = 0;
                break;

            case UI_FINISH_CHARGING_ERROR:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                // 에러나 나면 START_CHARGE를 false로 충전완료를 true로 세팅한다.
                // 시간 초과시에 START가 살아있으면 안됨(초기화면에서 리셋됨)
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
                uiHomeTimerCnt = TypeDefine.UI_DEFAULT_HOME_TIMEOUT;
                plugOrgPositionAlarmCnt = 0;
                break;
        }
    }

    public void playVoiceCurState() {
        playVoiceState(getUIFlowState());
    }

    public void playVoiceState(UIFlowState state) {
        if (isDspFault) return; // fault 중일때는 음성을 플레이 하지 않음
        switch ( state ) {
            case UI_START:
                soundManager.playSound(SoundManager.SoundKind.Ready);
                break;
            case UI_SELECT:
                soundManager.playSound(SoundManager.SoundKind.CableSelect);
                break;
            case UI_SELECT_AUTH:
                soundManager.playSound(SoundManager.SoundKind.AuthSelect);
                break;
            case UI_AUTH_MEMBER_CARD:
                soundManager.playSound(SoundManager.SoundKind.AuthCard);
                break;
            case UI_INPUT_NUMBER:
                soundManager.playSound(SoundManager.SoundKind.AuthNumber);
                break;
            case UI_INPUT_PASSWORD:
                soundManager.playSound(SoundManager.SoundKind.AuthPassword);
                break;
            case UI_SELECT_AMOUNT_TYPE:
                soundManager.playSound(SoundManager.SoundKind.SelectMethod);
                break;
            case UI_INPUT_AMOUNT_COST:
                soundManager.playSound(SoundManager.SoundKind.AuthCredit);
                break;
            case UI_INPUT_AMOUNT_KWH:
                soundManager.playSound(SoundManager.SoundKind.AuthCreditKwh);
                break;
            case UI_CREDIT_CARD_PAY:
                soundManager.playSound(SoundManager.SoundKind.CreditCardTag);
                break;
            case UI_AUTH_WAIT_CARD:
                soundManager.playSound(SoundManager.SoundKind.AuthWait);
                break;
            case UI_AUTH_WAIT_PASSWD:
                soundManager.playSound(SoundManager.SoundKind.AuthWait);
                break;
            case UI_PREPAY_WAIT:
                soundManager.playSound(SoundManager.SoundKind.AuthCreditWait);
                break;
            case UI_CONNECTOR_WAIT:
                soundManager.playSound(SoundManager.SoundKind.ConnectWait);
                break;
            case UI_RUN_CHECK:
                soundManager.playSound(SoundManager.SoundKind.Connecting);
                break;
            case UI_CHARGING:
                soundManager.playSound(SoundManager.SoundKind.Charging);
                break;
            case UI_FINISH_CHARGING:
                soundManager.playSound(SoundManager.SoundKind.FinishCharging);
                break;
            case UI_FINISH_CHARGING_ERROR:
                soundManager.playSound(SoundManager.SoundKind.ChargingError);
                break;
            case UI_UNPLUG:
                soundManager.playSound(SoundManager.SoundKind.Unplug);
                break;
        }
    }

    // 상태바의 상태중에 홈버튼 및 충전중 아이콘 표시 적용
    void setStatusBarControl(UIFlowState state) {
        boolean value = false;
        switch ( state ) {
            case UI_START:
                value = false;
                break;

            case UI_SELECT:
            case UI_SELECT_AUTH:
            case UI_AUTH_MEMBER_CARD:
            case UI_INPUT_NUMBER:
            case UI_INPUT_PASSWORD:
            case UI_SELECT_AMOUNT_TYPE:
            case UI_INPUT_AMOUNT_COST:
            case UI_INPUT_AMOUNT_KWH:
            case UI_CREDIT_CARD_PAY:
            case UI_AUTH_WAIT_CARD:
                value = true;
                break;

            case UI_PREPAY_WAIT:
                value = true;
                break;

            case UI_CONNECTOR_WAIT:
                value = true;
                break;

            case UI_RUN_CHECK:
            case UI_CHARGING:
            case UI_FINISH_CHARGING:
                value = false;   //homebutton
                break;
            case UI_CREDIT_REAL_PAY:
                value = true;
                break;
            case UI_CREDIT_CANCEL_PREPAY:
                value = true;
                break;
            case UI_UNPLUG:
                value = true;
                break;
        }

        boolean chargingStatus = false;
        if ( state == UIFlowState.UI_CHARGING ) chargingStatus = true;

        final boolean ishomeVisible = value;
        final boolean chgStatus = chargingStatus;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusBarControl.setHomeVisible(ishomeVisible);
                statusBarControl.setChargingStatus(chgStatus);
            }
        });
    }

    public void onEnterAdminMode() {
        pageManger.showAdminPasswrodInputView();
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

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        initChargingStartValue();   //충전 대기화면 이동시 각종 정보 초기화 34로 전문 날아갈때 정리
        kevcsComm.getCostManager().initCostSumValues(); //충전 대기화면 이동시 각종 정보 초기화 34로 전문 날아갈때 정리 코스트 정보 초기화
        // 통신 프로그램 세팅
        if (cpConfig.isFastCharger == true ) {
            kevcsComm.getChargerInfo().outlet_type = KevcsProtocol.OutletType.DC_COMBO;
        }
        else {
            kevcsComm.getChargerInfo().outlet_type = KevcsProtocol.OutletType.AC_SLOW_5PIN;
        }
        kevcsComm.setCPStat(KevcsProtocol.CPStat.READY);

        doorOpenErrCnt = 0;

        // 안테나 OFF 요청
        if (cpConfig.usePayTerminal) {
            kevcsComm.payTerminalAntReq(false);
        }else {
            rfidReader.rfidReadRelease();
        }

        //Payment 초기화
        chargeData.reqPayCost = 0;
        chargeData.reqPayKwh = 0.0;
        chargeData.isAuthCredit = false;
        isCreditCardPrePayed = false;
    }

    void recordSystemLog(String strLog) {
        FileUtil.appendDateLog(Environment.getExternalStorageDirectory()+TypeDefine.SYSLOG_PATH, strLog);
    }

    //===================================
    // UI Event 처리(버튼, 타이머등)
    //===================================

    void doCableSelect(PageEvent event) {
        switch ( event ) {
            case SELECT_AC3_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.AC3;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_AC3);
                kevcsComm.setOutletType(KevcsProtocol.OutletType.AC3);
                break;

            case SELECT_CHADEMO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCHADEMO);
                kevcsComm.setOutletType(KevcsProtocol.OutletType.DC_CHADEMO);
                break;

            case SELECT_DCCOMBO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCOMBO);
                kevcsComm.setOutletType(KevcsProtocol.OutletType.DC_COMBO);
                break;

            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                kevcsComm.setOutletType(KevcsProtocol.OutletType.AC_SLOW_7PIN);
                break;

            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                kevcsComm.setOutletType(KevcsProtocol.OutletType.AC_SLOW_5PIN);
                break;
        }

        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
    }

    public void onPageSelectEvent(PageEvent event) {
        kevcsComm.getCostManager().updateCurrentCostTable(); // 단가 업데이트

        doCableSelect(event);

        // 자동인증인경우 인증 과정 생략
        if ( cpConfig.isAuthSkip == true ) {
            recordSystemLog("(자동인증완료)");
            setUIFlowState(UIFlowState.UI_AUTH_WAIT_CARD);
            onAuthResult(true);
        }
        else {
            setUIFlowState(UIFlowState.UI_SELECT_AUTH);
            pageManger.changePage(PageID.SELECT_AUTH);
        }
    }

    public void onPageSelectAuthEvent(PageEvent event) {
        switch ( event ) {
            case SELECT_MEMBER_CARD:
                if (cpConfig.usePayTerminal) {
                    kevcsComm.payTerminalAntReq(true);
                }
                else {
                    rfidReader.rfidReadRequest();
                }

                setUIFlowState(UIFlowState.UI_AUTH_MEMBER_CARD);
                pageManger.changePage(PageID.AUTH_MEMBER_CARD);
                break;

            case SELECT_MEMBER_NUM:
                setUIFlowState(UIFlowState.UI_INPUT_NUMBER);
                pageManger.changePage(PageID.INPUT_NUMBER);
                break;

            case SELECT_CREDIT_CARD:
                kevcsComm.getCostManager().updateCurrentCostTable(); // 단가 업데이트

                setUIFlowState(UIFlowState.UI_SELECT_AMOUNT_TYPE);
                pageManger.changePage(PageID.SELECT_AMOUNT_TYPE);
                chargeData.isAuthCredit = true; // 비회원 인증 설정
                break;
        }
    }

    // 회원 번호 입력이 끝이 났을때 호출됨
    public void onPageInputNumberComplete(String number) {
        setUIFlowState(UIFlowState.UI_AUTH_WAIT_NUMBER);
        pageManger.changePage(PageID.AUTH_PAY_WAIT);
        lastCardNum = number;
        kevcsComm.memberAuthReq(lastCardNum , KevcsProtocol.CERT_TP_MEMBER_NUM, "");
    }

    public void onPageInputPasswordComplete(String password) {
        setUIFlowState(UIFlowState.UI_AUTH_WAIT_PASSWD);
        pageManger.changePage(PageID.AUTH_PAY_WAIT);
        kevcsComm.memberAuthReq(lastCardNum , KevcsProtocol.CERT_TP_MEMBER_NUM, password);
    }

    public void onPageSelectAmountType(PageEvent event) {
        switch ( event ) {
            case SELECT_AMOUNT_COST_CLICK:
                setUIFlowState(UIFlowState.UI_INPUT_AMOUNT_COST);
                pageManger.changePage(PageID.INPUT_AMOUNT_COST);
                kevcsComm.setChargeReqCfmMethod(KevcsProtocol.CHARGE_REQ_CFM_METHOD_COST);
                break;

            case SELECT_AMOUNT_KWH_CLICK:
                setUIFlowState(UIFlowState.UI_INPUT_AMOUNT_KWH);
                pageManger.changePage(PageID.INPUT_AMOUNT_KWH);
                kevcsComm.setChargeReqCfmMethod(KevcsProtocol.CHARGE_REQ_CFM_METHOD_KWH);
                break;

            case SELECT_AMOUNT_FULL_CLICK:  //풀 선택시
                setUIFlowState(UIFlowState.UI_CREDIT_CARD_PAY);
                pageManger.changePage(PageID.CREDIT_CARD_PAY);

                // 한전 프로토콜 지원 단말기인 경우 안테나 On을 보낸다.
                if ( payTerminalKind == KevcsComm.PayTerminalKind.STANDARD) kevcsComm.payTerminalAntReq(true);
                else if (payTerminalKind == KevcsComm.PayTerminalKind.TL3500S ) {
                    // 테크리더인경우에는 바로 결제 요청을 진행함
                    kevcsComm.payRequest(KevcsProtocol.APRV_TYPE_PREPAY, chargeData.reqPayKwh, kevcsComm.getCostManager().getBasePaymAmt());
                }
                kevcsComm.setChargeReqCfmMethod(KevcsProtocol.CHARGE_REQ_CFM_METHOD_FULL);
                getChargeData().reqPayCost = kevcsComm.getCostManager().getBasePaymAmt();
                getChargeData().reqPayKwh = getChargeData().reqPayCost/kevcsComm.getCostManager().getNoMemberCost();
                break;
        }
    }

    // 비회원 가격 입력 완료(또는 kWh 입력 완료)
    public void onPageInputCostComplete() {
        setUIFlowState(UIFlowState.UI_CREDIT_CARD_PAY);
        pageManger.changePage(PageID.CREDIT_CARD_PAY);

        // 한전 프로토콜 지원 단말기인 경우 안테나 On을 보낸다.
        if ( payTerminalKind == KevcsComm.PayTerminalKind.STANDARD) kevcsComm.payTerminalAntReq(true);
        else if (payTerminalKind == KevcsComm.PayTerminalKind.TL3500S ) {
            // 테크리더인경우에는 바로 결제 요청을 진행함
            kevcsComm.payRequest(KevcsProtocol.APRV_TYPE_PREPAY, chargeData.reqPayKwh, chargeData.reqPayCost);
        }
    }

    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case COMMON_GO_HOME:
                goStartProcess();
                break;

            case START_CLICK:
                if ( dspControl.getDspCommError() ) {
                    showMessageBox("UI-제어보드 통신에러입니다.", 20, true, true);
                    break;
                }

                kevcsComm.setCPStat(KevcsProtocol.CPStat.START);

                setUIFlowState(UIFlowState.UI_SELECT);
                if ( cpConfig.isFastCharger ) {
                    pageManger.changePage(PageID.SELECT_FAST);
                }
                else {
                    pageManger.changePage(PageID.SELECT_SLOW);
                }
                break;
        }
    }

    // Home 버턴을 눌러졌을떄 처리
    public void onHomeBtClick() {

        // Connector Wait일때 사용자 종료로 함.
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
            kevcsComm.getChargerInfo().charge_end_stat = KevcsProtocol.CHARGE_END_USER;
        }
        if (getUIFlowState() == UIFlowState.UI_CREDIT_REAL_PAY || getUIFlowState() == UIFlowState.UI_CREDIT_CANCEL_PREPAY ){
            setUIFlowState(UIFlowState.UI_START);
            pageManger.changePage(PageID.START);
        }

        goStartProcess();
    }
    public void setCreditCardPrePayed(boolean tf) {
        isCreditCardPrePayed = tf;
    }

    // goStartProcess 에 실결재 및 결제 취소 적용
    public void goStartProcess() {
        // 선결제가 있는경우 리턴
        if ( isCreditCardPrePayed ) {
            // 만약 사용량이 50원 미만인경우 선결제 취소
            // 한전 프로토콜 지원 단말기인 경우 안테나 On을 보낸다.
            if ( chargeData.chargingCost < 50 ) {
                kevcsComm.getChargerInfo().paym_aprv_type = ""+ KevcsProtocol.APRV_TYPE_CANCEL_PREPAY;
                kevcsComm.payCancel();
                setUIFlowState(UIFlowState.UI_CREDIT_CANCEL_PREPAY);
                pageManger.changePage(PageID.CREDIT_CANCEL_PREPAY);
            }
            else { // 실결제
                kevcsComm.payRealRequest(KevcsProtocol.APRV_TYPE_REALPAY, chargeData.reqPayKwh, (int)chargeData.chargingCost);
                setUIFlowState(UIFlowState.UI_CREDIT_REAL_PAY);
                pageManger.changePage(PageID.CREDIT_REAL_PAY);
            }
            return;
        }

        setUIFlowState(UIFlowState.UI_START);
        pageManger.changePage(PageID.START);
    }

    public void onConnectCarWaitTimeout() {
        chargeData.faultBoxContent = "차량 연결 시간 초과";
        setUIFlowState(UIFlowState.UI_FINISH_CHARGING_ERROR);
        pageManger.changePage(PageID.FINISH_CHARGING_ERRROR);

        kevcsComm.getChargerInfo().charge_end_stat = KevcsProtocol.CHARGE_END_FAULT;

        recordSystemLog("(차량연결시간초과) 카드번호:"+kevcsComm.getChargerInfo().member_card_no);

        //선결제 있을때 finishCharge보냄
        if ( chargeData.isAuthCredit ) {
            kevcsComm.finishCharge();
        }
    }

    // 충전 종료시 확인 버턴
    public void onPageEventFinishOKClick() {
        // 만약 선결제가 있을경우
        if (isCreditCardPrePayed) {
            goStartProcess();   //초기 화면 이동 시퀀스
        } else {    //없을 경우
            setUIFlowState(UIFlowState.UI_UNPLUG);
            pageManger.changePage(PageID.UNPLUG);
        }
    }
    public void onPageEventFinishErrorOKClick() {
        // 만약 선결제가 있을경우
        if (isCreditCardPrePayed) {
            goStartProcess();   //초기 화면 이동 시퀀스
        } else {    //없을 경우
            setUIFlowState(UIFlowState.UI_UNPLUG);
            pageManger.changePage(PageID.UNPLUG);
        }
    }

    public void onChargingStopAsk() {
        soundManager.playSound(SoundManager.SoundKind.AskStopCharging);
        pageManger.showChargingStopAskView();
    }

    public void onChargingStopAskResult(boolean tf) {
        pageManger.hideChargingStopAskView();
        if ( tf ) {
            kevcsComm.getChargerInfo().charge_end_stat = KevcsProtocol.CHARGE_END_USER;
            stopCharging();
            pageManger.getChargingView().onChargingStop();
        }
    }

    public void onAuthPayTimeout() {
        showMessageBox("시간이 초과하였습니다.", 20, true, true);
        goStartProcess();
        /*setUIFlowState(UIFlowState.UI_START, false);
        pageManger.changePage(PageID.START);*/
    }
    //=================================== UI Event 끝

    public void onAuthResult(boolean isSuccess) {
        if ( isSuccess == true ) {
            if ( getUIFlowState() == UIFlowState.UI_AUTH_WAIT_CARD || getUIFlowState() == UIFlowState.UI_AUTH_WAIT_PASSWD ) {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                if ( isDspPlug ) onCablePlugEvent(true);
                else pageManger.changePage(PageID.CONNECTOR_WAIT);

                // 안테나 OFF 요청
                if (cpConfig.usePayTerminal && getUIFlowState() == UIFlowState.UI_AUTH_WAIT_CARD) {
                    kevcsComm.payTerminalAntReq(false);
                }
                recordSystemLog("(인증완료) 카드번호:"+kevcsComm.getChargerInfo().member_card_no);
            }
            else if ( getUIFlowState() == UIFlowState.UI_AUTH_WAIT_NUMBER ) {
                setUIFlowState(UIFlowState.UI_INPUT_PASSWORD);
                pageManger.changePage(PageID.INPUT_PASSWORD);
            }
        }
        else {
            if ( getUIFlowState() == UIFlowState.UI_AUTH_WAIT_CARD ) { //인증 에러
                setUIFlowState(UIFlowState.UI_START, false);
                pageManger.changePage(PageID.START);

                FaultInfo fInfo = new FaultInfo(5503, "사용자 인증실패로 인한 충전불가", false, 50, 503);
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR_END);

                showMessageBox("회원확인이 실패하였습니다.", 20, true, true);
            }
            else if( getUIFlowState() == UIFlowState.UI_AUTH_WAIT_PASSWD ) { // 비밀번호 에러
                showMessageBox("잘못된 비밀번호입니다.", 20, true, true);
                setUIFlowState(UIFlowState.UI_INPUT_PASSWORD, false);
                pageManger.changePage(PageID.INPUT_PASSWORD);
            }
            else {
                showMessageBox("등록되지 않은 회원번호입니다.", 20, true, true);
                setUIFlowState(UIFlowState.UI_INPUT_NUMBER, false);
                pageManger.changePage(PageID.INPUT_NUMBER);
            }
        }

        if (cpConfig.usePayTerminal) {
            kevcsComm.payTerminalAntReq(false);
        }else {
            rfidReader.rfidReadRelease();
        }
    }

    void showMessageBox(String msg, int timeoutSec, boolean retryVisible, boolean okBtUse) {
        chargeData.messageBoxContent = msg;
        chargeData.messageBoxTimeout = timeoutSec;
        chargeData.messageBoxRetryVisible = retryVisible;
        chargeData.messageBoxOkBtUse = okBtUse;
        pageManger.showMessageBox();
    }

     /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        chargeData.soc = 0;
        chargeData.remainTime = 0;
        meterNoChangeCnt = 0;
        //isCreditCardPrePayed = false;
    }

    /**
     * 충전 시작일때 이벤트
     * dsp에서 이벤트나 타이머에서 체크시에 변경됨(둘중에 한번만 호출됨)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    synchronized public void onChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            //lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel); 매터 프로그램 사용하면서 주석 처리 201108 swpark

            // 변수를 초기화 한다.
            initChargingStartValue();

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);

            kevcsComm.setCPStat(KevcsProtocol.CPStat.CHARGING);

            kevcsComm.startCharge((double)lastMeterValue/1000.0);

            recordSystemLog("(충전시작) 카드번호:"+kevcsComm.getChargerInfo().member_card_no+", 계량기값:"+lastMeterValue);
        }
    }

    public void onFinishChargingEvent() {
        // DSP 충전 종료 신호 초기화
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

        // 폴트가 있는경우 폴트를 먼저 처리함
        onFaultEventProcess();

        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManger.changePage(PageID.FINISH_CHARGING);
            pageManger.hideChargingStopAskView();
            kevcsComm.finishCharge();

            recordSystemLog("(충전완료) 카드번호:"+kevcsComm.getChargerInfo().member_card_no +
                    ", 계량기값:" + lastMeterValue +
                    ", 충전시간:" + kevcsComm.getChargerInfo().charge_accum_time +
                    ", 충전량:" + String.format("%.2f", kevcsComm.getCostManager().getSumChargeKwh(KevcsCostManager.IDX_SUM_TOTAL)) +
                    ", 충전요금:" + (int)kevcsComm.getCostManager().getSumKwhCost());
        }
    }

    public void stopCharging() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);

        // 종료이벤트 발생
       // onFinishChargingEvent();
        // 종료될때까지 기다린다.
    }

    public void onSettingViewExit() {
        pageManger.hideSettingView();
    }

    // 1초에 한번씩 WatchDog 타이머를 수행한다.
    public void watchDogTimerProcess() {
        if ( cpConfig.useWatchDogTimer == false || enterResetMode) return;

        if ( watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
            if ( watchDogTimerStartCnt == TypeDefine.WATCHDOG_START_TIMEOUT) {
                // WatchDog 타이머 시작(open과 함께 시작)
                watchDogTimer.openAndStart(TypeDefine.WATCHDOG_TIMEOUT);
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

        //Fault 함수 수행
        onFaultEventProcess();

        getMeterValueProcess();

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        // 보드 버전 정보 저장
        dspVersion = rxData.version;
        kevcsComm.getChargerInfo().board_ver = String.format("0x%08X",rxData.version);

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();

            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            kevcsComm.setChargingTime(chargingTimeSec);

            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;

            kevcsComm.getCostManager().updateCurrentCostTable(); // 1초에 한번씩 단가 업데이트
            kevcsComm.setCarSoc(chargeData.soc);

            // 계량기 값의 차이를 계속 더한다.
            // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
            Calendar c = Calendar.getInstance();
            int curTimeSec = (c.get(Calendar.HOUR_OF_DAY)*3600)+(c.get(Calendar.MINUTE)*60) + c.get(Calendar.SECOND);
            int timeCost = curTimeSec/1800;

            // 5분주기로 데이터를 보내는 동작을 수행
            // 적산 전에 호출됨..(뒤에 호출될 경우 00:00, 00:30분 되는 시점에 계량기 증가시에 계산이 맞지 않음. 이전값까지 보내고 계산함)
            kevcsComm.doChargingInfoPeroidPerSecond(c.get(Calendar.MINUTE), timeCost);

            boolean isPayCalcNeed = true;

            // 비회원이고 요구량보다 큰경우 더이상 계상하지 않음
            if ( chargeData.isAuthCredit && (chargeData.measureWh >= chargeData.reqPayKwh)) {
                isPayCalcNeed = false;
                isCreditCardPrePayed = false;
            }

            // 데이터를 적산한다.
            if ( isPayCalcNeed == true ) {
                kevcsComm.getCostManager().processCostCalc((double) lastMeterValue / 1000.0, timeCost, chargeData.isAuthCredit, chargeData.isAuthCredit);
                chargeData.chargingCost = kevcsComm.getCostManager().getSumChargeCostVAT();
                chargeData.measureWh = kevcsComm.getCostManager().getSumChargeKwh(KevcsCostManager.IDX_SUM_TOTAL);
            }

            // 비회원인증인경우
            if ( chargeData.isAuthCredit ) {
                // 충전 요구량보다 같거나 큰경우 Stop시킨다.
                if ( chargeData.measureWh >= chargeData.reqPayKwh ) {
                    isCreditCardPrePayed = false;
                    stopCharging();
                }
            }

            // 계량기 값이 일정시간 이상 증가하지 않으면 stop한다.
            if ( lastMeterValue != oldLastMeterValue ) {
                oldLastMeterValue = lastMeterValue;
                meterNoChangeCnt = 0;
            }
            else {
                meterNoChangeCnt++;
                if ( meterNoChangeCnt >= TypeDefine.METER_NOCHANGE_TIMEOUT ) {
                    stopCharging();
                }
            }

            // 결제시 요구 충전량보다 크면 종료
            if ( chargeData.isAuthCredit ) {
                if ((double) (chargeData.measureWh / 1000.0) >= chargeData.reqPayCost) {
                    //isStopByPayReqPower = true; // 더이상 전력량을 적산하지 않음
                    chargeData.isAuthCredit = false; // 실결제와 선결제 취소가 필요가 없음
                    //선결제 취소 플래그
                    isCreditCardPrePayed = false;
                    stopCharging();
                }
            }

            // 주기적으로도 종료 검사함
            if ( rxData.get400Reg(DSPRxData2.STATUS400.FINISH_CHARGE) == true ) {
                isDspChargeFinish = true;
                onFinishChargingEvent();
            }
        }
        else {
            if ( !enterResetMode && resetFlag ) {
                startResetByRemote();
            }
            else if ( enterResetMode ){
                if ( resetCount-- == 0 ) {
                    rebootSystem();
                }
                else if (resetCount == 10) { // 10초 남으면 GlobInitEnd 전송
                    kevcsComm.sendGlobInitEnd_12();
                }
            }
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        isDspPlug = rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG);
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && isDspPlug == true ) {
            onCablePlugEvent(true);
        }

        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK && isDspChargeRun ) {
            onChargingStartEvent();
        }

        checkDoorOpen(rxData);

        checkUpdateFinish(); // 업데이트 완료 플레그 체크

        checkUIHomeTimeout(); // UI 화면 타이머 동작

        checkPlugOrgPosition(rxData);

        // 보드 버전 정보 저장
        kevcsComm.getChargerInfo().board_ver = String.format("0x%08X",rxData.version);

        // 6시간 이상 통신 단절시에 reboot
        if ( kevcsComm.getConnectStatus() == false ) {
            commNotConnectCnt++;
            if ( commNotConnectCnt > TypeDefine.COMM_NOT_CONNECT_RESET_TIMEOUT ) {
                if ( getUIFlowState() == UIFlowState.UI_START ) rebootSystem();
            }
        }
        else {
            commNotConnectCnt = 0;
        }

        //Log.d("flow Timer", TimeUtil.getCurrentTimeAsString("HH:mm:ss")) ;
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

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);
                dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);
                dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);     //단위 확인 필요

                //LogWrapper.d(TAG, "Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if ( meterVal > 0 ) lastMeterValue = meterVal;

            }catch(Exception e){
                LogWrapper.e(TAG, "Meter Err:"+e.toString());
            }
        }

    }
    void decreseUIHomeTimerAndGoHome() {
        if ( uiHomeTimerCnt > 0 ) {
            uiHomeTimerCnt--;
            if ( uiHomeTimerCnt == 0 ) {
                goStartProcess();
            }
        }
    }

    void checkUIHomeTimeout() {
        // 충전완료(에러)화면에서 언플러그시 첫화면 이동
        if (getUIFlowState() == UIFlowState.UI_FINISH_CHARGING || getUIFlowState() == UIFlowState.UI_FINISH_CHARGING_ERROR ) {
            if ( isDspPlug == false ) {
                decreseUIHomeTimerAndGoHome();
            }
        }
        else {
            decreseUIHomeTimerAndGoHome();
        }
    }

    void checkDoorOpen(DSPRxData2 rxData) {
        // Event에서 poll로 바꿈.
        isDspDoor = rxData.get400Reg(DSPRxData2.STATUS400.STATE_DOOR);
        if ( isDspDoor == false ) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
            doorOpenErrCnt = 0;

            if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
                if (chargeData.connectorType == TypeDefine.ConnectorType.CTYPE) {
                    if (isDoorOpenErrorCType) {
                        isDoorOpenErrorCType = false;
                        makeAndSendFault(6902, "커넥터 Lock 오류(CType)", isDoorOpenErrorCType, 90, 902);
                    }
                } else {
                    if (isDoorOpenError == true) {
                        isDoorOpenError = false;
                        makeAndSendFault(5902, "커넥터 Lock 오류", isDoorOpenError, 90, 902);
                    }
                }
            }
        }
        else if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ){
            // 커넥터 대기화면에서 도어가 Close가 되어 있으면 계속 연다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
            //LogWrapper.d(TAG, "Door Open Again");

            if (doorOpenErrCnt++ > TypeDefine.UI_DOOR_OPEN_ERROR_TIMEOUT) {
                if (chargeData.connectorType == TypeDefine.ConnectorType.CTYPE) {
                    if (isDoorOpenErrorCType == false) {
                        isDoorOpenErrorCType = true;
                        makeAndSendFault(6902, "커넥터 Lock 오류(CType)", isDoorOpenErrorCType, 90, 902);
                    }
                }
                else {
                    if (isDoorOpenError == false) {
                        isDoorOpenError = true;
                        makeAndSendFault(5902, "커넥터 Lock 오류", isDoorOpenError, 90, 902);
                    }
                }
            }
        }

    }

    void checkPlugOrgPosition(DSPRxData2 rxData) {

        // 원위치 플래그(폴링에서 검사)
        isPlugOrgPosition = !rxData.get400Reg(DSPRxData2.STATUS400.CONNECTOR_POS_COMBO_BC);

        // UI START에서만 검사함
        if ( getUIFlowState() == UIFlowState.UI_START ) {
            if (isPlugOrgPosition == false) {
                isPlugOrgPositionErrorCnt++;

                if ((isPlugOrgPositionErrorCnt % TypeDefine.UI_PLUG_ORG_POS_ALARM_PERIOD) == 0) {
                    if (plugOrgPositionAlarmCnt < TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX) {
                        if (chargeData.connectorType != TypeDefine.ConnectorType.BTYPE) { // B타입은 사운드 Play안함
                            soundManager.playSound(SoundManager.SoundKind.ConnectorOrg);
                        }
                        plugOrgPositionAlarmCnt++;
                    }
                }

                if (isPlugOrgPositionError == false && (isPlugOrgPositionErrorCnt > TypeDefine.UI_PLUG_ORG_POS_TIMEOUT)) {
                    isPlugOrgPositionErrorCnt = 0;
                    isPlugOrgPositionError = true;
                    plugOrgPositionAlarmCnt = TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX;

                    // Send Error
                    makeAndSendFault(5903, "커넥터 위치센서 오류", isPlugOrgPositionError, 90, 903);
                }
            } else if (isPlugOrgPosition == true && isPlugOrgPositionError == true) {
                isPlugOrgPositionErrorCnt = 0;
                isPlugOrgPositionError = false;

                //알람 카운트 리셋 안함(한 충전 시퀀스에서 한번만 발생함)
                plugOrgPositionAlarmCnt = TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX;

                // Repair Error
                makeAndSendFault(5903, "커넥터 위치센서 오류", isPlugOrgPositionError, 90, 903);
            } else if (isPlugOrgPosition == true) {
                isPlugOrgPositionErrorCnt = 0;
                // 한번이라도 발생이 되면 알람을 멈춘다.
                plugOrgPositionAlarmCnt = TypeDefine.UI_PLUG_ORG_POS_ALARM_CNT_MAX;
            }
        }
    }

    void makeAndSendFault(int faultId, String msg, boolean isError, int tp, int cd) {
        FaultInfo fInfo = new FaultInfo(faultId, msg, !isError, tp, cd);

        boolean isFaultListChanged = false;
        boolean isContain = false;

        Iterator<FaultInfo> iter = faultList.iterator();

        while (iter.hasNext()) {
            FaultInfo fInfoCur = iter.next();
            if ( fInfoCur.id == fInfo.id ) {
                isContain = true;
                fInfoCur.isRepair = fInfo.isRepair;
                if ( fInfoCur.isRepair == true ) {
                    kevcsComm.sendGlobFaultEvent_16(fInfoCur, FaultInfo.FAULT_REPAIR);
                    iter.remove();
                    isFaultListChanged = true;
                }
            }
        }

        // 새로운 이벤트인경우
        if ( isContain == false && isError) {
            faultList.add(fInfo);

            // 신규 이벤트들은 통신으로 전송함.
            if ( !fInfo.isRepair ) {
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR);
            }
            isFaultListChanged = true;
        }

        if ( isFaultListChanged ) {
            faultManager.saveFaultStatus(Environment.getExternalStorageDirectory().toString() + TypeDefine.REPOSITORY_BASE_PATH, TypeDefine.FAULT_INFO_FILENAME, faultList);
        }
    }

    void rebootSystem() {
        try {
            PowerManager pm = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
            pm.reboot("force");
        } catch (Exception e ) {

        }
    }

    void startResetByRemote() {
        // 리셋 카운트 시작
        resetCount = 30;
        enterResetMode = true; // 만약 soft reset이 동작안되면 5분뒤에 watchdog reset이 작동함.
        resetCtl.st_datetime = TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss");
        resetCtl.state = KevcsChargeCtl.State.STARTED;
        kevcsComm.sendGlobChargeCtlBegin_15(resetCtl);
        kevcsComm.payTerminalResetReq();

        resetCtl.saveToFile(Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH, TypeDefine.RESET_INFO_FILENAME);

        showMessageBox("재부팅중입니다. 잠시만 기다려 주세요.", 60, false, false);
    }

    public void stopWatdogTimer() {
        watchDogTimer.stop();
        watchDogTimer.close();
    }

    void checkUpdateFinish() {
        if (updateFinishFlag && getUIFlowState() == UIFlowState.UI_START) {
            try {
                String updatePath = Environment.getExternalStorageDirectory()+TypeDefine.KEVCS_COMM_PATH;

                ZipUtils.unzip(updatePath+"/"+KevcsComm.FILE_UPDATE_FIRMWARE, updatePath, false);
                // 성공시 처리
                LogWrapper.v(TAG, "Firmware Unzip Successed");

                RemoteUpdater updater = new RemoteUpdater(mainActivity, updatePath, KevcsComm.FILE_UPDATE_FIRMWARE_APK);
                updater.doUpdateFromApk("com.joas.smartcharger");

            } catch(Exception e) {
                LogWrapper.v(TAG, "Firmware Unzip Failed");
            }
            updateFinishFlag = false;
        }
    }

    public void setPowerControl(int power) {
        dspControl.setPowerlimit(chargeData.dspChannel, power);
        LogWrapper.d(TAG, "SetPowerLimit:"+power+"%");
    }

    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
        initConfigValue();

        statusBarControl.setUsePayTeminal(cpConfig.usePayTerminal);

        // 현재 연결되어 있다면 연결을 끊고 새로운 세팅으로 재접속을 한다.
        kevcsComm.disconnect();

        soundManager.setVolume(((float)cpConfig.soundVol)/10.0f);
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
                    isDspReady = val;
                    break;

                case AVAL_CHARGE:
                    isDspAvalCharge = val;
                    break;

                case STATE_PLUG:
                    isDspPlug = val;
                    if ( val == false ) onCablePlugEvent(val);
                    break;

                case STATE_DOOR:
                    isDspDoor = val;
                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if ( val == true ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
                        onChargingStartEvent();
                    }
                    break;

                case FINISH_CHARGE:
                    isDspChargeFinish = val;
                    if ( val == true ) onFinishChargingEvent();
                    break;

                case FAULT:
                    onFaultEventProcess();
                    break;

                case STATE_RESET:
                    break;

                case CONNECTOR_LOCK_A:
                    break;
            }
        }
    }

    void onCablePlugEvent(boolean tf) {
        // Cable Plug In
        if (tf == true) {
            if (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT) {
                setUIFlowState(UIFlowState.UI_RUN_CHECK);
                pageManger.changePage(PageID.CONNECT_CAR_WAIT);
            }
            kevcsComm.setCPStat(KevcsProtocol.CPStat.CONNECT);
        }
        else {
            kevcsComm.setCPStat(KevcsProtocol.CPStat.SEPERATE_CONNECTOR);
            recordSystemLog("(언플러그) 카드번호:"+kevcsComm.getChargerInfo().member_card_no);
        }
    }

    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false && fInfo.id != 40603 ) { // 외함오픈인 경우에 제외
                chargeData.faultBoxContent += "[" + fInfo.cd + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";
            }
        }
    }

    public synchronized void onFaultEventProcess() {
        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        isDspFault =  rxData.get400Reg(DSPRxData2.STATUS400.FAULT);

        Vector<FaultInfo> fList = faultManager.sacnFault(chargeData.dspChannel);
        boolean isEmergency = faultManager.isFaultEmergency(chargeData.dspChannel);

        if ( fList.size() > 0 ) {
            boolean isFaultListChanged = false;
            boolean isContain = false;
            for (FaultInfo fInfo : fList) {
                Iterator<FaultInfo> iter = faultList.iterator();

                while (iter.hasNext()) {
                    FaultInfo fInfoCur = iter.next();
                    if ( fInfoCur.id == fInfo.id ) {
                        isContain = true;
                        fInfoCur.isRepair = fInfo.isRepair;
                        if ( fInfoCur.isRepair == true ) {
                            kevcsComm.sendGlobFaultEvent_16(fInfoCur, FaultInfo.FAULT_REPAIR);
                            iter.remove();
                            isFaultListChanged = true;
                        }
                    }
                }

                // 새로운 이벤트인경우
                if ( isContain == false ) {
                    FaultInfo newInfo = new FaultInfo(fInfo.id, fInfo.errorMsg, fInfo.isRepair, fInfo.tp, fInfo.cd);
                    faultList.add(newInfo);

                    // 신규 이벤트들은 통신으로 전송함.
                    if ( !fInfo.isRepair ) {
                        kevcsComm.sendGlobFaultEvent_16(newInfo, FaultInfo.FAULT_OCCUR);
                    }
                    isFaultListChanged = true;
                }

                faultStatusInit(fInfo, true);
            }
            if ( isFaultListChanged ) {
                faultManager.saveFaultStatus(Environment.getExternalStorageDirectory() + TypeDefine.REPOSITORY_BASE_PATH, TypeDefine.FAULT_INFO_FILENAME, faultList);
            }

        }

        if ( isPreDspFault != isDspFault ) {
            if ( isDspFault == true ) {
                fillFaultMessage();

                // 충전충이라면 충전을 중지한다.
                if ( getUIFlowState() == UIFlowState.UI_CHARGING || getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
                    kevcsComm.getChargerInfo().charge_end_stat = KevcsProtocol.CHARGE_END_FAULT;
                    stopCharging();
                    setUIFlowState(UIFlowState.UI_FINISH_CHARGING_ERROR);
                    pageManger.changePage(PageID.FINISH_CHARGING_ERRROR);
                    kevcsComm.finishCharge();
                    pageManger.hideChargingStopAskView();

                    recordSystemLog("(충전중에러종료) 카드번호:"+kevcsComm.getChargerInfo().member_card_no +
                            ", 계량기값:" + lastMeterValue +
                            ", 충전시간:" + kevcsComm.getChargerInfo().charge_accum_time +
                            ", 충전량:" + String.format("%.2f", kevcsComm.getCostManager().getSumChargeKwh(KevcsCostManager.IDX_SUM_TOTAL)) +
                            ", 충전요금:" + (int)kevcsComm.getCostManager().getSumKwhCost() +
                            ", 에러내용:" + chargeData.faultBoxContent );
                }
            }
            isPreDspFault = isDspFault;
        }

        // FaultBox 검사
        if ( getUIFlowState() == UIFlowState.UI_START) {
            if ( isDspFault == true ) {
                if ( !pageManger.isShowFaultBox()) pageManger.showFaultBox();
            }
            else {
                if ( pageManger.isShowFaultBox()) pageManger.hideFaultBox();
            }
        }

        // 긴급버턴 이벤트 발생
        if ( isEmergencyPressed != isEmergency ) {
            if (isEmergency == true) {
                pageManger.showEmergencyBox();
                soundManager.playSound(SoundManager.SoundKind.Emergency);
            } else { // 긴급 버턴 해제
                pageManger.hideEmergencyBox();
            }
            isEmergencyPressed = isEmergency;
        }
    }

    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if ( getUIFlowState() == UIFlowState.UI_AUTH_MEMBER_CARD ) {
            String firstNum = rfid.substring(0,1);
            if (firstNum.equals("1") == false && firstNum.equals("2") == false ) {
                showMessageBox("사용자 카드가 아닙니다.", 20, true, true);
                setUIFlowState(UIFlowState.UI_START, false);
                pageManger.changePage(PageID.START);
                return;
            }

            kevcsComm.memberAuthReq(rfid, KevcsProtocol.CERT_TP_RFCARD, "");

            setUIFlowState(UIFlowState.UI_AUTH_WAIT_CARD);
            pageManger.changePage(PageID.AUTH_PAY_WAIT);
        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {

        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {
        makeAndSendFault(5015, "UI-제어보드간 통신 오류", isError, 50, 501);

        statusBarControl.setDSPCommError(isError);
    }

    //=========================================================
    // 통신 모듈 이벤트 처리
    //=========================================================
    @Override
    public void onTimeUpdate(String strTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date syncTime = null;

        try {
            syncTime = format.parse(strTime);
        } catch (ParseException e) {
            LogWrapper.e(TAG, "onTimeUpdate err: "+e.toString()+", strTime:"+strTime);
            return;
        }

        Date curTime = new Date();
        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);

        // 현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTime() - syncTime.getTime()) > TypeDefine.TIME_SYNC_GAP_MS) {
            am.setTime(syncTime.getTime());
            LogWrapper.v(TAG, "TimeSync : "+syncTime.toString());
        }
    }

    @Override
    public void onOnlineModeAndPwdUpdate(String password) {
        if ( !cpConfig.settingPassword.equals(password) ) {
            cpConfig.settingPassword = password;
            cpConfig.saveConfig(mainActivity);
        }
        statusBarControl.setServerCommError(false);
    }

    @Override
    public void onCommLocalMode(boolean tf) {
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
    }

    @Override
    public void onKevcsCommConnected() {
        statusBarControl.setServerCommError(false);
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
    }

    /**
     * 처음 접속 성공시에 불려짐
     */
    @Override
    public void onKevcsCommConnectedFirst() {
        // 리셋 명령 ACK 처리
        loadResetInfo();
        statusBarControl.setServerCommError(false);
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태

        // DSP 에러 초기화
        if ( !dspControl.getDspCommError() ) onDspCommErrorStstus(false);
    }

    @Override
    public void onKevcsCommDisconnected() {
        statusBarControl.setServerCommError(true);
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, false);     //서버통신상태
    }

    @Override
    public void onKevcsCommAuthResult(boolean success) {
        onAuthResult(success);
    }

    @Override
    public void onKevcsReadCardNum(String cardNum) {
        //신용카드 테깅
        if ( chargeData.isAuthCredit ) {
            if (getUIFlowState() == UIFlowState.UI_CREDIT_CARD_PAY) {
                kevcsComm.getChargerInfo().member_card_no = cardNum;
                // 한전 프로토콜 지원 단말기인 경우 안테나 On을 보낸다.
                if ( payTerminalKind == KevcsComm.PayTerminalKind.STANDARD) {
                    // 결제 요청
                    kevcsComm.payRequest(KevcsProtocol.APRV_TYPE_PREPAY, chargeData.reqPayKwh, chargeData.reqPayCost);
                }
                else {
                    // 테크리더인 경우에는 결제 요청이후에 자동으로 결제진행이 된다. 위 과정이 필요 없음.
                }
                setUIFlowState(UIFlowState.UI_PREPAY_WAIT);
                pageManger.changePage(PageID.AUTH_PAY_WAIT);
            }
        }
        else {
            onRfidDataReceive(cardNum, true);
            kevcsComm.payTerminalAntReq(false);
        }
    }

    @Override
    public void onPayResult(KevcsChargerInfo.KevcsPayRetInfo payRetInfo) {
        if (getUIFlowState() == UIFlowState.UI_PREPAY_WAIT) {
            if ( payRetInfo.pay_stat.equals(KevcsProtocol.PAY_STAT_SUCCESS)) {

                // 선결제 취소 플래그 세팅 swpark 201121
                //chargeData.isAuthCredit = true;
                isCreditCardPrePayed = true;

                // 카드 결제가 잘 되면 커넥터 연결 화면으로 이동함
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);

                if ( isDspPlug ) onCablePlugEvent(true);
                else pageManger.changePage(PageID.CONNECTOR_WAIT);
            }
            else {
                FaultInfo fInfo = new FaultInfo(5503, "신용카드 승인실패로 인한 충전불가", false, 50, 503);
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR_END);

                //showMessageBox("신용카드 결제승인이 실패하였습니다.\n["+payRetInfo.pay_ret_msg1+" "+payRetInfo.pay_ret_msg2+"]", 20, true, true);
                showMessageBox("신용카드 결제승인이 실패하였습니다.", 20, true, true);
                setUIFlowState(UIFlowState.UI_START, false);
                pageManger.changePage(PageID.START);
            }
        }
        //승승취 결제 시퀀스 swpark 201122
        else if (getUIFlowState() == UIFlowState.UI_CREDIT_REAL_PAY_WAIT) {

            if (payRetInfo.pay_stat.equals(KevcsProtocol.PAY_STAT_SUCCESS)) {
                isCreditCardPrePayed = true;
                chargeData.chargingCost = 0;
                goStartProcess();   //선결제 취소를 위해 goStartProcess()로 이동 swpark 201122
            } else {
                FaultInfo fInfo = new FaultInfo(5503, "신용카드 승인실패로 인한 실결제 불가", false, 50, 503);
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR_END);

                //showMessageBox("신용카드 결제승인이 실패하였습니다.\n["+payRetInfo.pay_ret_msg1+" "+payRetInfo.pay_ret_msg2+"]", 20, true, true);
                showMessageBox("실결제승인이 실패하였습니다.", 20, true, true);
                kevcsComm.payRealRequest(KevcsProtocol.APRV_TYPE_REALPAY, chargeData.reqPayKwh, (int)chargeData.chargingCost);
                setUIFlowState(UIFlowState.UI_CREDIT_REAL_PAY);
                pageManger.changePage(PageID.CREDIT_REAL_PAY);
            }
        }
        else  if (getUIFlowState() == UIFlowState.UI_CREDIT_CANCEL_PREPAY) {    //승승취 시 작동 (카드가 이미 인입되어 있는 상태)  swpark 201122

            if (payRetInfo.pay_stat.equals(KevcsProtocol.PAY_STAT_SUCCESS)) {

                // 선결제 취소 완료 플래그 세팅 swpark 201121
                isCreditCardPrePayed = false;
                // 결제 취소 완료 후 언플러그로 이동
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManger.changePage(PageID.UNPLUG);
            } else {
                FaultInfo fInfo = new FaultInfo(5503, "신용카드 취소실패로 인한 실결제 불가", false, 50, 503);
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR_END);
                //showMessageBox("신용카드 결제승인이 실패하였습니다.\n["+payRetInfo.pay_ret_msg1+" "+payRetInfo.pay_ret_msg2+"]", 20, true, true);
                showMessageBox("취소 승인에 실패 하였습니다.", 20, true, true);
                goStartProcess();
            }
        }
        else  if (getUIFlowState() == UIFlowState.UI_CREDIT_CANCEL_PREPAY_WAIT) {   //승취 시 작동 (카드를 인입할때 상태) swpark 201122

            if (payRetInfo.pay_stat.equals(KevcsProtocol.PAY_STAT_SUCCESS)) {

                // 선결제 취소 완료 플래그 세팅 swpark 201121
                isCreditCardPrePayed = false;
                // 결제 취소 완료 후 언플러그로 이동
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManger.changePage(PageID.UNPLUG);
            } else {
                FaultInfo fInfo = new FaultInfo(5503, "신용카드 취소실패로 인한 실결제 불가", false, 50, 503);
                kevcsComm.sendGlobFaultEvent_16(fInfo, FaultInfo.FAULT_OCCUR_END);
                //showMessageBox("신용카드 결제승인이 실패하였습니다.\n["+payRetInfo.pay_ret_msg1+" "+payRetInfo.pay_ret_msg2+"]", 20, true, true);
                showMessageBox("취소 승인에 실패 하였습니다.", 20, true, true);
                goStartProcess();
            }
        }

    }

    @Override
    public void onKevcsTL3500CardEvent(String event) {
        LogWrapper.d(TAG, "onKepcoTL3500CardEvent:"+event);

        if (getUIFlowState() == UIFlowState.UI_CREDIT_CARD_PAY) {
            // 카드 입력, RF, 삼성 pay(MS 카드) 이벤트 발생시
            if ( event.equals("I") || event.equals("M") || event.equals("R")) {
                setUIFlowState(UIFlowState.UI_PREPAY_WAIT);
                pageManger.changePage(PageID.AUTH_PAY_WAIT);

            }
        }
        else if (getUIFlowState() == UIFlowState.UI_PREPAY_WAIT) {
            if ( event.equals("O") ) {
                showMessageBox("신용카드 결제승인이 실패하였습니다.", 6, true, true);
                setUIFlowState(UIFlowState.UI_START, false);
                pageManger.changePage(PageID.START);
                // 초기화
                kevcsComm.payTerminalAntReq(false);
            }
        }
        else if (getUIFlowState() == UIFlowState.UI_CREDIT_REAL_PAY) {
            // 카드 입력, RF, 삼성 pay(MS 카드) 이벤트 발생시
            if ( event.equals("I") || event.equals("M") || event.equals("R")) {
                setUIFlowState(UIFlowState.UI_CREDIT_REAL_PAY_WAIT);
                pageManger.changePage(PageID.AUTH_PAY_WAIT);

            }
        }
        else if (getUIFlowState() == UIFlowState.UI_CREDIT_REAL_PAY_WAIT) {
            if ( event.equals("O") ) {
                showMessageBox("신용카드 결제승인이 실패하였습니다.", 6, true, true);
                //goStartProcess();
                kevcsComm.payTerminalAntReq(false);
            }
        }
        else if (getUIFlowState() == UIFlowState.UI_CREDIT_CANCEL_PREPAY) {
            // 카드 입력, RF, 삼성 pay(MS 카드) 이벤트 발생시
            if ( event.equals("I") || event.equals("M") || event.equals("R")) {
                setUIFlowState(UIFlowState.UI_CREDIT_CANCEL_PREPAY_WAIT);
                pageManger.changePage(PageID.AUTH_PAY_WAIT);

            }
        }
        else if (getUIFlowState() == UIFlowState.UI_CREDIT_CANCEL_PREPAY_WAIT) {
            if ( event.equals("O") ) {
                showMessageBox("신용카드 결제승인이 실패하였습니다.", 6, true, true);
                //goStartProcess();
                kevcsComm.payTerminalAntReq(false);
            }
        }

    }
    @Override
    public void onKevcsCommLogEvent(String tag, String logData) {} // Empty 어떻게 할지 고민 201107 swpark

    @Override
    public void onChargeCtl(KevcsChargeCtl chargeCtl) {
        // 리셋인 경우
        if ( chargeCtl.ctrl_cd.matches("3|5|9")) {
            resetCtl = chargeCtl;
            resetFlag = true; // 리셋 플레그 설정

            //즉시 리셋인 경우
            if ( chargeCtl.ctrl_type.equals("1") ) {
                // 충전중이면 충전 중지를 한다.
                if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                    stopCharging();
                }

                startResetByRemote();
            }
        }
        else {
            // 그외에는 처리안함.
            chargeCtl.state = KevcsChargeCtl.State.FINISHED;
        }

    }

    @Override
    public boolean onRemoteAuthReq(String connType) {
        if ( isDspFault || flowState != UIFlowState.UI_START) return false;

        PageEvent event = PageEvent.SELECT_BTYPE_CLICK;
        switch (connType) {
            case "1": event = PageEvent.SELECT_CTYPE_CLICK;
                break;
            case "2": event = PageEvent.SELECT_BTYPE_CLICK;
                break;
            case "3": event = PageEvent.SELECT_CHADEMO_CLICK;
                break;
            case "4": event = PageEvent.SELECT_AC3_CLICK;
                break;
            case "5": event = PageEvent.SELECT_DCCOMBO_CLICK;
                break;
        }
        doCableSelect(event);

        // 이미 Plug가 꽂혀 있는 상태라면 바로 차량 연결 화면으로 전환한다.
        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
        if ( isDspPlug ) onCablePlugEvent(true);
        else pageManger.changePage(PageID.CONNECTOR_WAIT);

        kevcsComm.setCPStat(KevcsProtocol.CPStat.START);

        recordSystemLog("(App 인증 완료) 카드번호:"+kevcsComm.getChargerInfo().member_card_no);
        return true;
    }

    @Override
    public boolean onRemoteChargingStopReq() {
        return false;
    }

    @Override
    public void onPaymStatErr(boolean isError) {
        statusBarControl.setPayTerminalStatusError(isError);

        if ( isPayTeminalError != isError ) {
            isPayTeminalError = isError;
            makeAndSendFault(5905, "결제모듈 오류", isError, 90, 905);
        }
    }

    /**
     * 업데이트 종료 이벤트(리부팅 필요)
     */
    @Override
    public void onFinishUpdateDownload() {
        updateFinishFlag = true;
    }

    @Override
    public void onQRImageDownFinish(String qrImgFile) {
        pageManger.getStartView().onQRImageDownUpdate(qrImgFile);
    }

    // Test Code
    public void lastPaymCancel() {
        kevcsComm.getTl3500s().cancelLastPay(0);
    }
}
