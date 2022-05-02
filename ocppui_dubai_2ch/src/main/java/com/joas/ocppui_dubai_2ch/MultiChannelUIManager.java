/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 2:00
 *
 */

package com.joas.ocppui_dubai_2ch;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.ocpp.chargepoint.OCPPSession;
import com.joas.ocpp.chargepoint.OCPPSessionManager;
import com.joas.ocpp.chargepoint.OCPPSessionManagerListener;
import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.FirmwareStatusNotification;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.StatusNotification;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.ocpp.msg.TriggerMessage;
import com.joas.ocpp.stack.OCPPStackProperty;
import com.joas.ocppui_dubai_2ch.page.AdminPasswordInputView;
import com.joas.ocppui_dubai_2ch.page.JoasCommMonitorView;
import com.joas.ocppui_dubai_2ch.page.JoasDSPMonitorView;
import com.joas.ocppui_dubai_2ch.page.JoasDebugMsgView;
import com.joas.ocppui_dubai_2ch.page.MessageSingleBoxView;
import com.joas.ocppui_dubai_2ch.page.SettingView;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.io.IOException;
import java.net.URI;
import java.sql.Time;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MultiChannelUIManager implements RfidReaderListener, DSPControlListener, OCPPSessionManagerListener, UpdateManagerListener {
    public static final String TAG = "MultiChannelUIManager";

    OCPPUIActivity mainActivity;

    SettingView settingView;
    JoasCommMonitorView commMonitorView;
    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;
    MessageSingleBoxView messageSingleBoxView;
    AdminPasswordInputView adminPasswordInputView;

    // WatchDog Timer 세팅
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    TimeoutTimer timerSec = null;

    // DSP 관련 Attr
    DSPControl dspControl;

    boolean isHardResetEvent = false;
    boolean restartBySoftReset = false;

    OCPPSessionManager ocppSessionManager;

    UpdateManager updateManager;
    int firmwareInstallCounter = 0;

    PageManager[] pageManagers;
    CPConfig cpConfig;

    //RFID Reader
    RfidReader rfidReader;
    int rfidReaderSelect = -1;

    UIFlowManager[] uiFlowManager = new UIFlowManager[TypeDefine.MAX_CHANNEL];

    int lastDateValue = 0;


    public MultiChannelUIManager (OCPPUIActivity activity, PageManager[] pages, ChargeData[] datas, CPConfig config, String restartReason) {
        mainActivity = activity;
        cpConfig = config;
        pageManagers = pages;

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        dspControl = new DSPControl(TypeDefine.MAX_CHANNEL, "/dev/ttyS2", DSPControl.DSP_VER_REG17_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE,10, this);
        dspControl.setMeterType(DSPControl.METER_TYPE_SLOW);

        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(true);
        dspControl.start();

        //rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
        rfidReader.setRfidReaderEvent(this);

        // UpdateManager를 생성한다.
        updateManager = new UpdateManager(mainActivity, this, OCPPUIActivity.uiVersion);

        initOCPPSession();

        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i] = new UIFlowManager(i, activity,this,  datas[i], config, ocppSessionManager, dspControl, pageManagers[i]);
            pageManagers[i].init(i, uiFlowManager[i], mainActivity);
            uiFlowManager[i].setPageManager(pageManagers[i]);
        }

        initSinglePages();

        // 1초 타이머 시작
        startPeroidTimerSec();
    }

    void initSinglePages() {
        settingView = new SettingView(mainActivity, this, mainActivity);
        commMonitorView = new JoasCommMonitorView(mainActivity, this, mainActivity);
        joasDSPMonitorView = new JoasDSPMonitorView(mainActivity, this, mainActivity);
        joasDebugMsgView = new JoasDebugMsgView(mainActivity, this, mainActivity);
        messageSingleBoxView = new MessageSingleBoxView(mainActivity, this, mainActivity);
        adminPasswordInputView = new AdminPasswordInputView(mainActivity, this, mainActivity);

        RelativeLayout mainLayout = mainActivity.findViewById(R.id.layoutMain);

        mainLayout.addView(settingView);
        settingView.setVisibility(View.INVISIBLE);

        mainLayout.addView(commMonitorView);
        commMonitorView.setVisibility(View.INVISIBLE);

        mainLayout.addView(joasDSPMonitorView);
        joasDSPMonitorView.setVisibility(View.INVISIBLE);

        mainLayout.addView(joasDebugMsgView);
        joasDebugMsgView.setVisibility(View.INVISIBLE);

        mainLayout.addView(messageSingleBoxView);
        messageSingleBoxView.setVisibility(View.INVISIBLE);

        mainLayout.addView(adminPasswordInputView);
        adminPasswordInputView.setVisibility(View.INVISIBLE);

        //DSP MonitorView 등록
        dspControl.setDspMonitorListener(joasDSPMonitorView);

        //commMonitor Listener 등록
        ocppSessionManager.getOcppStack().setTransportMonitorListener(commMonitorView);
    }

    public SettingView getSettingView() { return settingView; }
    public CPConfig getCpConfig() { return cpConfig; }

    public void showSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.onPageActivate();
                settingView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideSettingView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingView.setVisibility(View.INVISIBLE);
                settingView.onPageDeactivate();
            }
        });
    }
    public void showJoasCommMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideJoasCommMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commMonitorView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideJoasDspMonitor() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDSPMonitorView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideJoasDebugView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                joasDebugMsgView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showSingleMessageBoxView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageSingleBoxView.setVisibility(View.VISIBLE);
            }
        });
    }
    public void hideSingleMessageBoxView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageSingleBoxView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void showAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.VISIBLE);
            }
        });
    }
    public boolean isShowAdminPassswordInputView() {
        return adminPasswordInputView.getVisibility() == View.VISIBLE;
    }
    public void hideAdminPasswrodInputView() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adminPasswordInputView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public UIFlowManager getUIFlowManager(int channel) { return uiFlowManager[channel]; }
    public PageManager getPageManager(int channel) { return pageManagers[channel]; }

    public JoasDebugMsgView getJoasDebugMsgView() { return joasDebugMsgView; }
    public JoasCommMonitorView getCommMonitorView() { return commMonitorView; }

    public void onSettingViewExit() {
        hideSettingView();
    }

    public void onAdminPasswordOK(String pwd) {
        if ( pwd.equals(cpConfig.settingPassword) == true ) {
            hideAdminPasswrodInputView();
            showSettingView();
        }
        else {
            Toast.makeText(mainActivity , mainActivity.getResources().getString(R.string.string_password_incorrect), Toast.LENGTH_SHORT).show();
        }
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

    void initOCPPSession() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager = new OCPPSessionManager(mainActivity, TypeDefine.MAX_CHANNEL,Environment.getExternalStorageDirectory().toString()+TypeDefine.REPOSITORY_BASE_PATH, restartBySoftReset);
        ocppSessionManager.init(newOcppProperty);
        ocppSessionManager.setListener(this);
    }

    public OCPPStackProperty loadOcppStackProperty() {
        OCPPStackProperty newOcppProperty = new OCPPStackProperty();

        newOcppProperty.serverUri = cpConfig.serverURI;
        newOcppProperty.cpid = cpConfig.chargerID;
        newOcppProperty.useBasicAuth = cpConfig.useHttpBasicAuth;
        newOcppProperty.authID = cpConfig.httpBasicAuthID;
        newOcppProperty.authPassword = cpConfig.httpBasicAuthPassword;
        return newOcppProperty;
    }

    public OCPPSessionManager getOcppSessionManager() { return ocppSessionManager; }

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();

        rfidReader.stopThread();
        ocppSessionManager.closeManager();
        updateManager.closeManager();
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

    public boolean rfidReaderRequest(int channel) {
        if ( rfidReaderSelect >= 0)  return false;

        rfidReaderSelect = channel;
        rfidReader.rfidReadRequest();
        return true;
    }

    public void rfidReaderRelease(int channel) {
        if ( channel == rfidReaderSelect ) {
            rfidReaderSelect = -1;
            rfidReader.rfidReadRelease();
        }
    }

    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
        OCPPStackProperty newOcppProperty = loadOcppStackProperty();

        ocppSessionManager.restartManager(newOcppProperty);
    }

    //region 원격 업데이트 (Firmware Update) 처리
    /**
     * 업데이트가 진행될때 이벤트 발생(UpdateManager로부터 발생되는 이벤트)
     * @param state
     */
    public void onUpdateStatus(UpdateManager.UpdateState state) {
        switch ( state ) {
            case None:
                break;
            case Waiting:
                break;
            case Started:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADING);
                break;
            case Error:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOAD_FAILED);
                break;
            case Retrying:
                break;
            case Finished:
                ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.DOWNLOADED);
                break;
        }
    }

    /**
     * 1초에 한번식 수행 만약 파일 다운로드 상태라고 하면 충전중이 아닐때 수행됨
     */
    void firmwareInstallProcess() {
        if ( updateManager.getStatus() == UpdateManager.UpdateState.Finished) {
            if ( uiFlowManager[0].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING &&
                    uiFlowManager[1].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING ) {
                firmwareInstallCounter++;
                if ( firmwareInstallCounter == TypeDefine.FIRMWARE_UPDATE_COUNTER  ) {
                    ocppSessionManager.sendFirmwareStatusNotification(FirmwareStatusNotification.Status.INSTALLING);
                    updateManager.doInstallFirmware(OCPPUIActivity.uiVersion);
                }
            }
        }
    }
    //endregion

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {
        //WatchDog 수행
        watchDogTimerProcess();

        //FirmwareInstall 체크
        firmwareInstallProcess();

        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].timerProcessSec();
        }

        checkTransitionDay();
    }

    protected void checkTransitionDay() {
        Calendar curTime = Calendar.getInstance();
        if ( curTime.get(Calendar.DAY_OF_MONTH) != lastDateValue ) {
            lastDateValue = curTime.get(Calendar.DAY_OF_MONTH);

            // 날짜가 봐뀔때 과거 로그를 지운다.
            ocppSessionManager.getOcppStack().getOcppDiagnosticManager().removePastLog();
        }
    }

    public void stopWatdogTimer() {
        watchDogTimerStartCnt = 0;
        watchDogTimer.stopAndClose();
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
     * 전체 시스템 상태(모든 커넥터)를 보낸다.
     * 오직 Available, Unavailable and Faulted 만 보낼 수 있다.(Spec 4.9 Status Notification)
     */
    public void sendNotificationStatusOfSystem() {
        // TODO 현재 Fault인지 아닌지 구별하여 보내야함..
        ocppSessionManager.SendStatusNotificationRequest(0, StatusNotification.Status.AVAILABLE, StatusNotification.ErrorCode.NO_ERROR );

        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].sendNotificationStatusOfSystem();
        }
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
            for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].setStopReason(StopTransaction.Reason.HARD_RESET);
                    uiFlowManager[i].onChargingStop();
                }
            }


            // 약 15초뒤에 Reset됨
            if ( watchDogTimer.isStarted == false ) watchDogTimer.openAndStart(15);
            else watchDogTimer.update();

            // 메시지 박스를 띄운다.
            messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_hard_reset_title));
            messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_hard_reset_content));
            messageSingleBoxView.setMessageTimeout(15); // WatchDog 시간 최대가 16초)
            showSingleMessageBoxView();

        }
        else { // Soft Reset
            // 충전중이라면 충전을 중지한다.
            for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].setStopReason(StopTransaction.Reason.SOFT_RESET);
                    uiFlowManager[i].onChargingStop();
                }
            }

            // 메시지 박스를 띄운다.
            messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_soft_reset_title));
            messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_soft_reset_content));
            messageSingleBoxView.setMessageTimeout(5); // WatchDog 시간 최대가 16초)
            showSingleMessageBoxView();

            runSoftReset(5*1000);
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
                if ( message.getConnectorId() != null ) {
                    uiFlowManager[message.getConnectorId()-1].onTriggerMessage(message);
                }
                break;
            case "StatusNotification":
                if ( message.getConnectorId() == null ) {
                    sendNotificationStatusOfSystem();
                } else {
                    if ( message.getConnectorId() != null ) {
                        uiFlowManager[message.getConnectorId()-1].onTriggerMessage(message);
                    }
                }
                break;
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

    @Override
    public void onDspStatusChange(int channel, DSPRxData.STATUS400 idx, boolean val) {
        uiFlowManager[channel].onDspStatusChange(channel, idx, val);
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        uiFlowManager[channel].onDspMeterChange(channel, meterVal);
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {
        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].onDspCommErrorStstus(isError);
        }
    }

    @Override
    public void onAuthSuccess(int connectorId) {
        uiFlowManager[connectorId-1].onAuthSuccess(connectorId);
    }

    @Override
    public void onAuthFailed(int connectorId) {
        uiFlowManager[connectorId-1].onAuthFailed(connectorId);
    }

    @Override
    public void onChangeState(int connectorId, OCPPSession.SessionState state) {
        uiFlowManager[connectorId-1].onChangeState(connectorId,state);
    }

    @Override
    public CancelReservationResponse.Status onCancelReservation(int reservationId) {
        CancelReservationResponse.Status ret = CancelReservationResponse.Status.REJECTED;
        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            if ( uiFlowManager[i].onCancelReservation(reservationId) == CancelReservationResponse.Status.ACCEPTED) {
                ret = CancelReservationResponse.Status.ACCEPTED;
                break;
            }
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
        uiFlowManager[connectorId-1].onRemoteStopTransaction(connectorId);
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
        return uiFlowManager[connectorId-1].onRemoteStartTransaction(connectorId, idTag, chargingProfile);
    }

    /**
     * StartTranscation의 IdTagInfo 상태를 받았을 때처리함.
     * @param connectorId
     * @param tagInfo
     */
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo) {
        uiFlowManager[connectorId-1].onStartTransactionResult(connectorId, tagInfo);
    }

    @Override
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId) {
        return uiFlowManager[connectorId-1].onReserveNow(connectorId, expiryDate, idTag, parentIdTag, reservationId);
    }

    @Override
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type) {
        if ( connectorId == 0 ) {
            for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
                uiFlowManager[i].onChangeAvailability(connectorId, type);
            }
        }
        else {
            uiFlowManager[connectorId - 1].onChangeAvailability(connectorId, type);
        }
    }

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        uiFlowManager[rfidReaderSelect].onRfidDataReceive(rfid, success);
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
}
