/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch;

import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.joas.hw.rfid.RfidReaderACM1281S;
import com.joas.j14_touch_2ch.page.AdminPasswordInputView;
import com.joas.j14_touch_2ch.page.JoasCommMonitorView;
import com.joas.j14_touch_2ch.page.JoasDSPMonitorView;
import com.joas.j14_touch_2ch.page.JoasDebugMsgView;
import com.joas.j14_touch_2ch.page.MessageSingleBoxView;
import com.joas.j14_touch_2ch.page.SettingView;
import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;

import java.io.IOException;
import java.util.Calendar;

public class MultiChannelUIManager implements RfidReaderListener, DSPControl2Listener, UpdateManagerListener {
    public static final String TAG = "MultiChannelUIManager";

    J14Touch2chUIActivity mainActivity;

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
    DSPControl2 dspControl;

    boolean isHardResetEvent = false;
    boolean restartBySoftReset = false;

    UpdateManager updateManager;
    int firmwareInstallCounter = 0;

    PageManager[] pageManagers;
    CPConfig cpConfig;

    //RFID Reader
    RfidReader rfidReader;
    int rfidReaderSelect = -1;

    UIFlowManager[] uiFlowManager = new UIFlowManager[TypeDefine.MAX_CHANNEL];

    int lastDateValue = 0;


    public MultiChannelUIManager (J14Touch2chUIActivity activity, PageManager[] pages, ChargeData[] datas, CPConfig config, String restartReason) {
        mainActivity = activity;
        cpConfig = config;
        pageManagers = pages;

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE,10, this);    //meterValueScale 값은 dspv 2.0에서는 전력량계 app통신으로 사용하지 않음
        dspControl.setMeterType(DSPControl.METER_TYPE_SLOW);

        // 미터값을 DSP에서 가져온다.
        dspControl.setMeterUse(false);      //false로 설정, 미터프로그램에서 가져옴
        dspControl.start();

//        rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        //rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_MYFARE);
        rfidReader = new RfidReaderACM1281S("/dev/ttyS3", RfidReaderACM1281S.RFID_CMD.RFID_TMONEY);

        rfidReader.setRfidReaderEvent(this);

        // UpdateManager를 생성한다.
        updateManager = new UpdateManager(mainActivity, this, J14Touch2chUIActivity.uiVersion);

        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i] = new UIFlowManager(i, activity,this,  datas[i], config, dspControl, pageManagers[i]);
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
        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].getDspControl().setDspMonitorListener(joasDSPMonitorView);
        }
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

    void destoryManager() {
        watchDogTimer.stop();
        dspControl.interrupt();
        dspControl.stopThread();

        rfidReader.stopThread();
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
//            rfidReader.rfidReadRelease();
        }
    }

    /**
     * 세팅이 바뀔때 불려지는 이벤트
     * 통신프로그램 파라미터를 수정한다.
     */
    public void onSettingChanged() {
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
                    updateManager.doInstallFirmware(J14Touch2chUIActivity.uiVersion);
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

    @Override
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
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

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        uiFlowManager[rfidReaderSelect].onRfidDataReceive(rfid, success);
    }

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
        //onResetRequest(true);
    }

    @Override
    public void onUpdateStatus(UpdateManager.UpdateState state) {

    }
}
