/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch;

import android.os.Handler;
import android.view.View;

import com.joas.j14_touch_2ch.page.PageEvent;
import com.joas.j14_touch_2ch.page.PageID;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.utils.LogWrapper;

import java.util.Date;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, UpdateManagerListener {
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
    DSPControl2 dspControl;

    J14Touch2chUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_START;
    ChargeData chargeData;
    CPConfig cpConfig;
    LocalMember localMem;

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

    FaultManagerV2 faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    StopTransaction.Reason stopReason = StopTransaction.Reason.LOCAL;
    double powerLimit = -1;

    int dspVersion = 0;

    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();
    int channel;

    public UIFlowManager(int chan, J14Touch2chUIActivity activity, MultiChannelUIManager uiManager, ChargeData data, CPConfig config, DSPControl2 control, PageManager page) {
        channel = chan;
        mainActivity = activity;
        multiChannelUIManager = uiManager;
        chargeData = data;
        cpConfig = config;
        dspControl = control;
        pageManager = page;
        localMem = new LocalMember();

        // FaultManager를 생성한다.
        faultManager = new FaultManagerV2(dspControl, mainActivity, chargeData.dspChannel);

        initDspState();
        //통신 프로그램 시작....// 민수 통신 MinsuComm
    }

    public void setPageManager(PageManager manager) {
        pageManager = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }
    public LocalMember getLocalmember(){return localMem;}
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
                unplugTimerCnt = 0;
                break;
        }
    }

    void initDspState()
    {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
        dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE); // CC타입으로 처음부터 계속 주는것으로 하드웨어팀과 협의 20201217
    }
    void initStartState() {
        initDspState();
//        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
//        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
//        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
//        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
//        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        if ( isConnectorOperative == false ) pageManager.showUnavailableConView();

        // 변수 초기화
        chargeData.measureWh = 0;
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        powerLimit = -1.0d;
        unplugTimerCnt = 0;

        multiChannelUIManager.rfidReaderRelease(channel);

        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;
    }

    public void onPageStartEvent() {
        setUIFlowState(UIFlowState.UI_START);
//        pageManager.changePage(PageID.CONNECTOR_WAIT);
        pageManager.changePage(PageID.SELECT_SLOW);
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
            // Next Flow. Card Tag
            if (cpConfig.isAuthSkip)        //자동인증      20201214
            {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                pageManager.changePage(PageID.CONNECTOR_WAIT);
            }
            else if (multiChannelUIManager.rfidReaderRequest(channel) == false ) {

                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_notice);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_rfid_request_not_ready);
                pageManager.showMessageBox();

                goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                return;
            }
            else
            {
                setUIFlowState(UIFlowState.UI_CARD_TAG);
                pageManager.changePage(PageID.CARD_TAG);
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
//                    ocppSessionManager.authorizeRequest(chargeData.curConnectorId, tagNum);
                    lastCardNum = tagNum;

                    // 승인대기 화면 전환
                    setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                    pageManager.changePage(PageID.AUTH_WAIT);


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
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.CHARGING);

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
            //lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            // 통신으로 충전시작 메시지를 보낸다.
