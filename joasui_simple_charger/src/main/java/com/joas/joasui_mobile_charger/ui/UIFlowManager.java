/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:35
 *
 */

package com.joas.joasui_mobile_charger.ui;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;

import com.joas.joasui_mobile_charger.ui.page.PageEvent;
import com.joas.joasui_mobile_charger.ui.page.PageID;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class UIFlowManager implements DSPControlListener {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_SELECT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING
    }

    PageManger pageManger;

    Activity mainActivity;

    UIFlowState flowState = UIFlowState.UI_SELECT;
    ChargeData chargeData;
    CPConfig cpConfig;

    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    // DSP 관련 Attr
    DSPControl dspControl;
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

    TimeoutTimer timerSec = null;
    String lastCardNum = "";

    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    public UIFlowManager(Activity activity, ChargeData data, CPConfig config) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        dspControl = new DSPControl(1, "/dev/ttyS2", DSPControl.DSP_VER_REG23_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE, 10,  this);
        dspControl.start();

        dspControl.setMeterUse(true);
        dspControl.setMeterType(DSPControl.METER_TYPE_FAST);

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        // 1초 타이머 시작
        startPeroidTimerSec();
    }

    public void destory() {
        dspControl.interrupt();
        dspControl.stopThread();
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public DSPControl getDspControl() { return dspControl; }

    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }

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
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch ( state ) {
            case UI_SELECT:
                initStartState();
                dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.READY, true);
                break;

            case UI_CONNECTOR_WAIT:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, true);
                }
                break;

            case UI_RUN_CHECK:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, true);
                break;

            case UI_FINISH_CHARGING:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_FINISH_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, true);
                break;
        }
    }

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.READY, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, false);
    }

    public void onPageSelectEvent(PageEvent event) {
        switch ( event ) {
            case SELECT_AC3_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.AC3;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_FAST_AC3);
                break;
            case SELECT_CHADEMO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_FAST_DCCHADEMO);
                break;
            case SELECT_DCCOMBO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_FAST_DCCOMBO);
                break;
        }

        setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
        pageManger.changePage(PageID.CONNECTOR_WAIT);
    }

    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                goHomeProcess();
                break;
            case START_CHARGE:
                startCharge();
                break;
        }
    }

    // Home으로 돌아간다.
    public void goHomeProcess() {
        setUIFlowState(UIFlowState.UI_SELECT);
        pageManger.changePage(PageID.SELECT_FAST);
    }

    public void goHomeProcessDelayed(int delayMs) {
        final int timeout = delayMs;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        goHomeProcess();
                    }
                }, timeout);
            }
        });
    }

    public void startCharge() {
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
            setUIFlowState(UIFlowState.UI_RUN_CHECK);

            // 이미 Run이 된 상태이라면
            if ( isDspChargeRun ) {
                onChargingStartEvent();
            }
            else {
                // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                pageManger.changePage(PageID.CONNECT_CAR_WAIT);
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
        chargeData.soc = 0;
        chargeData.remainTime = 0;
    }

    /**
     * 충전 시작일때 이벤트
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

            // 변수를 초기화 한다.
            initChargingStartValue();

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        if ( isConnected ) {
            if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
                setUIFlowState(UIFlowState.UI_RUN_CHECK);

                // 이미 Run이 된 상태이라면
                if ( isDspChargeRun ) {
                    onChargingStartEvent();
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
        }
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, true);

        // 종료이벤트 발생
       // onFinishChargingEvent();
        // 종료될때까지 기다린다.
    }

    public void onSettingViewExit() {
        pageManger.changePreviousPage();
    }

    // 1초에 한번씩 WatchDog 타이머를 수행한다.
    public void watchDogTimerProcess() {
        if ( cpConfig.useWatchDogTimer == false ) return;

        if ( watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
            if ( watchDogTimerStartCnt == TypeDefine.WATCHDOG_START_TIMEOUT) {
                // WatchDog 타이머 시작(open과 함께 시작)
                watchDogTimer.openAndStart(16);
                watchDogTimerStartCnt++; // 이후버터는 update만 실행
            }
            else {
                //Watch Dog Timer 갱신
                watchDogTimer.update();
                Log.v(TAG, "WatchDog Update..");
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

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();

            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            if ( chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD ) == 0) {
            }

            DSPRxData rxData = dspControl.getDspRxData(chargeData.dspChannel);
            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;
        }
    }

    public void stopWatdogTimer() {
        watchDogTimer.stop();
        watchDogTimer.close();
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
                        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, false);
                    }
                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if ( val == true ) onChargingStartEvent();
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
            }
        }
    }

    public synchronized void onFaultEventProcess() {
        Vector<FaultInfo> fList = faultManager.sacnFault(chargeData.dspChannel);
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
                    onChargingStop();
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                        getUIFlowState() != UIFlowState.UI_SELECT )  {
                    goHomeProcess();
                }

                fillFaultMessage();
                pageManger.showFaultBox();
            }
            else {
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
    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            int gapMeter = (int)(meterVal - lastMeterValue);
            chargeData.measureWh += (meterVal - lastMeterValue);
            chargeData.chargingCost += ((double)gapMeter/1000.0)*(double)chargeData.chargingUnitCost;
        }
        lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {

    }
}
