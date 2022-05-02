/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_dubai_2ch;

import android.os.Handler;
import android.view.View;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;
import com.joas.hw.rfid.RfidReaderListener;

import com.joas.ocpp.chargepoint.OCPPSession;
import com.joas.ocpp.chargepoint.OCPPSessionManager;
import com.joas.ocpp.chargepoint.OCPPSessionManagerListener;
import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.SampledValue;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.ocpp.msg.TriggerMessage;
import com.joas.ocpp.stack.OCPPConfiguration;
import com.joas.ocppui_dubai_2ch.page.PageEvent;
import com.joas.ocppui_dubai_2ch.page.PageID;
import com.joas.utils.LogWrapper;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControlListener, OCPPSessionManagerListener, UpdateManagerListener {
    public static final String TAG = "UIFlowManager";

    @Override
    public void onUpdateStatus(UpdateManager.UpdateState state) {

    }

    public enum UIFlowState {
        UI_START,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING
    }

    MultiChannelUIManager multiChannelUIManager;
    PageManager pageManager;
    DSPControl dspControl;

    OCPPUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_START;
    ChargeData chargeData;
    CPConfig cpConfig;


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

    String lastCardNum = "";

    int meterTimerCnt = 0;
    long lastClockedMeterValue = -1;

    boolean isRemoteStarted = false;
    boolean isConnectorOperative = true;

    int unplugTimerCnt = 0;

    OCPPSessionManager ocppSessionManager;

    FaultManager faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    StopTransaction.Reason stopReason = StopTransaction.Reason.LOCAL;
    double powerLimit = -1;

    int dspVersion = 0;

    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();
    int channel;

    public UIFlowManager(int chan, OCPPUIActivity activity, MultiChannelUIManager uiManager, ChargeData data, CPConfig config, OCPPSessionManager sessionManager, DSPControl control, PageManager page) {
        channel = chan;
        mainActivity = activity;
        multiChannelUIManager = uiManager;
        chargeData = data;
        cpConfig = config;
        ocppSessionManager = sessionManager;
        dspControl = control;
        pageManager = page;

        // FaultManager를 생성한다.
        faultManager = new FaultManager(dspControl, mainActivity, chargeData.dspChannel);
    }

    public void setPageManager(PageManager manager) {
        pageManager = manager;
    }
    public DSPControl getDspControl() { return dspControl; }

    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }
    public MultiChannelUIManager getMultiChannelUIManager() { return multiChannelUIManager; }

    public UIFlowState getUIFlowState() { return flowState; }

    void setUIFlowState(UIFlowState state) {
        flowState = state;

        processChangeState(flowState );
    }

    public void setStopReason(StopTransaction.Reason reason) { stopReason = reason; }
    public int getDspVersion() { return dspVersion; }

    /**
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch ( state ) {
            case UI_START:
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
                unplugTimerCnt = 0;
                break;
        }
    }

    void initStartState() {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, false);

        if ( isConnectorOperative == false ) pageManager.showUnavailableConView();

        // 변수 초기화
        chargeData.measureWh = 0;
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        powerLimit = -1.0d;
        unplugTimerCnt = 0;

        multiChannelUIManager.rfidReaderRelease(channel);

        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
        ocppSessionManager.closeSession(chargeData.curConnectorId);

        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;
    }

    public void onPageStartEvent() {
        setUIFlowState(UIFlowState.UI_START);
        pageManager.changePage(PageID.CONNECTOR_WAIT);
    }

    /**
     * Select화면에서 선택되었을때 이벤트 발생
     * @param event
     */

    public void onPageSelectEvent(PageEvent event) {
        // Fault인경우에 초기화면으로 돌아감
        if ( isDspFault ) {
            fillFaultMessage();
            pageManager.showFaultBox();
            return;
        }

        switch ( event ) {
            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_SLOW_BTYPE);
                break;
            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_SLOW_CTYPE);
                break;
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

        if ( isRemoteStarted ) {
            // 원격 시작이면 이미 session은 시작되었음. startSession을 따로 하지 않는다.
            // 바로 커넥터 연결화면으로 간다.
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            // 이미 Connect 된 상태이라면
            if ( isDspPlug ) {
                onConnectedCableEvent(true);
            } else {
                // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                pageManager.changePage(PageID.CONNECTOR_WAIT);
            }
        }
        else {
            if ( multiChannelUIManager.rfidReaderRequest(channel) == false ) {
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_notice);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_rfid_request_not_ready);
                pageManager.showMessageBox();

                goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
                return;
            }
            // OCPP Session Start
            ocppSessionManager.startSession(chargeData.curConnectorId);

            // Next Flow. Card Tag
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManager.changePage(PageID.CARD_TAG);


        }

        setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
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
                        pageManager.showMessageBox();

                        goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
                    }
                    else {
                        reserveInfo.init();
                        mainActivity.setReservedVisible(View.INVISIBLE);
                        setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                        onAuthResultEvent(true);
                    }
                }
                else {
                    // Tag Data 전송
                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);
                    lastCardNum = tagNum;

                    // 승인대기 화면 전환
                    setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                    pageManager.changePage(PageID.AUTH_WAIT);
                }
            }
            else if ( flowState == UIFlowState.UI_CHARGING ) {
                if ( tagNum.equals(lastCardNum) == true ) {
                    onChargingStop();

                    // 일반적인 충전 시퀀스에 따라 인증후 Finish 상태를 보냄
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);
                }
                else {
                    // To Do.
                    // 메시지 박스!!!
                    chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                    chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                    pageManager.showMessageBox();
                    pageManager.getChargingView().onFailStopCharging();
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

    public void onAuthResultEvent(boolean isSuccess) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (isSuccess) {
                // 일반적인 충전 시퀀스에 따라 인증후 Charging상태를 보냄
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);

                setUIFlowState(UIFlowState.UI_RUN_CHECK);

                // 이미 Run이 된 상태이라면
                if ( isDspChargeRun ) {
                    onDspChargingStartEvent();
                }
                else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManager.changePage(PageID.CONNECT_CAR_WAIT);
                }
            } else {
                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManager.getAuthWaitView().stopTimer();
                pageManager.showMessageBox();

                goHomeProcessDelayed(chargeData.messageBoxTimeout*1000);
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
    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            // 통신으로 충전시작 메시지를 보낸다.
            ocppSessionManager.startCharging(chargeData.curConnectorId, (int)lastMeterValue);

            DSPRxData rxData = dspControl.getDspRxData(chargeData.dspChannel);
            sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_BEGIN, true);

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManager.changePage(PageID.CHARGING);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        if ( isConnected ) {
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            if ( getUIFlowState() == UIFlowState.UI_START) {
                setUIFlowState(UIFlowState.UI_CARD_TAG);
                pageManager.changePage(PageID.CARD_TAG);

                // OCPP Session Start
                ocppSessionManager.startSession(chargeData.curConnectorId);

                multiChannelUIManager.rfidReaderRequest(channel);

                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
            }
        }
        else {
            //충전중이 아닐때
            if ( getUIFlowState() == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }
            else {
                setUIFlowState(UIFlowState.UI_START);
                pageManager.changePage(PageID.CONNECTOR_WAIT);
            }
        }
    }

    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManager.changePage(PageID.FINISH_CHARGING);

            // 커넥터 상태를 충전 종료중으로 바꾼다.(Status 메시지 보냄)
            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FINISHING);

            DSPRxData rxData = dspControl.getDspRxData(chargeData.dspChannel);
            sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_END, true);

            //통신으로 종료 패킷을 보낸다.
            ocppSessionManager.stopCharging(chargeData.curConnectorId, (int)lastMeterValue, stopReason);

            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, false);
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, true);
        pageManager.getChargingView().onChargingStop();
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
        //Fault 함수 수행
        onFaultEventProcess();

        //PowerLimit 함수 수행
        powerLimitProcess();

        //Reserve Check
        if ( reserveInfo.expiryCheck() == true ) mainActivity.setReservedVisible(View.INVISIBLE);

        DSPRxData rxData = dspControl.getDspRxData(chargeData.dspChannel);
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
            if ( meterTimerCnt >= meterInterval && meterInterval > 0) {
                sendOcppMeterValues(rxData, SampledValue.Context.SAMPLE_PERIODIC, true);
                meterTimerCnt = 0;
            }

            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;
        }

        // ClockedAlign MeterValue 를 보낸다.
        processClockAlignMeterValue(rxData);

        // Event에서 poll로 바꿈.
        if ( rxData.get400Reg(DSPRxData.STATUS400.STATE_DOOR) == false ) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.DOOR_OPEN, false);
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && rxData.get400Reg(DSPRxData.STATUS400.STATE_PLUG) == true ) {
            //onConnectedCableEvent(true);
        }

        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++> 5 && rxData.get400Reg(DSPRxData.STATUS400.STATE_PLUG) == false) {
                onPageCommonEvent(PageEvent.GO_HOME);
            }
        }
    }
    /**
     * 1초마다 호출, Meter Interval 마다 Sample List들을 보냄
     */
    protected void processClockAlignMeterValue(DSPRxData rxData) {
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

            ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, lastMeterValue, gapMeter, (int)rxData.ampareOut, (int)(rxData.ampareOut*rxData.voltageOut),
                    currentOffered, powerOffered, soc, SampledValue.Context.SAMPLE_CLOCK,
                    ocppSessionManager.getOcppConfiguration().MeterValuesAlignedData, getUIFlowState() == UIFlowState.UI_CHARGING );
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
                    onConnectedCableEvent(val);
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
                    if (isEmergency == true) stopReason = StopTransaction.Reason.EMERGENCY_STOP;
                    else stopReason = StopTransaction.Reason.OTHER;
                    onChargingStop();
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_START)  {
                    onPageStartEvent();
                }

                fillFaultMessage();
                pageManager.showFaultBox();
            }
            else {
                pageManager.hideFaultBox();
            }
            isPreDspFault = isDspFault;
        }

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

        // 긴급버턴 이벤트 발생
        if ( isEmergencyPressed != isEmergency ) {
            if (isEmergency == true) {
                pageManager.showEmergencyBox();
            } else { // 긴급 버턴 해제
                pageManager.hideEmergencyBox();
            }
            isEmergencyPressed = isEmergency;
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
        if (isError == true) {
            pageManager.showDspCommErrView();
        }
        else {
            pageManager.hideDspCommErrView();
        }
    }

    //================================================
    // OCPP Event 송/수신, OCPP 처리
    //================================================

    public void setOcppStatus(int connectorId, StatusNotification.Status status) {
        StatusNotification.Status oldStatus = chargeData.ocppStatus;
        if ( oldStatus != status ) {
            chargeData.ocppStatus = status;
            ocppSessionManager.SendStatusNotificationRequest(connectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
        }
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
        // 각 Connector의 상태값을 보낸다.
        ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId, chargeData.ocppStatus, chargeData.ocppStatusError );
    }

    // 미터값, SOC 등 값을 전달한다.
    public void sendOcppMeterValues(DSPRxData rxData, SampledValue.Context context, boolean isInTransaction) {
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

        ocppSessionManager.sendMeterValueRequest(chargeData.curConnectorId, lastMeterValue, -1, (int)rxData.ampareOut, (int)(rxData.ampareOut*rxData.voltageOut),
                currentOffered, powerOffered, soc, context,
                ocppSessionManager.getOcppConfiguration().MeterValuesSampledData, isInTransaction);
    }

    @Override
    public void onAuthSuccess(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEvent(true);
            }
        }
    }

    @Override
    public void onAuthFailed(int connectorId) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (connectorId == chargeData.curConnectorId) {
                onAuthResultEvent(false);
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
        }
        return ret;
    }

    @Override
    public void onBootNotificationResponse(boolean success) {

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
        if (  flowState == UIFlowState.UI_START ||
                flowState == UIFlowState.UI_CARD_TAG ) {

            mainActivity.setRemoteStartedVisible(View.VISIBLE);
            isRemoteStarted = true;

            if ( flowState == UIFlowState.UI_CARD_TAG ) {
                // 카드 테깅 화면이면 인증을 넘어간다.
                setUIFlowState(UIFlowState.UI_RUN_CHECK);

                // 이미 Run이 된 상태이라면
                if ( isDspChargeRun ) {
                    onDspChargingStartEvent();
                }
                else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManager.changePage(PageID.CONNECT_CAR_WAIT);
                }
            }
            else {
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.PREPARING);
            }
            return true;
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
                }
                else {
                    // 커넥터 상태를 SUSPENDED_EVSE 로 바꾸고 충전을 중지한다.
                    setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.SUSPENDED_EVSE);

                    OCPPConfiguration ocppConfiguration = ocppSessionManager.getOcppConfiguration();
                    // 만약 StopTransactionOnInvalidId 가 true 이면 충전을 중지한다.
                    if ( ocppConfiguration.StopTransactionOnInvalidId == true) {
                        stopReason = StopTransaction.Reason.DE_AUTHORIZED;
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
        else if ( flowState != UIFlowState.UI_START) ret = ReserveNowResponse.Status.OCCUPIED;
        else if ( isDspFault ) ret = ReserveNowResponse.Status.FAULTED;
        else if ( reserveInfo.reservationId > 0 && reserveInfo.reservationId != reservationId ) ret = ReserveNowResponse.Status.REJECTED;
        else {
            reserveInfo.setInfo(reservationId, idTag, parentIdTag, expiryDate);
            mainActivity.setReservedVisible(View.VISIBLE);
            ret = ReserveNowResponse.Status.ACCEPTED;
        }
        return ret;
    }

    @Override
    public void onTriggerMessage(TriggerMessage message) {
        switch( message.getRequestedMessage().toString() ) {
            case "MeterValues":
                DSPRxData rxData = dspControl.getDspRxData(chargeData.dspChannel);
                sendOcppMeterValues(rxData, SampledValue.Context.TRIGGER, false);
                break;
            case "StatusNotification":
                if ( message.getConnectorId() != null ) {
                    ocppSessionManager.SendStatusNotificationRequest(chargeData.curConnectorId, chargeData.ocppStatus, chargeData.ocppStatusError);
                }
                break;
        }
    }


    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        if ( connectorId == chargeData.curConnectorId || connectorId == 0) {
            if ( type == ChangeAvailability.Type.OPERATIVE ) {
                isConnectorOperative = true;
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);

                // 사용불가 화면 숨김
                pageManager.hideUnavailableConView();
            }
            else {
                isConnectorOperative = false;
                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.UNAVAILABLE);

                // 사용불가 화면 첫화면에서만 동작하도록 함
                if ( flowState == UIFlowState.UI_START) pageManager.showUnavailableConView();
            }
        }
    }

    @Override
    public void onResetRequest(boolean isHard) {

    }

    @Override
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval) {

    }

    @Override
    public void onTimeUpdate(Calendar syncTime) {

    }


    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
            onCardTagEvent(rfid, true);
        }

        multiChannelUIManager.rfidReaderRelease(channel);
    }
}