//            ocppSessionManager.startCharging(chargeData.curConnectorId, (int)lastMeterValue);

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManager.changePage(PageID.CHARGING);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        if ( isConnected ) {
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            //if ( getUIFlowState() == UIFlowState.UI_START) {
            if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT) {

                if (cpConfig.isAuthSkip)        //자동인증
                {
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
                else
                {
                    setUIFlowState(UIFlowState.UI_CARD_TAG);
                    pageManager.changePage(PageID.CARD_TAG);

                    // OCPP Session Start
//                ocppSessionManager.startSession(chargeData.curConnectorId);

                    multiChannelUIManager.rfidReaderRequest(channel);
                }
            }
        }
        else {
            //충전중이 아닐때
            if ( getUIFlowState() == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }
            else {
                setUIFlowState(UIFlowState.UI_START);
                //pageManager.changePage(PageID.CONNECTOR_WAIT);
                pageManager.changePage(PageID.SELECT_SLOW);
            }
        }
    }

    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {

            setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
            pageManager.changePage(PageID.FINISH_CHARGING);

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
//            sendOcppMeterValues(rxData, SampledValue.Context.TRANSACTION_END, true);

            //통신으로 종료 패킷을 보낸다.
//            ocppSessionManager.stopCharging(chargeData.curConnectorId, (int)lastMeterValue, stopReason);

            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
        pageManager.getChargingView().onChargingStop();
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수행한다.
     */
    public void timerProcessSec() {
        //Fault 함수 수행
        onFaultEventProcess();

        //Reserve Check
        if ( reserveInfo.expiryCheck() == true ) mainActivity.setReservedVisible(View.INVISIBLE);


        if ( mainActivity.getMeterService() != null ) {
            try {
                long meterVal = mainActivity.getMeterService().readMeterCh(channel);
                double meterVolt = mainActivity.getMeterService().readMeterVoltageCh(channel);
                double meterCurrent = mainActivity.getMeterService().readMeterCurrentCh(channel);

//                try {
//                    jejuEvChargerInfo.meterVal = (int) (meterVal / 10); //kwh 단위 * 2 ( 서버 전송 데이터 )
//                }
//                catch (Exception e1)
//                {}

                //add by si - 20.12.09 - MeterReadError상태 감지추가
                MeterStatusMonitoring(meterVal);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);       //AC 전압
                dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);     //AC 전류
                dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);     //AC Meter 단위 확인 필요


                LogWrapper.d(TAG, "CH:"+ chargeData.dspChannel +", Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if ( meterVal > 0 ) {
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
                    // Meter Error !!!
                    //전력량계 에러 발생 시, 200 - 7 bit true

                }
            }catch(Exception e){
                LogWrapper.d(TAG, "Meter Err:"+e.toString());
            }
        }



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

            chargeData.soc = rxData.batterySOC;
            chargeData.remainTime = rxData.remainTime;
        }
        // Event에서 poll로 바꿈.
        if ( rxData.get400Reg(DSPRxData2.STATUS400.STATE_DOOR) == false ) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true ) {
            //onConnectedCableEvent(true);
        }

        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++> 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
                onPageCommonEvent(PageEvent.GO_HOME);
            }
        }

        // 시작 화면에서 플러그 연결 후, 버튼을 눌렀다면 타이머에 의해 변경된다. 커넥트 연결-인식 시퀀스로 변경한다.
        // by Lee 20201214
        if (isDspPlug && (getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT))
        {
            onConnectedCableEvent(true);
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
                    //충전중 발생했을 경우 충전 중지.
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
                        onChargingStop();
                    }
                    else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_START)  {
                        onPageStartEvent();
                    }
                    //Meter Read error일 경우
                    //dsp로 에러신호 전송
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, true);

                    //fault messagebox 띄우기
                    chargeData.faultBoxContent = "";
                    chargeData.faultBoxContent += "[20007] 전력량계 통신오류 발생(" + String.valueOf(chargeData.dspChannel)+")";
                    chargeData.faultBoxContent += "\r\n";
                    pageManager.showFaultBox();

                } else if (!isMeterCommErr) {
                    //미터기 상태 정상일 경우
                    //dsp 미터에러신호 복구 및 기타변수 초기화
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, false);

                    pageManager.hideFaultBox();
                }
                isMeterCommErr_backup = isMeterCommErr;
            }
        } catch (Exception e) {

        }
    }

    /**
     *  DSP에서 오는 이벤트를 처리한다.
     * @param channel 해당 채널값
     * @param idx 상태값 Index
     * @param val
     */
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

    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false ) {
                // 비상정지버튼은 별도 창을 띄운다
                if (!faultManager.isFaultEmergency(chargeData.dspChannel)) {
                    chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                    chargeData.faultBoxContent += "\r\n";
                }
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
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_START)  {
                    onPageStartEvent();
                }

                if (faultManager.isFaultEmergency(chargeData.dspChannel)) {     //20201215
                    pageManager.showEmergencyBox();
                }
                else {              //20201215
                    fillFaultMessage();
                    pageManager.showFaultBox();
                }
            }
            else {
                if (!isEmergency)       //20201215
                    pageManager.hideEmergencyBox();

                pageManager.hideFaultBox();
            }
            isPreDspFault = isDspFault;
        }

        if (isDspFault == true ) {
            if ( chargeData.ocppStatus != StatusNotification.Status.FAULTED ) {
                // To.Do.. Error Code..Set
//                setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.FAULTED);
            }
        }
        else if ( chargeData.ocppStatus == StatusNotification.Status.FAULTED ) {
            chargeData.ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;
//            setOcppStatus(chargeData.curConnectorId, StatusNotification.Status.AVAILABLE);
        }

        // 긴급버턴 이벤트 발생
//        if ( isEmergencyPressed != isEmergency ) {
//            if (isEmergency == true) {
//                pageManager.showEmergencyBox();
//            } else { // 긴급 버턴 해제
//                pageManager.hideEmergencyBox();
//            }
//            isEmergencyPressed = isEmergency;
//        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        //if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
        //    if ( lastMeterValue < 0) lastMeterValue = meterVal;
        //    int gapMeter = (int)(meterVal - lastMeterValue);
        //    if ( gapMeter < 0) gapMeter = 0;
        //    chargeData.measureWh += gapMeter;
        //    chargeData.chargingCost += ((double)gapMeter/1000.0)*(double)chargeData.chargingUnitCost;
        //}
        //lastMeterValue = meterVal;
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
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        localMem.readCardNum = rfid;
        if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
            onCardTagEvent(rfid, true);
        }

        multiChannelUIManager.rfidReaderRelease(channel);
    }
}
