/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.view.View;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTXData2ExtInterface;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.gridwiz_slow_charlcd.page.PageEvent;
import com.joas.gridwiz_slow_charlcd.page.PageID;
import com.joas.metercertviewer.IMeterAidlInterface;
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;


public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, LogWrapperListener, DSPTXData2ExtInterface {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_NO_SERVICE
    }

    PageManger pageManger;

    GridwizSlowCharLCDUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_CARD_TAG;
    ChargeData chargeData;
    CPConfig cpConfig;


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
    int lastDateValue = 0;

    int meterTimerCnt = 0;

    boolean isHardResetEvent = false;
    boolean isConnectorOperative = true;

    FaultManagerV2 faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    //RFID Reader
    RfidReader rfidReader;

    int firmwareInstallCounter = 0;
    double powerLimit = -1;

    int unplugTimerCnt = 0;

    private IMeterAidlInterface meterService;

    GridwizEvChargerInfo gridwizEvChargerInfo;

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
    long backupMeterval = 0;
    boolean metererr_reset_flag = false;
    boolean meter_isRfActivate = false;

    public UIFlowManager(GridwizSlowCharLCDUIActivity activity, ChargeData data, CPConfig config, String restartReason) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        LogWrapper.setLogWrapperListener(this);

        gridwizEvChargerInfo = new GridwizEvChargerInfo();
        gridwizEvChargerInfo.loadCostInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+TypeDefine.COST_INFO_FILENAME);

//        dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE,1, this);
        dspControl = new DSPControl2(1, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, 40,1, this); // 사용자 지정 프로토콜 사용시(크기 지정, ex> tx size = 30->40)

        dspControl.setMeterType(DSPControl2.METER_TYPE_SLOW);
        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);          //미터를 전력량계 통신 프로그램에서 가져온다 (false로 지정해야함)

        dspControl.setDSPTXDataExtInterface(this::dspTxDataEncodeExt);      //add by si. 200818 DSP2 TX rewrite

        dspControl.start();

//        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
        rfidReader.setRfidReaderEvent(this);

        String basePath = Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH;

        // FaultManager를 생성한다.
        faultManager = new FaultManagerV2(dspControl, mainActivity, chargeData.dspChannel);

        // 1초 타이머 시작
        startPeroidTimerSec();
        initStartState();
        rfidReader.rfidReadRequest();


        if ( checkCpModeOperateStop() )
            onOperatingStopCharLCD();
        else
            if(!dspPLCStat) dispMeteringString(new String[]{"Welcome! Gridwiz", "Tag Card for EV"});
            else dispMeteringString(new String[]{"PLC Disconnected"});

//        initServerData_h1();
    }


    /**
     * 충전기 설치 데이터 초기화 ( 설정파일 저장값 불러오기 )
     *
     * by Lee 20200528
     */
    private void initServerData_h1()
    {
        //설치 정보 가져오기
        gridwizEvChargerInfo.serverIp = cpConfig.serverIP;
        gridwizEvChargerInfo.portNumber = String.valueOf(cpConfig.serverPort);
        gridwizEvChargerInfo.stationId = cpConfig.stationID;
        gridwizEvChargerInfo.chargerId = cpConfig.chargerID;
        gridwizEvChargerInfo.gpsLocInfo = cpConfig.gps_X + "," + cpConfig.gps_Y;
    }

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();
        rfidReader.stopThread();
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }

   public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }

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
        dispMeteringString(listStr, true);
    }

    public void dispMeteringString(String[] listStr, boolean isBacklightOn) {
        lastMeterStringList = listStr;

        if ( dispMeteringStringTempLock ) return;

        if ( listStr == null ) return;
        try {
            IMeterAidlInterface meterService = mainActivity.getMeterService();

            if ( isBacklightOn ) {
                meterService.setCharLCDBacklight(true);
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
        dispMeteringString(lastMeterStringList);
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
            case UI_CARD_TAG:
                initStartState();
                break;

            case UI_CONNECTOR_WAIT:
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음

                //dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, true);

                //캐릭터 LCD 용
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE )
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);

                break;
            case UI_RUN_CHECK:
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                boolean tflag = dspControl.getState200(chargeData.dspChannel,DSPTxData2.STATUS200.START_CHARGE);
                LogWrapper.v(TAG, "DSP Set Status:"+ "START_CHARGE"+" is "+tflag);
                break;

            case UI_FINISH_CHARGING:
                //dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);

                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);

                unplugTimerCnt = 0;
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
    }

    public void onPageStartEvent(boolean checkDspComm) {

        gridwizEvChargerInfo.cardNum = "0000000000000000";      //카드번호 초기화 - add by si. 200821
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.RF_TAG_EVENT, false);     //RF카드 태깅 이벤트 전달(EIM) - add by si.200821
        setUIFlowState(UIFlowState.UI_CARD_TAG);
        pageManger.changePage(PageID.CARD_TAG);

        onReadyCharLCD(checkDspComm);
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

        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.MANAGE_STOP, false);     //무료모드

        // Session Start

        // Next Flow. Card Tag
        if(cpConfig.isAuthSkip)
        {
            gridwizEvChargerInfo.cardNum = "0000000000009999";       //자동인증 시 회원번호

            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, true);     //무료모드

            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            doAuthComplete();
        }else {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, false);     //유료모드

            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManger.changePage(PageID.CARD_TAG);

            rfidReader.rfidReadRequest();
        }
    }

    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                onPageStartEvent(false);
                break;
        }
    }

    boolean tagCardChargingStop = false;        //확인용 태그
    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.RF_TAG_EVENT, true);     //RF카드 태깅 이벤트 전달(EIM) - add by si.200821

            if ( flowState == UIFlowState.UI_CARD_TAG ) {
                    lastCardNum = tagNum;

                    gridwizEvChargerInfo.cardNum = tagNum;

                    // 승인대기 화면 전환
                    setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                    pageManger.changePage(PageID.AUTH_WAIT);

                    onCardVerifingCharLCD();
            }
