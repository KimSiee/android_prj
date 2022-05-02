/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 8:38
 *
 */

package com.joas.joasui_iot_video.ui;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.joas.evcomm.EvCommManager;
import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;

import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.joasui_iot_video.ui.page.PageEvent;
import com.joas.joasui_iot_video.ui.page.PageID;
import com.joas.utils.ConvertUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.sql.Time;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UIFlowManager implements RfidReaderListener, DSPControlListener {
    public static final String TAG = "UIFlowManager";

    public enum UIFlowState {
        UI_START,
        UI_SELECT,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING
    }

    PageManger pageManger;

    IOTVideoActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_START;
    ChargeData chargeData;
    CPConfig cpConfig;
    EvCommManager commManager = null;

    //RFID Reader
    RfidReader rfidReader;

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
    boolean isDspDspFault = false;

    long lastMeterValue = 0;

    TimeoutTimer timerSec = null;
    String lastCardNum = "0000000000000000";


    public UIFlowManager(IOTVideoActivity activity, ChargeData data, CPConfig config) {
        mainActivity = activity;
        chargeData = data;
        cpConfig = config;

        String basePath = Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH;
        //commManager = new EvCommManager(activity.getBaseContext(), cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID, cpConfig.chargerID, chargeData.chargerType, basePath);

        if ( cpConfig.isFastCharger == true ) {
            dspControl = new DSPControl(1, "/dev/ttyS2", DSPControl.DSP_VER_REG23_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE, 10, this);
        }
        else {
            dspControl = new DSPControl(1, "/dev/ttyS2", DSPControl.DSP_VER_REG17_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE,1, this);
        }
        dspControl.start();

        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        rfidReader.setRfidReaderEvent(this);

        dspControl.setMeterUse(true);

        // 1초 타이머 시작
        startPeroidTimerSec();
    }

    public void setPageManager(PageManger manager) {
        pageManger = manager;
    }
    public void setJoasCommManager(EvCommManager manager) {
        commManager = manager;
    }
    public EvCommManager getJoasCommManager() {
        return commManager;
    }
    public DSPControl getDspControl() { return dspControl; }

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

    public UIFlowState getUIFlowState() { return flowState; }

    void setUIFlowState(UIFlowState state) {
        flowState = state;

        processChangeState(flowState );
    }

    public void destory() {
        dspControl.interrupt();
        dspControl.stopThread();
//        commManager.interrupt();
        rfidReader.destory();
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
            case UI_START:
                initStartState();
                break;

            case UI_SELECT:
                //UI_SELECT 가 첫화면일때
                if ( cpConfig.isStartVideo == false ) initStartState();

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

    public void onPageStartEvent() {
        // Next Flow. Card Tag
        setUIFlowState(UIFlowState.UI_SELECT);
        if ( cpConfig.isFastCharger ) {
            pageManger.changePage(PageID.SELECT_FAST);
        }
        else {
            pageManger.changePage(PageID.SELECT_SLOW);
        }
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

            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_SLOW_BTYPE);
                break;

            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData.CHARGER_SELECT_SLOW_CTYPE);
                break;
        }

        // 자동인증인경우 인증 과정 생략
        if ( cpConfig.isAuthSkip == true ) {
            lastCardNum = "0000000000000000";
            setUIFlowState(UIFlowState.UI_AUTH_WAIT);
            onAuthResultEvent(true);
        }
        else {
            // Next Flow. Card Tag
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManger.changePage(PageID.CARD_TAG);
        }
    }

    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                goHomeProcess();
                break;
        }
    }

    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            //commManager.sendAuthReq_a1(tagNum);
            lastCardNum = tagNum;

            // 승인대기 화면 전환
            setUIFlowState(UIFlowState.UI_AUTH_WAIT);
            pageManger.changePage(PageID.AUTH_WAIT);
        }
    }

    // Home으로 돌아간다.
    public void goHomeProcess() {
        if ( cpConfig.isStartVideo ) {
            setUIFlowState(UIFlowState.UI_START);
            pageManger.changePage(PageID.START);
        }
        else {
            onPageStartEvent();
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
                        goHomeProcess();
                    }
                }, timeout);
            }
        });
    }

    public void onAuthResultEvent(boolean isSuccess) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (isSuccess) {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);

                // 이미 Connect 된 상태이라면
                if ( isDspPlug ) {
                    onConnectedCableEvent(true);
                } else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManger.changePage(PageID.CONNECTOR_WAIT);
                }

            } else {
                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = "인증 실패";
                chargeData.messageBoxContent = "카드 인증에 실패하였습니다.\r\n확인하시고 다시 시도해 주시기 바랍니다.";
                pageManger.getAuthWaitView().stopTimer();
                pageManger.showMessageBox();

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

            // 통신으로 충전시작 메시지를 보낸다.
            sendCommChargeStart();

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

            //통신으로 종료 패킷을 보낸다.
            sendCommChargeEnd();
        }
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData.STATUS200.FINISH_CHARGE, true);

        // 종료이벤트 발생
        onFinishChargingEvent();
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

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();

            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            if ( chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD ) == 0) {
                sendCommChargeState();
            }
        }

        if ( mainActivity.getMeterService() != null ) {
            try {
                long start = System.nanoTime();
                long meterVal = mainActivity.getMeterService().readMeter();
                long end = System.nanoTime();
                if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                    int gapMeter = (int) (meterVal - lastMeterValue);
                    chargeData.measureWh += (meterVal - lastMeterValue);
                    chargeData.chargingCost += ((double) gapMeter / 1000.0) * (double) chargeData.chargingUnitCost;
                }
                lastMeterValue = meterVal;
                LogWrapper.d(TAG, "Meter:"+lastMeterValue+", time:"+(end-start));
            }catch(Exception e){
                LogWrapper.d(TAG, "Meter Err:"+e.toString());
            }
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
        commManager.setCommSetting(cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID, cpConfig.chargerID, chargeData.chargerType);
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
                    isDspDspFault = val;
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
        /*
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            int gapMeter = (int)(meterVal - lastMeterValue);
            chargeData.measureWh += (meterVal - lastMeterValue);
            chargeData.chargingCost += ((double)gapMeter/1000.0)*(double)chargeData.chargingUnitCost;
        }
        lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );

        */
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {

    }

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        if (flowState == UIFlowState.UI_CARD_TAG) {
            onCardTagEvent(rfid, true);
        }
    }

    //================================================
    // Comm 이벤트 송신
    //================================================
    public void sendCommChargeStart() {
        JoasCommMsg.ChargerStart_d1 msg = new JoasCommMsg.ChargerStart_d1();

        msg.cpMode = chargeData.cpMode;
        msg.cpStatus = chargeData.cpStatus;
        msg.meterVal = (int)(lastMeterValue/10);
        msg.cardNum = lastCardNum;
        msg.reqAmountSel = 0x01; // FULL;
        msg.payMethod = 0x02; // 전기요금 합산

        //commManager.sendChargeStart_d1(msg.encode());
    }

    public void sendCommChargeState() {
        JoasCommMsg.ChargerState_e1 msg = new JoasCommMsg.ChargerState_e1();

        msg.cpMode = chargeData.cpMode;
        msg.cpStatus = chargeData.cpStatus;
        msg.meterVal = (int)(lastMeterValue/10);
        msg.cardNum = lastCardNum;
        msg.reqAmountSel = 0x01; // FULL;
        msg.payMethod = 0x02; // 전기요금 합산

        msg.curChargingKwh = (int)(chargeData.measureWh/10);
        msg.curChargingCost = (int)chargeData.chargingCost;
        msg.curChargingUnitCost = chargeData.chargingUnitCost;

        //commManager.sendChargeState_e1(msg.encode());
    }

    public void sendCommChargeEnd() {
        JoasCommMsg.ChargerEnd_f1 msg = new JoasCommMsg.ChargerEnd_f1();

        msg.cpMode = chargeData.cpMode;
        msg.cpStatus = chargeData.cpStatus;
        msg.meterVal = (int)(lastMeterValue/10);
        msg.cardNum = lastCardNum;
        msg.payMethod = 0x02; // 전기요금 합산

        msg.chargingKwh = (int)(chargeData.measureWh/10);
        msg.chargingCost = (int)chargeData.chargingCost;

        msg.chargeEndStatus = 0x01; // 정상

        msg.chargingTime = ConvertUtil.secTimeToBCDWord((int)(chargeData.chargingTime/1000));
        msg.chargeStartTime = ConvertUtil.DateTimeToBCDLong(chargeData.chargeStartTime);

        //commManager.sendChargeEnd_f1(msg.encode());
    }


    //================================================
    // Comm 이벤트 수신
    //================================================

    /*

    @Override
    public void onTimeUpdate(Date syncTime) {
        Date curTime = new Date();
        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);

        // 현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTime() - syncTime.getTime()) > TypeDefine.TIME_SYNC_GAP_MS) {
            am.setTime(syncTime.getTime());
            LogWrapper.v(TAG, "TimeSync : "+syncTime.toString());
        }
    }

    @Override
    public byte[] requestStatusVDData() {
        int idx = 0;
        byte[] vdData = new byte[EvCommDefine.VD_DATA_SIZE_STATUS];
        ByteUtil.wordToByteArray(chargeData.cpMode , vdData, idx);
        idx += 2;

        ByteUtil.longToByteArray(chargeData.cpStatus, vdData, 8, idx);
        idx += 8;

        int meterValInt = (int)(chargeData.measureWh/10); //
        ByteUtil.intToByteArray(meterValInt, vdData, idx);
        idx += 4;
        return vdData;
    }

    @Override
    public byte[] reqeustVD_b1() {
        return requestStatusVDData();
    }

    @Override
    public byte[] reqeustVD_A1() {
        return requestStatusVDData();
    }

    @Override
    public void onAuthResp_1a(byte[] vdData) {
        if ( vdData[EvCommDefine.VD_DATA_AUTH_RET_POS] == EvCommDefine.AUTH_SUCCESS  ) {
            onAuthResultEvent(true);
        }
        else {
            onAuthResultEvent(false);
        }
    }
    */
}
