/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:16
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui;

import android.os.Handler;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;

import com.joas.joasui_mobile_charger_2ch.ui.page.PageEvent;
import com.joas.joasui_mobile_charger_2ch.ui.page.PageID;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class UIFlowManager  {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_SELECT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING
    }

    PageManger pageManger;

    MoileChargerActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_SELECT;
    ChargeData chargeData;
    CPConfig cpConfig;

    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;

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

    CableSelectorAlt cableSelectorAlt;

    public UIFlowManager(MoileChargerActivity activity, DSPControl dspctl, ChargeData data, CPConfig config, CableSelectorAlt cablesel) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        dspControl = dspctl;

        cableSelectorAlt = cablesel;

        // 완속일 경우 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(true);
        dspControl.setMeterType(DSPControl.METER_TYPE_FAST);

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);

        // 1초 타이머 시작
        startPeroidTimerSec();

        initStartState();
        if ( chargeData.dspChannel == 1 )
            dspControl.setConnectorSelect(chargeData.dspChannel, 0x8000);
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

        processChangeState(flowState);
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

    void addLog(String msg) {
        FileUtil.appendDateLog(chargeData.logFilePath, ", CH:"+chargeData.dspChannel+", " + msg);
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
                dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, false);
                dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, false);
                break;
        }
    }

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.READY, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, false);

        // 충전기 케이블 상태 초기화
        switch ( chargeData.connectorType ) {
            case CHADEMO:
                if ( cableSelectorAlt.useChademo ) cableSelectorAlt.useChademo = false;
                break;
            case DCCOMBO1:
                if ( cableSelectorAlt.useCombo1 ) cableSelectorAlt.useCombo1 = false;
                break;
            case DCCOMBO2:
                if ( cableSelectorAlt.useCombo2 ) cableSelectorAlt.useCombo2 = false;
                break;
        }
        chargeData.connectorType = TypeDefine.ConnectorType.NONE;
        mainActivity.setPlugTitle(chargeData.dspChannel, "");
    }

    void showCableSelectWrong() {
        chargeData.messageBoxTitle = "알림";
        chargeData.messageBoxContent = "다른 채널에서 이미 사용중입니다.\r\n\r\n다른 충전 타입을 선택해 주세요.";
        pageManger.showMessageBox();
    }

    public void onPageSelectEvent(PageEvent event) {
        switch ( event ) {
            case SELECT_CHADEMO_CLICK:
                if ( cableSelectorAlt.useChademo == true ) {
                    showCableSelectWrong();
                    return;
                }
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                if ( chargeData.dspChannel == 0 )
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCHADEMO);
                else
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCHADEMO | 0x8000);
                cableSelectorAlt.useChademo = true;
                mainActivity.setPlugTitle(chargeData.dspChannel, "(DC 차데모)");
                addLog("Chademo 선택");
                break;
            case SELECT_DCCOMBO1_CLICK:
                if ( cableSelectorAlt.useCombo1 == true ) {
                    showCableSelectWrong();
                    return;
                }

                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO1;
                if ( chargeData.dspChannel == 0 )
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCOMBO1);
                else
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCOMBO1 | 0x8000);
                cableSelectorAlt.useCombo1 = true;
                mainActivity.setPlugTitle(chargeData.dspChannel, "(DC 콤보1)");
                addLog("DCCombo1 선택");
                break;
            case SELECT_DCCOMBO2_CLICK:
                if ( cableSelectorAlt.useCombo2 == true ) {
                    showCableSelectWrong();
                    return;
                }

                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO2;
                if ( chargeData.dspChannel == 0 )
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCOMBO2);
                else
                    dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_MOBILE_DCCOMBO2 | 0x8000);

                cableSelectorAlt.useCombo2 = true;
                mainActivity.setPlugTitle(chargeData.dspChannel, "(DC 콤보2)");
                addLog("DCCombo2 선택");
                break;
        }

        // 전력제한 세팅
        dspControl.setPowerlimit(chargeData.dspChannel, chargeData.powerLimitValue | 0x8000);

        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.READY, true);
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
            addLog("충전 시작 요청(사용자)");

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
            chargeData.lastMeterValue = lastMeterValue; // 최종 전력값
            // 변수를 초기화 한다.
            initChargingStartValue();

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManger.changePage(PageID.CHARGING);

            addLog("차량 연결 성공. 충전 시작("+chargeData.powerLimitValue+"kWh)");
        }
    }

    /*
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
    */


    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManger.changePage(PageID.FINISH_CHARGING);
            addLog("충전 종료");
        }
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, true);
        addLog("충전 중지 요청(사용자)");

        // 종료이벤트 발생
       // onFinishChargingEvent();
        // 종료될때까지 기다린다.
    }

    public void onSettingViewExit() {
        pageManger.changePreviousPage();
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {

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

            lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);
            chargeData.lastMeterValue = lastMeterValue; // 최종 전력값

            chargeData.chargingVoltage = rxData.voltageOut;
            chargeData.chargingAmpare = rxData.ampareOut;
            if ( chargeData.chargingVoltage  < 0 ) chargeData.chargingVoltage = 0;
            if ( chargeData.chargingAmpare < 0 ) chargeData.chargingAmpare = 0;

            String logStr = String.format("[충전 데이터], 전압: %.2fV, 전류: %.2fA, 전력:%.2fkWh, SOC:%d, 남은시간:%d",
                    chargeData.chargingVoltage, chargeData.chargingAmpare, (float)lastMeterValue/100.0f, chargeData.soc, chargeData.remainTime);
            addLog(logStr);
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
                timerSec.cancel();
            }
        }

        mainActivity.finish();
    }

    public void setDisableUIByMode(boolean tf) {
        if ( tf ) {
            chargeData.messageBoxTitle = "알림";
            chargeData.messageBoxContent = "다른 채널에서 싱글모드 사용중입니다.\r\n\r\n사용하시려면 듀얼모드를 선택해 주세요.";
            chargeData.messageBoxInfiniteMode = true;
            pageManger.showMessageBox();
        }
        else {
            pageManger.hideMessageBox();
            chargeData.messageBoxInfiniteMode = false;
        }
    }

    public void onModeChange(boolean isSingle) {
        int peerCh = chargeData.dspChannel == 0 ? 1 : 0;
        UIFlowManager manager = mainActivity.getFlowManager(peerCh);

        if ( isSingle ) {
            manager.setDisableUIByMode(true);
        }
        else {
            manager.setDisableUIByMode(false);
        }
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
                    //onConnectedCableEvent(true);
                    addLog("케이블 연결 "+(isDspPlug ? "신호 발생" : "끊어짐 신호 발생"));
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
                    if ( val == true ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, false);
                        onChargingStartEvent();
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
        String logMsg = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false ) {
                chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                chargeData.faultBoxContent += "\r\n";

                logMsg += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                logMsg += " | ";
            }
        }
        addLog("폴트(오류)발생: "+logMsg);
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


    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)

        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            /*
            int gapMeter = (int)(meterVal - lastMeterValue);
            if ( gapMeter < 0 ) gapMeter = 1;

            // 2CH용일때 처음 시작시에 DSP에서 연결 이후에 전력량 값이 제대로 읽어짐.
            // 처음 10초동안 만약 gap이 1kw이상 날경우 0으로 처리함
            if ( gapMeter > 100 ) {
                int chargingTimeSec = (int)(chargeData.chargingTime/1000);
                if ( chargingTimeSec < 10 ) gapMeter = 0;
                else gapMeter = 100;
            }
            chargeData.measureWh += gapMeter;
            */
            //chargeData.measureWh = meterVal;
        }
        lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }
}
