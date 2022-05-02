/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 2 오전 11:17
 *
 */

package com.joas.joasui_mobile_charger_2ch.ui.page;

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

import com.joas.joasui_mobile_charger_2ch.ui.R;
import com.joas.joasui_mobile_charger_2ch.ui.UIFlowManager;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class SettingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer = null;

    TextView tvPassword;

    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;

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
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);

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
    }

    void onExitClick() {
        onHideKeyboard();

        flowManager.onSettingViewExit();
    }

    void saveCPConfig() {
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().saveConfig(mainActivity);

        flowManager.onSettingChanged();
    }

    void loadCPConfig() {
        flowManager.getCpConfig().loadConfig(mainActivity);

        tvPassword.setText(flowManager.getCpConfig().settingPassword);
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
    }

    public void onCommTestErr() {
       // TextView a = null;
        //a.setText("aa");
        try {
            Thread.sleep(10*1000);
        }catch (Exception e){}
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
