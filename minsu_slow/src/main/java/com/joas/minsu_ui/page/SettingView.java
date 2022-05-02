/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPRxData2;
import com.joas.minsu_ui.PageManger;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.TypeDefine;
import com.joas.minsu_ui.UIFlowManager;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class SettingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer = null;

    TextView tvStationID;
    TextView tvCPID;
    TextView tvServerIP;
    TextView tvServerPort;
    TextView tvPassword;

    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingIsFaster;
    Switch switchSettingIsTestMode;

    TextView tvVersion;
    TextView tvFWVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;
    LinearLayout layoutContentView;
    TextView tvContentView;
    TextView tvCostUnitInfo;

    public SettingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_setting, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateStatus();
            }
        });
    }

    void initComponents() {
        tvStationID      = (TextView)findViewById(R.id.textSettingStationID);
        tvCPID  = (TextView)findViewById(R.id.textSettingChargerID);
        tvServerIP = (TextView)findViewById(R.id.textSettingServerIP);
        tvServerPort = (TextView)findViewById(R.id.textSettingServerPort);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvFWVersion = (TextView)findViewById(R.id.textSettingFWVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);
        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);
        switchSettingIsFaster = (Switch)findViewById(R.id.switchSettingIsFaster);
        switchSettingIsTestMode = (Switch)findViewById(R.id.switchSettingIsTestMode);

        layoutContentView = findViewById(R.id.layoutContentView);
        tvContentView = findViewById(R.id.tvContentView);
        tvCostUnitInfo = findViewById(R.id.textSettingUnitcost);

        initButtonEvent();
    }

    void initButtonEvent() {
        Button btLoadTest = (Button)findViewById(R.id.btLoadTest);
        btLoadTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadTestStart();
            }
        });

        Button btExit = (Button) findViewById(R.id.btSettingExit);
        btExit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onExitClick();
            }
        });

        Button btSave = (Button) findViewById(R.id.btSettingSave);
        btSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveClick();
            }
        });

        Button btHideKeyboard = (Button) findViewById(R.id.btSettingHideKeyboard);
        btHideKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHideKeyboard();
            }
        });

        Button  btSettingCommMNT = (Button) findViewById(R.id.btSettingCommMNT);
        btSettingCommMNT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommMntShow();
            }
        });

        Button btFinishApp = (Button) findViewById(R.id.btFinishApp);
        btFinishApp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommFinishApp();
            }
        });

        Button  btSettingUpdateTest = (Button) findViewById(R.id.btSettingUpdateTest);
        btSettingUpdateTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommUpdateTest();
            }
        });

        Button  btSettingDspMNT  = (Button) findViewById(R.id.btSettingDspMNT);
        btSettingDspMNT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDspMntViewShow();
            }
        });

        Button btSettingDebugView = (Button) findViewById(R.id.btSettingDebugView);
        btSettingDebugView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDspDebugViewShow();
            }
        });

        //for Test
        Button  btTestErr = (Button)findViewById(R.id.btTestErr);
        btTestErr.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommTestErr();
            }
        });

        Button btContentViewClose = findViewById(R.id.btContentViewClose);
        btContentViewClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutContentView.setVisibility(INVISIBLE);
            }
        });
    }

    void onLoadTestStart(){
        PageManger pm = flowManager.getPageManager();
        pm.showLoadTestView();
    }

    void onExitClick() {
        onHideKeyboard();

        this.setVisibility(View.INVISIBLE);
    }

    void saveCPConfig() {
        boolean oldUseWatchDog = flowManager.getCpConfig().useWatchDogTimer;
        flowManager.getCpConfig().stationID = tvStationID.getText().toString();
        flowManager.getCpConfig().chargerID = tvCPID.getText().toString();
        flowManager.getCpConfig().serverIP = tvServerIP.getText().toString();
        flowManager.getCpConfig().serverPort = Integer.parseInt(tvServerPort.getText().toString());
        flowManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        flowManager.getCpConfig().isFastCharger = switchSettingIsFaster.isChecked();
        flowManager.getCpConfig().isTestMode = switchSettingIsTestMode.isChecked();
        flowManager.getCpConfig().settingUnitCost = Integer.valueOf(tvCostUnitInfo.getText().toString());
        flowManager.getCpConfig().saveConfig(mainActivity);

        flowManager.onSettingChanged();
        if ( oldUseWatchDog != flowManager.getCpConfig().useWatchDogTimer ) {
            if (flowManager.getCpConfig().useWatchDogTimer != false) flowManager.stopWatdogTimer();
        }
    }

    void loadCPConfig() {
        flowManager.getCpConfig().loadConfig(mainActivity);

        tvStationID.setText(flowManager.getCpConfig().stationID);
        tvCPID.setText(flowManager.getCpConfig().chargerID);
        tvServerIP.setText(flowManager.getCpConfig().serverIP);
        tvServerPort.setText("" + flowManager.getCpConfig().serverPort);

        tvPassword.setText(flowManager.getCpConfig().settingPassword);
        switchSkipAuth.setChecked(flowManager.getCpConfig().isAuthSkip);
        switchSettingWatchDogUse.setChecked(flowManager.getCpConfig().useWatchDogTimer);
        switchSettingIsFaster.setChecked(flowManager.getCpConfig().isFastCharger);
        switchSettingIsTestMode.setChecked(flowManager.getCpConfig().isTestMode);
        tvCostUnitInfo.setText(""+flowManager.getCpConfig().settingUnitCost);
    }

    void statusUpdate() {
        String localIP = NetUtil.getLocalIpAddress();
        if (localIP != null) tvSettingLocalIP.setText(localIP);
    }

    void onHideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    public void onSaveClick() {
        final Context context = this.getContext();
        new AlertDialog.Builder(mainActivity)
                .setTitle("설정 저장")
                .setMessage("설정을 저장하시겠습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        saveCPConfig();
                        Toast.makeText(context, "세팅 내용이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(context, "취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void onCommMntShow() {
        flowManager.showJoasCommMonitor();
    }

    public void onDspMntViewShow()  {
        flowManager.showDspMonitor();
    }

    public void onDspDebugViewShow() {
        flowManager.showDebugView();
    }

    public void onCommFinishApp() {
        flowManager.onFinishApp();
    }

    public void onCommUpdateTest() {
        flowManager.stopWatdogTimer();
        RemoteUpdater updater = new RemoteUpdater(mainActivity, Environment.getExternalStorageDirectory()+"/Update", "update.apk");
        updater.doUpdateFromApk("com.joas.smartcharger");
    }

    public void onCommTestErr() {
        TextView a = null;
        a.setText("abcd");
    }


    void updateStatus() {
        tvLastErrorMsg.setText(LogWrapper.lastErrorMessage);
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timer.start();
    }


    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        loadCPConfig();
        statusUpdate();
        onHideKeyboard();
        startTimer();

        //sw version
        tvVersion.setText(TypeDefine.SW_VERSION+"("+TypeDefine.SW_RELEASE_DATE+")");
        //fw version
        DSPRxData2 rxData = flowManager.getDspControl().getDspRxData2(0);
        int dspVersion = rxData.version; // DSP 버전 정보 저장
        tvFWVersion.setText("" + dspVersion);
    }

    @Override
    public void onPageDeactivate() {
        if ( timer != null ) timer.cancel();
    }
}