//            else if ( flowState == UIFlowState.UI_CHARGING ) {
//                if ( tagNum.equals(lastCardNum) == true ) {
//
//                    tagCardChargingStop = true;     //카드태그에 의한 충전 중지 플래그 체크
//
//                    LogWrapper.v(TAG, "Stop by User Card Tag");
//                    onChargingStop();       //card tag
//                    dispMeteringString(new String[] {"Stoping...", "Wait a second"});
//                }
//                else {
//                    /*
//                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
//                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
//                    pageManger.showMessageBox();
//                    */
//
//                    dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
//                }
//            }
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
                        onPageStartEvent(false);
                    }
                }, timeout);
            }
        });
    }

    public void onAuthResultEvent(boolean isSuccess) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (isSuccess) {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);

                doAuthComplete();

            } else {
                /* Charlcd 필요없음
                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManger.getAuthWaitView().stopTimer();
                pageManger.showMessageBox();
                */

                goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
                dispTempMeteringString(new String[] {"Card Failed", "Check Card Info"}, 6000);

            }
        }
    }

    void doAuthComplete() {
        LogWrapper.v(TAG, "Auth Complete");

        onCardCheckDoneCharLCD();

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
            // 이미 Connect 된 상태이라면
            onConnectedCableEvent(true);
        } else {
            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
            pageManger.changePage(PageID.CONNECTOR_WAIT);
            // Ctype인 경우에는 도어를 오픈할 필요가 없음
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);

        }
    }

    public void setMemberCostUnit(boolean isMember) {
        int slot = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        // 회원 단가 적용
        if (isMember)
            chargeData.chargingUnitCost = gridwizEvChargerInfo.memberCostUnit[slot];
        // 비회원 단가 적용
        else
            chargeData.chargingUnitCost = gridwizEvChargerInfo.nonMemberCostUnit[slot];

        gridwizEvChargerInfo.curChargingCostUnit = chargeData.chargingUnitCost;
        LogWrapper.d(TAG, "setMemberCostUnit:"+ gridwizEvChargerInfo.curChargingCostUnit);
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

        tagCardChargingStop = false;

        // 통신 충전 메시지 초기화
        gridwizEvChargerInfo.reqAmoundSel = 0x01; // Full
        gridwizEvChargerInfo.payMethod = 0x02; // 전기요금 합산

        // 단가 설정
        if (gridwizEvChargerInfo.cardNum.equals("0000000000000000"))
            setMemberCostUnit(false);       //비회원
        else
            setMemberCostUnit(true);        //회원

        // 바로 Char 디스플레이 카운트 초기화
        dispChargingMeterStrCnt = TypeDefine.DISP_CHARGING_CHARLCD_PERIOD;
        dispChargingMeterBacklightCnt = 0; // 백라이트 카운터 초기화

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        gridwizEvChargerInfo.socBatt = rxData.batterySOC*100;      //소수점 2자리까지 표시 (프로토콜상) 12.34 -> 1234

    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작 시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        DSPRxData2 rxData2 = dspControl.getDspRxData2(chargeData.dspChannel);

        // 기존 충전 시퀀스이거나 테스트모드 시 run 신호가 올라왔을 경우, 충전 진행
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK || rxData2.get407Reg(DSPRxData2.STATUS407.TEST_MODE)) {
            LogWrapper.v(TAG, "Start Charging");
            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            java.sql.Date date = new java.sql.Date(new java.util.Date().getTime());

            gridwizEvChargerInfo.chargingStartTime = date;



            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);


