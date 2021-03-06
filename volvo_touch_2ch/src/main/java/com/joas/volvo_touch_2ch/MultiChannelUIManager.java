/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. μ€ν 4:36
 *
 */

package com.joas.volvo_touch_2ch;

import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.rfid.RfidReader;
import com.joas.hw.rfid.RfidReaderACM1281S;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.hw.rfid.RfidReaderSehan;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.WatchDogTimer;
import com.joas.volvo_touch_2ch.page.AdminPasswordInputView;
import com.joas.volvo_touch_2ch.page.JoasCommMonitorView;
import com.joas.volvo_touch_2ch.page.JoasDSPMonitorView;
import com.joas.volvo_touch_2ch.page.JoasDebugMsgView;
import com.joas.volvo_touch_2ch.page.MessageSingleBoxView;
import com.joas.volvo_touch_2ch.page.SettingView;
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;
import com.joas.volvo_touch_2ch_comm.VolvoCommManagerListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MultiChannelUIManager implements RfidReaderListener, DSPControl2Listener, UpdateManagerListener, VolvoCommManagerListener{
    public static final String TAG = "MultiChannelUIManager";

    VolvoTouch2chUIActivity mainActivity;

    SettingView settingView;
    JoasCommMonitorView commMonitorView;
    JoasDSPMonitorView joasDSPMonitorView;
    JoasDebugMsgView joasDebugMsgView;
    MessageSingleBoxView messageSingleBoxView;
    AdminPasswordInputView adminPasswordInputView;

    // WatchDog Timer μΈν
    int watchDogTimerStartCnt = 0;
    WatchDogTimer watchDogTimer = new WatchDogTimer();

    TimeoutTimer timerSec = null;

    // DSP κ΄λ ¨ Attr
    DSPControl2 dspControl;

    boolean isHardResetEvent = false;
    boolean restartBySoftReset = false;

    UpdateManager updateManager;
    int firmwareInstallCounter = 0;

    PageManager[] pageManagers;
    CPConfig cpConfig;
    MeterConfig meterConfig;

    //RFID Reader
    RfidReader rfidReader;
    int rfidReaderSelect = -1;

    UIFlowManager[] uiFlowManager = new UIFlowManager[TypeDefine.MAX_CHANNEL];

    int lastDateValue = 0;



    public MultiChannelUIManager (VolvoTouch2chUIActivity activity, PageManager[] pages, ChargeData[] datas, CPConfig config, MeterConfig mconfig, String restartReason) {
        mainActivity = activity;
        cpConfig = config;
        pageManagers = pages;
        meterConfig = mconfig;

        if ( restartReason != null && restartReason.equals("SoftReset") ) restartBySoftReset = true;

        dspControl = new DSPControl2(TypeDefine.MAX_CHANNEL, "/dev/ttyS2", DSPControl2.DSP_VER_REG17_SIZE, DSPControl2.DSP_VER_TXREG_DEFAULT_SIZE,10, this);    //meterValueScale κ°μ dspv 2.0μμλ μ λ ₯λκ³ appν΅μ μΌλ‘ μ¬μ©νμ§ μμ
        dspControl.setMeterType(DSPControl.METER_TYPE_SLOW);

        // λ―Έν°κ°μ DSPμμ κ°μ Έμ¨λ€.
        dspControl.setMeterUse(false);      //falseλ‘ μ€μ , λ―Έν°νλ‘κ·Έλ¨μμ κ°μ Έμ΄
        dspControl.start();

        if(cpConfig.useACMRF){
            rfidReader = new RfidReaderACM1281S("/dev/ttyS3", RfidReaderACM1281S.RFID_CMD.RFID_TMONEY); //ACM μΉ΄λλ¦¬λκΈ°
        }
        else{
            rfidReader = new RfidReaderSehan("/dev/ttyS3", RfidReaderSehan.RFID_CMD.RFID_AUTO_TMONEY);
        }

        rfidReader.setRfidReaderEvent(this);

        // UpdateManagerλ₯Ό μμ±νλ€.
        updateManager = new UpdateManager(mainActivity, this, VolvoTouch2chUIActivity.uiVersion);

        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i] = new UIFlowManager(i, activity,this,  datas[i], config, meterConfig, dspControl, pageManagers[i]);
            pageManagers[i].init(i, uiFlowManager[i], mainActivity);
            uiFlowManager[i].setPageManager(pageManagers[i]);
        }

        initSinglePages();

        // 1μ΄ νμ΄λ¨Έ μμ
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

        //DSP MonitorView λ±λ‘
        for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
            uiFlowManager[i].getDspControl().setDspMonitorListener(joasDSPMonitorView);
        }

        //COMM MonitorView λ±λ‘
        for(int i=0;i<TypeDefine.MAX_CHANNEL;i++){
            uiFlowManager[i].getCommManager().setMonitorListener(commMonitorView);
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
     * μΈνμ΄ λ°λλ λΆλ €μ§λ μ΄λ²€νΈ
     * ν΅μ νλ‘κ·Έλ¨ νλΌλ―Έν°λ₯Ό μμ νλ€.
     */
    public void onSettingChanged() {
    }



//    /**
//     * 1μ΄μ νλ²μ μν λ§μ½ νμΌ λ€μ΄λ‘λ μνλΌκ³  νλ©΄ μΆ©μ μ€μ΄ μλλ μνλ¨
//     */
//    void firmwareInstallProcess() {
//        if ( updateManager.getStatus() == UpdateManager.UpdateState.Finished) {
//            if ( uiFlowManager[0].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING &&
//                    uiFlowManager[1].getUIFlowState() != UIFlowManager.UIFlowState.UI_CHARGING ) {
//                firmwareInstallCounter++;
//                if ( firmwareInstallCounter == TypeDefine.FIRMWARE_UPDATE_COUNTER  ) {
//                    updateManager.doInstallFirmware(VolvoTouch2chUIActivity.uiVersion);
//                }
//            }
//        }
//    }
    //endregion

    /**
     * 1μ΄μ νλ²μ© μ€νλλ©΄μ νμν μΌμ μνΈνλ€.
     */
    public void timerProcessSec() {
        //WatchDog μν
        watchDogTimerProcess();

//        //FirmwareInstall μ²΄ν¬
//        firmwareInstallProcess();

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

    /**
     * Appμ μ¬μμνλ€.
     */
    public void runSoftReset(int timeout) {
        //Soft ResetμΈ κ²½μ° νλ©΄μ μ΄κΈ°ν νλ€.
        TimeoutTimer timer = new TimeoutTimer(timeout, new TimeoutHandler() {
            @Override
            public void run() {
                // App μ¬μμ
                Intent i = mainActivity.getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(mainActivity.getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("RestartReason", "SoftReset");
                mainActivity.startActivity(i);
            }
        });
        timer.startOnce();
    }

    public void stopWatdogTimer() {
        watchDogTimerStartCnt = 0;
        watchDogTimer.stopAndClose();
    }

    // 1μ΄μ νλ²μ© WatchDog νμ΄λ¨Έλ₯Ό μννλ€.
    public void watchDogTimerProcess() {
        // μ€μ μ WatchDogλ₯Ό μ¬μ©νμ§ μκ±°λ HardResetλ©μμ§λ₯Ό λ°λκ²½μ°μλ Skipν¨
        if ( cpConfig.useWatchDogTimer == false || isHardResetEvent == true ) return;

        if ( watchDogTimerStartCnt >= TypeDefine.WATCHDOG_START_TIMEOUT) {
            if ( watchDogTimerStartCnt == TypeDefine.WATCHDOG_START_TIMEOUT) {
                // WatchDog νμ΄λ¨Έ μμ(openκ³Ό ν¨κ» μμ)
                watchDogTimer.openAndStart(WatchDogTimer.WATCHDOG_MAX_UPDATE_TIMEOUT);
                watchDogTimerStartCnt++; // μ΄νλ²ν°λ updateλ§ μ€ν
            }
            else {
                //Watch Dog Timer κ°±μ 
                watchDogTimer.update();
                //Log.v(TAG, "WatchDog Update..");
            }
        }
        else {
            watchDogTimerStartCnt++;
        }
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
    // RFID μ΄λ²€νΈ μμ 
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        uiFlowManager[rfidReaderSelect].onRfidDataReceive(rfid, success);
    }

    public void onResetRequest(boolean isHard) {
        if ( isHard == true ) {
            isHardResetEvent = true;

            // μΆ©μ μ€μ΄λΌλ©΄ μΆ©μ μ μ€μ§νλ€.
            for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].onChargingStop();
                }
            }


            // μ½ 15μ΄λ€μ Resetλ¨
            if ( watchDogTimer.isStarted == false ) watchDogTimer.openAndStart(15);
            else watchDogTimer.update();

            // λ©μμ§ λ°μ€λ₯Ό λμ΄λ€.
            messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_hard_reset_title));
            messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_hard_reset_content));
            messageSingleBoxView.setMessageTimeout(15); // WatchDog μκ° μ΅λκ° 16μ΄)
            showSingleMessageBoxView();

        }
        else { // Soft Reset
            // μΆ©μ μ€μ΄λΌλ©΄ μΆ©μ μ μ€μ§νλ€.
            for (int i=0; i<TypeDefine.MAX_CHANNEL; i++) {
                if (uiFlowManager[i].getUIFlowState() == UIFlowManager.UIFlowState.UI_CHARGING) {
                    uiFlowManager[i].onChargingStop();
                }
            }

            // λ©μμ§ λ°μ€λ₯Ό λμ΄λ€.
            messageSingleBoxView.setSingleMessageBoxTitle(mainActivity.getResources().getString(R.string.str_soft_reset_title));
            messageSingleBoxView.setSingleMessageBoxContent(mainActivity.getResources().getString(R.string.str_soft_reset_content));
            messageSingleBoxView.setMessageTimeout(5); // WatchDog μκ° μ΅λκ° 16μ΄)
            showSingleMessageBoxView();

            runSoftReset(5*1000);
        }
    }

    //================================================
    // μ΄κΈ°ν(ν©ν λ¦¬λ¦¬μ)
    //================================================
    public void doFactoryReset() {
        // IP μΈν
        NetUtil.configurationStaticIP("192.168.0.230", "255.255.255.0", "192.168.0.1", "8.8.8.8");

        // BaseDirectory μλ λ°μ΄ν° λͺ¨λ μ­μ .
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

    @Override
    public void onSystemTimeUpdate(Date d) {
        uiFlowManager[0].onSystemTimeUpdate(d);
        uiFlowManager[1].onSystemTimeUpdate(d);
    }

    @Override
    public void onCommConnected(int ch) {
        uiFlowManager[ch].onCommConnected(ch);
    }

    @Override
    public void onCommDisconnected(int ch) {
        uiFlowManager[ch].onCommDisconnected(ch);
    }

    @Override
    public void onAuthResultEvent(boolean isSuccess, int reason, int costUnit, int ch) {
        uiFlowManager[ch].onAuthResultEvent(isSuccess, reason, costUnit, ch);
    }

    @Override
    public void onResetRequest(int kind) {
        uiFlowManager[0].onResetRequest(kind);
    }

    @Override
    public void onChargerInfoChangeReq(int kind, String value, int ch) {
        uiFlowManager[ch].onChargerInfoChangeReq(kind, value, ch);
    }

    @Override
    public boolean onRemoteStartStop(int cmd, int ch) {
        boolean ret;
        ret = uiFlowManager[ch].onRemoteStartStop(cmd, ch);
        return ret;
    }

    @Override
    public void onRecvStartChargingResp(byte rspCode, byte rspReason, int ch) {
        uiFlowManager[ch].onRecvStartChargingResp(rspCode,rspReason,ch);
    }

    @Override
    public void onRecvFinishChargingResp(int cost, int ch) {
        uiFlowManager[ch].onRecvFinishChargingResp(cost,ch);
    }

    @Override
    public void onRecvVersionResp(String version, int ch) {
        uiFlowManager[0].onRecvVersionResp(version,ch);
    }

    @Override
    public void onFirmwareDownCompleted(byte[] fwDownData, int ch) {
        uiFlowManager[0].onFirmwareDownCompleted(fwDownData,ch);
    }

    @Override
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url, int ch) {

        if (!url.equals("")) {
            uiFlowManager[ch].onReqInfoDownByHttp(dest1,dest2,url,ch);
            return true;
        }
        else return false;
    }

    @Override
    public void onChangeChargerMode(int mode, int ch) {
        uiFlowManager[ch].onChangeChargerMode(mode,ch);
    }


    @Override
    public void onRecvCostInfo(Volvo2chChargerInfo pinfo, int ch) {
        uiFlowManager[0].onRecvCostInfo(pinfo,ch);
    }

    @Override
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }

    @Override
    public void onRecvMissingPaymentCompleteResp(byte authResult, String prepayApprovalnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }
}
