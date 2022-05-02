/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_dubai_2ch.page;

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

import com.joas.ocppui_dubai_2ch.MultiChannelUIManager;
import com.joas.ocppui_dubai_2ch.R;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class SettingView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager uiManager;
    Activity mainActivity;
    TimeoutTimer timer = null;

    TextView tvCPID;
    TextView tvServerURI;
    TextView tvServerBasicAuthID;
    TextView tvServerBasicAuthPassword;
    TextView tvPassword;
    Switch switchUseBasicAuth;

    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingIsFaster;

    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;

    LinearLayout layoutContentView;
    TextView tvContentView;

    public SettingView(Context context, MultiChannelUIManager manager, Activity activity) {
        super(context);
        uiManager = manager;
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
        tvCPID      = (TextView)findViewById(R.id.textSettingChargerID);
        tvServerURI  = (TextView)findViewById(R.id.textSettingServerURI);
        tvServerBasicAuthID = (TextView)findViewById(R.id.textSettingBasicAuthID);
        tvServerBasicAuthPassword = (TextView)findViewById(R.id.textSettingBasicAuthPassword);
        switchUseBasicAuth = (Switch)findViewById(R.id.switchSettingBasicAuth);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);
        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);
        switchSettingIsFaster = (Switch)findViewById(R.id.switchSettingIsFaster);

        layoutContentView = findViewById(R.id.layoutContentView);
        tvContentView = findViewById(R.id.tvContentView);

        initButtonEvent();
    }

    void initButtonEvent() {
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

        Button btViewOcppConfig = findViewById(R.id.btViewOcppConfig);
        btViewOcppConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewOcppConfig();
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

    void onExitClick() {
        onHideKeyboard();

        uiManager.onSettingViewExit();
    }

    void saveCPConfig() {
        boolean oldUseWatchDog = uiManager.getCpConfig().useWatchDogTimer;
        uiManager.getCpConfig().chargerID = tvCPID.getText().toString();
        uiManager.getCpConfig().serverURI = tvServerURI.getText().toString();
        uiManager.getCpConfig().httpBasicAuthID = tvServerBasicAuthID.getText().toString();
        uiManager.getCpConfig().httpBasicAuthPassword = tvServerBasicAuthPassword.getText().toString();
        uiManager.getCpConfig().useHttpBasicAuth = switchUseBasicAuth.isChecked();

        uiManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        uiManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        uiManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        uiManager.getCpConfig().isFastCharger = switchSettingIsFaster.isChecked();
        uiManager.getCpConfig().saveConfig(mainActivity);

        /*
        uiManager.onSettingChanged();
        if ( oldUseWatchDog != uiManager.getCpConfig().useWatchDogTimer ) {
            if (uiManager.getCpConfig().useWatchDogTimer != false) uiManager.stopWatdogTimer();
        }
        */
    }

    void loadCPConfig() {
        uiManager.getCpConfig().loadConfig(mainActivity);

        tvCPID.setText(uiManager.getCpConfig().chargerID);
        tvServerURI.setText(uiManager.getCpConfig().serverURI);
        tvServerBasicAuthID.setText(uiManager.getCpConfig().httpBasicAuthID);
        tvServerBasicAuthPassword.setText(uiManager.getCpConfig().httpBasicAuthPassword);
        switchUseBasicAuth.setChecked(uiManager.getCpConfig().useHttpBasicAuth);

        tvPassword.setText(uiManager.getCpConfig().settingPassword);
        switchSkipAuth.setChecked(uiManager.getCpConfig().isAuthSkip);
        switchSettingWatchDogUse.setChecked(uiManager.getCpConfig().useWatchDogTimer);
        switchSettingIsFaster.setChecked(uiManager.getCpConfig().isFastCharger);
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
        uiManager.showJoasCommMonitor();
    }

    public void onDspMntViewShow()  {
        uiManager.showJoasDspMonitor();
    }

    public void onDspDebugViewShow() {
        uiManager.showJoasDebugView();
    }

    public void onCommFinishApp() {
        uiManager.onFinishApp();
    }

    public void onCommUpdateTest() {
        uiManager.stopWatdogTimer();
        RemoteUpdater updater = new RemoteUpdater(mainActivity, Environment.getExternalStorageDirectory()+"/Update", "update.apk");
        updater.doUpdateFromApk("com.joas.smartcharger");
    }

    public void onCommTestErr() {
        TextView a = null;
        a.setText("abcd");
    }

    public void onViewOcppConfig() {
        String content = uiManager.getOcppSessionManager().getOcppConfiguration().getListAsString();
        tvContentView.setText(content);
        layoutContentView.setVisibility(VISIBLE);
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
    }

    @Override
    public void onPageDeactivate() {
        if ( timer != null ) timer.cancel();
    }
}