//            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        dispMeteringString(new String[] {"Connecting to EV", "Wait a second"});
        if ( isConnected ) {
            if (cpConfig.isAuthSkip)
            {
                gridwizEvChargerInfo.cardNum = "0000000000009999";       //자동인증 시 회원번호

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
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            else if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
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

    /**
     * 커넥터 도어 Open/Close 알람 자료 전송
     *
     * by Lee 20200603
     * @param bIsOpen
     */
    public void onChangedConnectorStatus(boolean bIsOpen)
    {
    }

    public synchronized void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

            //충전완료상태 데이터 셋
            if (isDspFault) {
                Vector<FaultInfo> fList = faultManager.scanFaultV2(chargeData.dspChannel);
                boolean isEmergency = faultManager.isFaultEmergency(chargeData.dspChannel);
                if (isEmergency)
                    gridwizEvChargerInfo.chargingFinishStatus = GridwizEvChargerInfo.CHARGING_END_PRESS_EMERGENCY;    // 비상정지 버튼에 의한 충전완료
                else
                    gridwizEvChargerInfo.chargingFinishStatus = GridwizEvChargerInfo.CHARGING_END_CHARGING_ERR;       // 충전기 오류에 의한 충전완료
            }
//            else if (!tagCardChargingStop) {
//                jejuEvChargerInfo.chargingFinishStatus = GridwizEvChargerInfo.CHARGING_END_CAR_STOP;               // 차량에서 충전 중지 요청 (급속)
//            }
            else {
                gridwizEvChargerInfo.chargingFinishStatus = GridwizEvChargerInfo.CHARGING_END_SUCCESS;                // 정상 충전완료
            }


            if ( !checkCpModeOperateStop() )
            {
                DSPRxData2 rxData2 = dspControl.getDspRxData2(chargeData.dspChannel);

                if (rxData2.get407Reg(DSPRxData2.STATUS407.TEST_MODE))
                    gridwizEvChargerInfo.cpModeDspTestMode();
                else
                    gridwizEvChargerInfo.initCpModeReady();     // Finish Charging
            }



            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManger.changePage(PageID.FINISH_CHARGING);

            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

            //Fault가 아닐 시, 정상 충전 완료
            if (!isDspFault) {
                onFinishedCharLCD();

                LogWrapper.v(TAG, "Finish Charging:" + String.format("Usage: %.2fkWh", ((double) gridwizEvChargerInfo.curChargingKwh) / 100.0) +
                        ", " + String.format("Cost: %dWon", gridwizEvChargerInfo.curChargingCost));
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
     * 업데이트 체크한다. isFWUpdateReady 가 true이고 UI가 초기화면인 경우에 업데이트 및 리셋 실시
     */
    void checkUpdateReady() {
        if ( isFWUpdateReady == true ) {
            if ( getUIFlowState() == UIFlowState.UI_CARD_TAG ) {
                doInstallFirmware();
            }
        }
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수행한다.
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

        //RF 오토스캔 start/stop 모니터링
        onRFMonitoring();

//        // 예약단가 파일 적용 (있을 시)
//        reservationUnitCost();

        //전력량계 값 가져오기
        // meterVal = 전력량계
        //meter volt
        if ( mainActivity.getMeterService() != null ) {
            try {
                long meterVal = mainActivity.getMeterService().readMeter(); //w 단위
                double meterVolt = mainActivity.getMeterService().readMeterVoltage();
                double meterCurrent = mainActivity.getMeterService().readMeterCurrent();

                try {
                    gridwizEvChargerInfo.meterVal = (int) (meterVal / 10); //kwh 단위 * 2 ( 서버 전송 데이터 )
                }
                catch (Exception e1)
                {}

                MeterStatusMonitoring(meterVal);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);
                dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);
//                dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);     //단위 확인 필요

                //LogWrapper.d(TAG, "Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if ( meterVal > 0 ) {
                    dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);     //단위 확인 필요
                    if (getUIFlowState() == UIFlowState.UI_CHARGING){
                        if (lastMeterValue > 0) {
                            int gapMeter = (int) (meterVal - lastMeterValue);

                            if (gapMeter > 0) {
                                chargeData.measureWh += (meterVal - lastMeterValue);
                                chargeData.chargingCost += ((double) gapMeter / 1000.0) * (double) chargeData.chargingUnitCost;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                }
                else {
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);     //단위 확인 필요
//                    System.out.println(meterVal);
                    // Meter Error !!!
                    // 전력량계 미 통신 시, 이 로직을 타게 된다
                }
            }catch(Exception e){
                LogWrapper.d(TAG, "Meter Err:"+e.toString());
            }
        }

        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        dspVersion = rxData.version; // DSP 버전 정보 저장



        // 충전 중, 초기화면으로 빠져나온 경우이거나, PC재부팅됐을 때 처리 (DSP 충전 종료 write)
        // 초기화면 상태 (카드 태그 대기)
        // by Lee 20200526
        if ( getUIFlowState() == UIFlowState.UI_CARD_TAG )
        {
            try {

                DSPRxData2 rxData2 = dspControl.getDspRxData2(chargeData.dspChannel);

                // dsp TestMode 체크 함수
                dspTestModeCheck(rxData2);

                // add by si. dsp 충전금지상태 체크
                dspChargingAvailableStat(rxData2);

                //add by si. dsp-plc 연결상태 체크
                dspPLCConnectStatMonitoring(rxData2);

                // 테스트모드가 아닐 시에만, 초기화면에서 충전 상태 체크한다.
                if (!dspTestModeOld) {
                    if (rxData2.get400Reg(DSPRxData2.STATUS400.CHARGE_RUN))
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
                    else
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
                }
            }
            catch (Exception e)
            { }

            //회원인증을 안하고 커넥터를 먼저 연결할 시, 카드 태그 메시지를 출력한다.
            // 자동인증이 아닌 경우에만 적용
            // PLC 연결이 된 상태에서만 수행
            //by Lee 20200526
            boolean plcconnStat = rxData.get407Reg((DSPRxData2.STATUS407.PLC_CONN_STAT));
            if(plcconnStat) {
                if ((!cpConfig.isAuthSkip) && (lastMeterStringList[0].equals("Connecting to EV"))) {
                    onReadyCharLCD(false);
                }
            }

        }

        //인증대기중일 떄 EIM으로부터 카드인증 결과 모니터링 - add by si. 200821
        if(getUIFlowState() == UIFlowState.UI_AUTH_WAIT){
            dspAuthResultMonitor(rxData);
        }

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();
            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            gridwizEvChargerInfo.chargingTime = chargingTimeSec;
            gridwizEvChargerInfo.curChargingKwh = (int)(chargeData.measureWh/10);
            gridwizEvChargerInfo.curChargingCost = (int)chargeData.chargingCost;
            gridwizEvChargerInfo.curChargingCostUnit = chargeData.chargingUnitCost;


            dispChargingMeterStrCnt++;
            if ( dispChargingMeterStrCnt > TypeDefine.DISP_CHARGING_CHARLCD_PERIOD) { // 8초주기 2초*4개 문구
                dispChargingMeterStrCnt = 0;

                onChargingCharLCD();
            }

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            if (chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD) == 0) {
                gridwizEvChargerInfo.socBatt = rxData.batterySOC*100;      //소수점 2자리까지 표시 (프로토콜상) 12.34 -> 1234

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
        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {

            // 충전 완료 시, 일 마감 자료 적산
            if (unplugTimerCnt == 0)
                CountDayAllChargingData(gridwizEvChargerInfo.curChargingKwh, gridwizEvChargerInfo.curChargingCost);


            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            boolean plugstat = rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG);

//            if (unplugTimerCnt++ > 5 && (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false)) {
            if (unplugTimerCnt++ > 5 && (plugstat == false)) {
                onPageCommonEvent(PageEvent.GO_HOME);
            }
            else {
                // Thanks 문구 출력

            }
        }

//        boolean plugstat_test = rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG);       //plug stat check - test

        // CHARLCD Backlight Off Timeout
        if ( dispChargingMeterBacklightCnt > TypeDefine.DISP_CHARLCD_BACKLIGHT_OFF_TIMEOUT) {
            try {
                IMeterAidlInterface meterService = mainActivity.getMeterService();
                meterService.setCharLCDBacklight(false);
            }
            catch (Exception e){
                dispChargingMeterBacklightCnt = 0;
            }
        }
        else {
            dispChargingMeterBacklightCnt++;
        }

        // 충전기 모드 변경
        onCpModeChanged();

    }


    /**
     * 예약 단가 유무 체크 후, 예약 단가 파일 복사 -> 단가 리스트 적용
     *
     * by Lee 20200810
     */
    private void reservationUnitCost()
    {
        if (gridwizEvChargerInfo.CheckCopyReserveFileToCostFIle(Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+TypeDefine.COST_INFO_FILENAME
                , Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+TypeDefine.COST_RESERVATION_INFO_FILENAME))
            //단가 리스트 적용
            gridwizEvChargerInfo.loadCostInfo(null, Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+TypeDefine.COST_INFO_FILENAME);
    }



    /**
     * 테스트 모드일 경우, 충전 대기 상태에서 캐릭터 LCD에 문구 표출을 위한 함수
     * 테스트 모드 -> 일반 모드 전환할 경우, 문구 롤백 전환 함수
     *
     * by Lee 20200728
     */
    boolean dspTestModeOld = false;
    int modeOld;
    private void dspTestModeCheck(DSPRxData2 rxData2)
    {
        if (rxData2.get407Reg(DSPRxData2.STATUS407.TEST_MODE) && !dspTestModeOld)
        {
            // 테스트모드 문구 전환 (테스트모드)
            dspTestModeOld = true;

            // DSP TestMode 변경 시, 현재 Mode 임시 저장 (원복 시, 되돌리기용)
            modeOld = gridwizEvChargerInfo.cpMode;

            gridwizEvChargerInfo.cpModeDspTestMode();     //충전기 모드 변경 (테스트 중)
            onDspTestModeCharLCD();
        }
        else if (!rxData2.get407Reg(DSPRxData2.STATUS407.TEST_MODE) && dspTestModeOld)
        {
            // 카드태깅 문구 전환 (일반모드)
            // DSP TestMode -> 일반모드 변경 시, 임시 저장한 mode 되돌리기
            gridwizEvChargerInfo.cpMode = modeOld;

            dspTestModeOld = false;
            gridwizEvChargerInfo.initCpModeReady();     //충전기 모드 변경 (테스트 중)
            onReadyCharLCD(false);
        }
    }

    /**
     * 회원인증 결과 모니터링 from EIM(그리드위즈)
     *
     * add by si. 200821
     */
    private void dspAuthResultMonitor(DSPRxData2 rxData2){
//        boolean success = false;
//        boolean fail = false;
//
//        success = rxData2.get407Reg(DSPRxData2.STATUS407.AUTH_SUCCESS_STAT);
//        fail = rxData2.get407Reg(DSPRxData2.STATUS407.AUTH_FAIL_STAT);

        if(rxData2.get407Reg(DSPRxData2.STATUS407.AUTH_SUCCESS_STAT)){
            //인증 성공일 경우
            onAuthResultEvent(true);
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.RF_TAG_EVENT, false);     //RF카드 태깅 이벤트 전달(EIM) - add by si.200821
//            rfidReader.rfidReadRelease();
        }
        else if(rxData2.get407Reg(DSPRxData2.STATUS407.AUTH_FAIL_STAT)){
            //인증 실패일 경우
            onAuthResultEvent(false);
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.RF_TAG_EVENT, false);     //RF카드 태깅 이벤트 전달(EIM) - add by si.200821
//            rfidReader.rfidReadRelease();
        }
    }

    /**
     * 충전금지 상태 모니터링(그리드위즈)
     *
     * add by si. 200819
     */
    boolean dspServiceMode_old = false;
    private void dspChargingAvailableStat(DSPRxData2 rxData2){
        if (rxData2.get407Reg(DSPRxData2.STATUS407.NO_SERVICE_MODE) && !dspServiceMode_old)
        {
            // 충전금지모드 문구 전환 (충전금지 모드)
            dspServiceMode_old = true;
            onDspNoServiceModeCharLCD();
//            setUIFlowState(UIFlowState.UI_NO_SERVICE);      //UI상태 변경(충전불가)
//            rfidReader.rfidReadRelease();       //RFID 리딩 대기상태 해지
        }
        else if (!rxData2.get407Reg(DSPRxData2.STATUS407.NO_SERVICE_MODE) && dspServiceMode_old)
        {
            // 카드태깅 문구 전환 (일반모드)
            dspServiceMode_old = false;
//            gridwizEvChargerInfo.initCpModeReady();     //충전기 모드 변경 (테스트 중)
            onReadyCharLCD(false);

            onPageStartEvent(false);
        }
    }

    /**
     * PLC 서버연결상태 모니터링(그리드위즈)
     *
     * add by si. 200818
     */

    boolean dspPLCStat = false;
    boolean dspPLCStat_backup = true;
    private void dspPLCConnectStatMonitoring(DSPRxData2 rxData2) {
        dspPLCStat = rxData2.get407Reg((DSPRxData2.STATUS407.PLC_CONN_STAT));
        if(dspPLCStat!=dspPLCStat_backup) {
            if (!dspPLCStat) {
                onReadyCharLCD(false);
            } else if (dspPLCStat) {
                //PCL Disconnect시에 오토스캔모드 스탑
//                rfidReader.rfidReadRelease();
                onDspPLCDisconnectedCharLCD();
            }

            dspPLCStat_backup = dspPLCStat;
        }

    }

    /**
     * 일 마감 자료 적산
     * 충전 완료 -> 일 마감 자료 적산
     *
     * by Lee 20200715
     * @param chargingKwh
     * @param chargingCost
     */
    private void CountDayAllChargingData(int chargingKwh, int chargingCost)
    {
        gridwizEvChargerInfo.dayAllChargingNum++;
        gridwizEvChargerInfo.dayAllChargingKwh += chargingKwh;
        gridwizEvChargerInfo.dayAllChargingCost += chargingCost;
    }

    /**
     * 운영시간 체크 후, 캐릭터 LCD에 표출
     *
     * by Lee 20200714
     */
    private boolean checkOperatingTime()
    {
        try {
            if (!compareOpenTime(gridwizEvChargerInfo.openTime))
                return false;
            else if (!compareCloseTime(gridwizEvChargerInfo.closeTime))
                return false;
        }
        catch (ParseException pe)
        {
        }
        return true;
    }

    public boolean compareOpenTime(String openTime) throws ParseException {


        String openTimeTemp = GetOperatingTime(openTime);

        SimpleDateFormat sdf;

        if (openTimeTemp.length() == 14)
            sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        else
            sdf = new SimpleDateFormat("HHmmss");


        Date date1 = sdf.parse(openTimeTemp);
        Date date2 = sdf.parse(GetCurrTime(openTimeTemp.length()));

        int result = date1.compareTo(date2);


        // 오픈시간 06:00:00 / 현재시간 12:00:00
//        if(date1.after(date2)){
        if(result == 1){
            // 충전 불가능 시간
            return false;
        }
        // 오픈시간 06:00:00 / 현재시간 03:00:00
        else {
            // 충전 가능 시간
            return true;
        }
    }



    public boolean compareCloseTime(String closeTime) throws ParseException {

        String closeTimeTemp = GetOperatingTime(closeTime);

        SimpleDateFormat sdf;

        if (closeTimeTemp.length() == 14)
            sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        else
            sdf = new SimpleDateFormat("HHmmss");


        Date date1 = sdf.parse(closeTimeTemp);
        Date date2 = sdf.parse(GetCurrTime(closeTimeTemp.length()));

        int result = date1.compareTo(date2);

        // 종료시간 00:00:00 / 현재시간 23:00:00
//        if(date1.before(date2)){
        if(result == -1){
            // 충전 불가능 시간
            return false;
        }
        // 종료시간 00:00:00 / 현재시간 03:00:00
        else {
            // 충전 가능 시간
            return true;
        }
    }

    /**
     * 날짜 데이터를 못받은 경우 = true
     * 날짜 데이터를 받은 경우 = false
     *
     * by Lee 20200714
     * @return
     */
    private String GetOperatingTime(String operatingTime)
    {
        try {
            if (operatingTime.substring(0, 8).equals("00000000"))
                return operatingTime.substring(8);
            else
                return operatingTime;
        }
        catch (Exception e)
        {
            return operatingTime;
        }
    }


    // 운영시간 안내 메시지
    private void onOperatingTimeCharLCD()
    {
        String openTime = gridwizEvChargerInfo.openTime.substring(8);
        String closeTime = gridwizEvChargerInfo.closeTime.substring(8);

        dispMeteringString(new String[] {"Not Available!!!","[Operating Time]", openTime + " - " + closeTime});
    }


    /**
     * 현재 시간 가져오기
     *
     * by Lee 20200714
     * @return
     */
    private String GetCurrTime(int length)
    {
        Date today = new Date();

        SimpleDateFormat date = null;

        if (length == 14)
            date = new SimpleDateFormat("yyyyMMddHHmmss");
        else
            date = new SimpleDateFormat("HHmmss");


        return date.format(today);
    }

    /**
     * 충전기 모드 변경
     *
     * by Lee 20200603
     */
    boolean OperatingStop = false;       // 운영모드 중지 플래그
    boolean NotOperatingTime = false;       // 운영시간 외 플래그
    private void onCpModeChanged()
    {
        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

        switch ( getUIFlowState() )
        {
            case UI_CARD_TAG:
                // 운영 모드 (C1 전문 관련)
                if (checkCpModeOperateStop() && (!OperatingStop))
                {
                    // 대기 중일 때, 운영중지에 대한 데이터를 서버로부터 받게 됐을 때, 캐릭터 LCD 문자열 처리를 위함 (처음 한번만 진행함)
                    // 운영 -> 점검 중 or 운영중지
                    onReadyCharLCD(false);
                    OperatingStop = true;
                }
                else if (!checkCpModeOperateStop() && OperatingStop)
                {
                    // 점검 중 -> 운영
                    OperatingStop = false;
                    onReadyCharLCD(false);
                }

                //운영 시간  (L1 전문 관련)
                else if (!checkOperatingTime() && !NotOperatingTime)
                {
                    // 운영시간 -> 운영시간X
                    onOperatingTimeCharLCD();
                    NotOperatingTime = true;
                }
                else if (checkOperatingTime() && NotOperatingTime)
                {
                    // 운영시간X -> 운영시간
                    onReadyCharLCD(false);
                    NotOperatingTime = false;
                }

                break;

            case UI_CHARGING:
                /*
                if (gridwizEvChargerInfo.cpMode != (EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_CHARGING))
                {
                    //충전 중, 충전기 모드 변경 (충전 중X)이 되면 충전 중지
                    if (rxData.get400Reg(DSPRxData2.STATUS400.CHARGE_RUN))
                    {
                        onChargingStop();       //Changed CpMode
                        dispMeteringString(new String[] {"Stop by SVR REQ", "Wait a second"});
                    }

                    InitFirstCheckOperate();
                }
                */
                break;

            default:
                InitFirstCheckOperate();
                break;
        }
    }


    private void InitFirstCheckOperate()
    {
        NotOperatingTime = false;
        OperatingStop = false;
    }

    /**
     * 충전기 리셋 명령 체크 (from Server)
     *
     *
     * by Lee 20200602
     */
    private void checkResetReq()
    {
    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if ( curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue ) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);

        }
    }

    //add by si.201209 - 전력량계 오류상태 모니터링함수
    //5분동안 Meter Read Error 상태일 경우 시스템 리셋
    boolean meterErrStat = false;
    public void MeterStatusMonitoring(long m_meterVal) {
        try {
            if (backupMeterval == 0) {
                //프로그램 부팅 후 초기화 진행
                initTime = System.currentTimeMillis();
                endTime = System.currentTimeMillis();
                distanceTime = 0;
                metererr_reset_flag = false;

            }

            //rf 오토스캔 동작,멈춤 설정
            if (m_meterVal != backupMeterval) {
                if (m_meterVal == -1){
//                    rfidReader.rfidReadRelease();
                    meterErrStat = true;
                }
                else {
                    meterErrStat = false;
//                    if (dspPLCStat) {
//                        rfidReader.rfidReadRequest();
//                    }
                }

                backupMeterval = m_meterVal;
            }


            if (m_meterVal == -1) {
                //Meter Read error일 경우
                //dsp로 에러신호 전송
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, true);

//                //UI 리셋 시간타이머 5분 체크
//                endTime = System.currentTimeMillis();
//                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초
//                if (distanceTime >= TypeDefine.METER_COMM_ERR_TIMEOUT) {
//                    //충전기 리셋 진행
//                    if (metererr_reset_flag == false) {
//                        resetRequest(true);
//                        metererr_reset_flag = true;
//                    }
//                }
            } else if (m_meterVal != -1) {
                //미터기 상태 정상일 경우
                //dsp 미터에러신호 복구 및 기타변수 초기화
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, false);
//                initTime = System.currentTimeMillis();
//                endTime = System.currentTimeMillis();
//                distanceTime = 0;
//                metererr_reset_flag = false;
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
            if ( flowState == UIFlowState.UI_CHARGING ) {
                onChargingStop();       //hardware reset
            }

            // 약 15초뒤에 Reset됨
            if ( watchDogTimer.isStarted == false ) watchDogTimer.openAndStart(15);
            else watchDogTimer.update();

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
                onChargingStop();       //software Reset
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
     *  100ms 마다 400번지의 값이 바뀌면 아래의 이벤트 함수 동작
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
                    if(!dspPLCStat) {
                        if (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT)
                            onConnectedCableEvent(val);
                    }
                    else if(dspPLCStat) onDspPLCDisconnectedCharLCD();
                    break;

                case STATE_DOOR:
                    isDspDoor = val;
                    if ( isDspDoor == false ) { // 도어 오픈
                        //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
                    }

                    //커넥터 도어 Open / Close 시, 알람자료 전송
                    onChangedConnectorStatus(isDspDoor);

                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if ( val == true ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);

                        //추가
                        if (cpConfig.isAuthSkip)
                            setUIFlowState(UIFlowState.UI_RUN_CHECK);

                        onDspChargingStartEvent();

                        gridwizEvChargerInfo.cpModeCharging();     //충전기 모드 변경 (충전 중)
                    }
                    break;

                case CG_STARTSTOPBT:
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
                        dispTempMeteringString(new String[]{"Stopping...", "Wait a second.."}, 6000);
                    }
                    else {
                        try{
                            IMeterAidlInterface meterService = mainActivity.getMeterService();
                            meterService.setCharLCDBacklight(true);
                            dispChargingMeterBacklightCnt = 0;      //lCD LED ON, 60초뒤 꺼짐.
                        }catch (Exception e){
                            LogWrapper.e(TAG, "dispMeteringString err:"+e.toString());
                        }

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
            if ( fInfo.isRepair == false ) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";

                faultCode = dispDspErrorString(fInfo.errorCode);

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
            usage = String.format("Usage: %.2fkWh", ((double) gridwizEvChargerInfo.curChargingKwh) / 100.0);
            cost = String.format("Cost: %dWon", gridwizEvChargerInfo.curChargingCost);
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
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_0, usage, cost);
                faultCode = AlarmCode.EMERGENCY;
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_1, usage, cost);
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_2, usage, cost);
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_3, usage, cost);
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_4, usage, cost);
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_5, usage, cost);
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_6, usage, cost);
                break;
            case 7:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_7, usage, cost);
                break;
            case 8:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_8, usage, cost);
                break;
            case 9:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_9, usage, cost);
                break;
            case 10:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_10, usage, cost);
                faultCode = AlarmCode.CHGR_INDOOR_TEMP_ERR;
                break;
            case 11:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_11, usage, cost);
                faultCode = AlarmCode.CHGR_INDOOR_COMM_ERR;
                break;
            case 12:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_12, usage, cost);
                faultCode = AlarmCode.CHGR_INDOOR_COMM_ERR;
                break;
            case 13:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_13, usage, cost);
                faultCode = AlarmCode.METER_COMM_ERR;
                break;
            case 14:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_14, usage, cost);
                faultCode = AlarmCode.METER_COMM_ERR;
                break;
            case 15:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_423_15, usage, cost);
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
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_0, usage, cost);
                break;
            case 1:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_1, usage, cost);
                break;
            case 2:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_2, usage, cost);
                break;
            case 3:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_3, usage, cost);
                break;
            case 4:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_4, usage, cost);
                break;
            case 5:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_5, usage, cost);
                break;
            case 6:
                SetDispErrorString(isCharging, DefineErrorCode.ERR_CODR_425_6, usage, cost);
                break;
        }
        LogWrapper.v(TAG, "DSP ERROR-425-" + Integer.toString(bit));

        return faultData;
    }

    boolean pre_dspPLCStat = false;
    boolean pre_meterErrStat = false;
    boolean pre_isDspFault = false;
    boolean pre_isDSPCommErr = false;
    boolean pre_dspServiceMode_old = false;

    UIFlowState pre_rfcontrol = UIFlowState.UI_CARD_TAG;

    public void onRFMonitoring() {

        if (pre_rfcontrol != getUIFlowState()) {
            if (getUIFlowState() == UIFlowState.UI_CARD_TAG) rfidReader.rfidReadRequest();
            else rfidReader.rfidReadRelease();

            pre_rfcontrol = getUIFlowState();
        }

        //PLC연결상태,미터기 연결상태, DSP에러상태가 모두 정상일 경우에만 초기 page이동시 RF 오토스캔 동작하도록
        if (getUIFlowState() == UIFlowState.UI_CARD_TAG) {
            if (dspPLCStat != pre_dspPLCStat) {
                if (dspPLCStat) rfidReader.rfidReadRelease();      //PLC stat = false,연결정상, PLC stat = true,연결끊김
                else rfidReader.rfidReadRequest();
                pre_dspPLCStat = dspPLCStat;
            } else if (meterErrStat != pre_meterErrStat) {
                if (meterErrStat) rfidReader.rfidReadRelease();
                else rfidReader.rfidReadRequest();
                pre_meterErrStat = meterErrStat;
            } else if (isDspFault != pre_isDspFault) {
                if (isDspFault) rfidReader.rfidReadRelease();
                else rfidReader.rfidReadRequest();
                pre_isDspFault = isDspFault;
            } else if (isDSPCommErr != pre_isDSPCommErr) {
                if (isDSPCommErr) rfidReader.rfidReadRelease();
                else rfidReader.rfidReadRequest();
                pre_isDSPCommErr = isDSPCommErr;
            } else if (dspServiceMode_old != pre_dspServiceMode_old) {
                if (dspServiceMode_old) rfidReader.rfidReadRelease();
                else rfidReader.rfidReadRequest();
                pre_dspServiceMode_old = dspServiceMode_old;
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

                    onChargingStop();       //fault
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                        getUIFlowState() != UIFlowState.UI_CARD_TAG)  {
                    onPageStartEvent(false);        //충전중이 아니었을떄, 폴트
                }

                fillFaultMessage();
                pageManger.showFaultBox();
            }
            else {
                if (faultCode != 0)     //알람해제 중복 처리

                faultCode = 0;
                pageManger.hideFaultBox();
                onCheckHideFault();
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
        switch (getUIFlowState())
        {
            case UI_CARD_TAG:
                onReadyCharLCD(false);
                break;

            case UI_AUTH_WAIT:
                onCardVerifingCharLCD();
                break;

            case UI_CONNECTOR_WAIT:
                onCardCheckDoneCharLCD();
                break;

            case UI_RUN_CHECK:
                onStartChargingCharLCD();
                break;

            case UI_CHARGING:
                onChargingCharLCD();
                break;

            case UI_FINISH_CHARGING:
                onFinishedCharLCD();
                break;
        }

        LogWrapper.v(TAG, "Finish Charging:"+String.format("Usage: %.2fkWh", ((double) gridwizEvChargerInfo.curChargingKwh)/100.0) +
                ", "+String.format("Cost: %dWon", gridwizEvChargerInfo.curChargingCost));
    }

    private void onStartChargingCharLCD()
    {
        dispMeteringString(new String[] {"Start Charging"});
    }

    private void onCardVerifingCharLCD()
    {
        dispMeteringString(new String[] {"Card Verifing...", "Wait a second"});
    }

    private void onCardCheckDoneCharLCD()
    {
        dispMeteringString(new String[] {"Card Check Done.", "Connect Cable"});
    }

    /**
     * DSP Comm Error 복구 시, param=true
     *
     * by Lee 20200810
     */
    private void onReadyCharLCD(boolean dspCommRecovery)
    {
        NotOperatingTime = false;


        if (!dspCommRecovery)
            if (dspControl.getDspCommError())
                onDSPCommErrorCharLCD();

        if (dspTestModeOld)
            onDspTestModeCharLCD();
        else if ( checkCpModeOperateStop() )
            dispMeteringString(new String[]{"Operating Stop", "Plz use Others"});
        else if(dspPLCStat)
            dispMeteringString(new String[]{"PLC Disconnected"});
        else if (!cpConfig.isAuthSkip)
            dispMeteringString(new String[]{"Welcome! Gridwiz", "Tag Card for EV"});

        else
            dispMeteringString(new String[]{"Welcome!", "Connect Cable", "Free Mode"});
    }


    /**
     * DSP TestMode 시, 충전 대기 상태에서 캐릭터 LCD에 문구 표출
     *
     * by Lee 20200728
     */
    private void onDspTestModeCharLCD()
    {
        if (dspControl.getDspCommError())
            onDSPCommErrorCharLCD();
        else
            dispMeteringString(new String[]{"DSP TestMode ON"});
    }

    private void onDspNoServiceModeCharLCD(){
        if (dspControl.getDspCommError())
            onDSPCommErrorCharLCD();
        else
            dispMeteringString(new String[]{"DSP:No Service"});     //충전금지 표시
    }

    private void onDspPLCDisconnectedCharLCD(){
        if (dspControl.getDspCommError())
            onDSPCommErrorCharLCD();
        else if(dspServiceMode_old)
            dispMeteringString(new String[]{"DSP:No Service"});     //충전금지 표시
        else
            dispMeteringString(new String[]{"PLC Disconnected"});     //충전금지 표시
    }

    private void onChargingCharLCD()
    {
        dispMeteringString(new String[]{"Charging...",
                String.format("Usage: %.2fkWh", ((double) gridwizEvChargerInfo.curChargingKwh) / 100.0),
                String.format("Cost: %dWon", gridwizEvChargerInfo.curChargingCost)}, false);
    }

    private void onFinishedCharLCD()
    {
        dispMeteringString(new String[] {"Finished.",
                "Unplug Cable",
                String.format("Usage: %.2fkWh", ((double) gridwizEvChargerInfo.curChargingKwh)/100.0),
                String.format("Cost: %dWon", gridwizEvChargerInfo.curChargingCost)});
    }

    private void onOperatingStopCharLCD()
    {
        dispMeteringString(new String[] {"Operating Stop", "Plz use Others"});
    }

    private void onDisConnectSVRCharLCD()
    {
        dispMeteringString(new String[] {"Welcome! Gridwiz", "Disconnected SVR", "Connecting..."});
    }

    private void onDSPCommErrorCharLCD()
    {
        dispMeteringString(new String[]{"INTER COMM ERR.", "ERROR CODE:11"});
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 사용안함
    }

    @Override
    /**
     * DSP TX 데이터를 를 Overlay 하여 write 한다. DSP TX 주기적으로 하기 직전에 항상 불려진다.
     */
    public void dspTxDataEncodeExt(int channel, byte[] rawData, int startOffset) {
        /*
        // 예시
        int idx = startOffset + 30*2; // 230 번지 부터 시작
        rawData[idx++] = cardNum[0];
        rawData[idx++] = cardNum[1];
        */


        byte[] cardNum = gridwizEvChargerInfo.cardNum.getBytes();

        //카드번호 229번지부터 시작
        int idx = startOffset + 29 * 2;

        for (int i = 0; i < 16; i++) {
            rawData[idx++] = cardNum[i];
        }
    }


    public boolean isDSPCommErr = false;
    @Override
    public void onDspCommErrorStstus(boolean isError) {
        if (isError == true) {
            pageManger.showDspCommErrView();

            if (getUIFlowState() == UIFlowState.UI_CHARGING)
            {
                onChargingStop();
            }

            onDSPCommErrorCharLCD();

            isDSPCommErr = true;
//            //UI 상태 내부통신장애로 변경
//            rfidReader.rfidReadRelease();   //rf리더기 오토스캔 잠금 전환

            LogWrapper.e(TAG, "DSP-UI Comm Error!!");
        }
        else {
            pageManger.hideDspCommErrView();

            isDSPCommErr = false;
            onPageStartEvent(true);

            LogWrapper.e(TAG, "DSP-UI Comm Recovery.");
        }
    }

    //================================================
    // 통신 메시지 수신
    //================================================

    //@Override
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

        onReadyCharLCD(false);
    }

    //서버 커넥션 X
    public void onCommDisconnected() {
        mainActivity.setCommConnStatus(false);

        if ( checkCpModeOperateStop() )
            onOperatingStopCharLCD();
        else
            onDisConnectSVRCharLCD();
    }

    //@Override
    public void onAuthResultEvent(String cardNum, boolean isSuccess) {
        onAuthResultEvent(isSuccess);
    }

    //@Override
    public void onResetRequest(int kind) {
        // 충전기 리셋
        if ( kind == 0x01 ) {
            resetRequest(true);
        }
    }

    //@Override
    public void onRecvVersionResp(String swVersion, String costVersion) {
        // SW 버전을 비교하여 새로운 버전인 경우에 m1 업데이트를 진행한다.
        if ( swVersion.compareTo(TypeDefine.SW_VERSION) > 0) {
        }
    }

    //@Override
    public void onCostInfoUpdateEvent(boolean isReservation) {
        gridwizEvChargerInfo.saveCostInfo(mainActivity.getBaseContext(), Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, TypeDefine.COST_INFO_FILENAME, isReservation);
    }

    //@Override
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
     * 운영중지 체크 함수
     * 충전기 모드가 운영 중지 시, 사용자 인증 을 진행하지 않는다.
     *
     * by Lee 20200603
     * @return
     */
    private boolean checkCpModeOperateStop()
    {
        boolean bIsOperateStop = false;
        /*
        switch ( gridwizEvChargerInfo.cpMode )
        {
            case (EvCommDefine.CP_MODE_BIT_STOP_OPERATING)://운영중지
            case (EvCommDefine.CP_MODE_BIT_STOP_OPERATING | EvCommDefine.CP_MODE_BIT_CHECKING)://운영중지+점검중
            case (EvCommDefine.CP_MODE_BIT_STOP_OPERATING | EvCommDefine.CP_MODE_BIT_TESTING)://운영중지+테스트
            case (EvCommDefine.CP_MODE_BIT_OPERATING | EvCommDefine.CP_MODE_BIT_READY | EvCommDefine.CP_MODE_BIT_CHECKING)://운영+대기+점검중
                bIsOperateStop = true;
                break;

            default:
                bIsOperateStop = false;
                break;
        }
         */

        return bIsOperateStop;
    }

    //================================================
    // RFI+D 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if ((flowState == UIFlowState.UI_CARD_TAG) && !dspPLCStat) {
            LogWrapper.v(TAG, "Card Tag Event:" + rfid);
            String myfarecnum = "";
//            String myfarecnum = "00000000" + rfid;
            int rfidLen = 16 - rfid.length();

            for (int i = 0; i < rfidLen; i++) {
                myfarecnum += "0";
            }
            myfarecnum = myfarecnum + rfid;
            onCardTagEvent(myfarecnum, true);
//            onCardTagEvent(rfid, true);

            //dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
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
