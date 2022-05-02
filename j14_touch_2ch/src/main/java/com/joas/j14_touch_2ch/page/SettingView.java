/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.j14_touch_2ch.LocalMember;
import com.joas.j14_touch_2ch.MultiChannelUIManager;
import com.joas.j14_touch_2ch.R;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import org.w3c.dom.Text;

public class SettingView extends LinearLayout implements PageActivateListener {
    MultiChannelUIManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer = null;
    String m_chargerkind = "";

    TextView tvStationID;
    TextView tvStationID2;
    TextView tvCPID;
    TextView tvCPID2;
    TextView tvServerIP;
    TextView tvServerPort;
    TextView tvPassword;
    Switch switchUseBasicAuth;

    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingUseTL3500BS;
    Switch switchSettingUseSehanRF;
    Switch switchSettingUseAMCRF;
    Spinner chargerkindSpinner;


    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;

    TextView tvResrvinfo;

    public SettingView(Context context, MultiChannelUIManager manager, Activity activity) {
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
        tvStationID = (TextView)findViewById(R.id.textSettingStationID);
        tvStationID2 = (TextView)findViewById(R.id.textSettingStationID2);
        tvCPID      = (TextView)findViewById(R.id.textSettingChargerID);
        tvCPID2 = (TextView)findViewById(R.id.textSettingChargerID2);
        tvServerIP  = (TextView)findViewById(R.id.textSettingServerIP);
        tvServerPort  = (TextView)findViewById(R.id.textSettingServerPort);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);
        switchSettingUseTL3500BS = (Switch)findViewById(R.id.switchSettingTL3500Use);
        switchSettingUseSehanRF = (Switch)findViewById(R.id.switchSettingSehanRFUse);
        switchSettingUseAMCRF = (Switch)findViewById(R.id.switchSettingACMRFUse);

        chargerkindSpinner = (Spinner)findViewById(R.id.spinner_chargerkind);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(
                mainActivity,
                R.array.charger_kind,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chargerkindSpinner.setAdapter(adapter);
        chargerkindSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSelectChargerKind();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);

        tvResrvinfo = (TextView)findViewById(R.id.textSettingReservInfo);

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

        Button btOnFWRemoteTestOn = (Button)findViewById(R.id.btRmoteFWFotaOnBtn);
        btOnFWRemoteTestOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onFwRemoteOnClick();
            }
        });

        Button btOnFWRemoteTestOff = (Button)findViewById(R.id.btRemoteFWFotaOFFBtn);
        btOnFWRemoteTestOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onFwRemoteOffClick();
            }
        });

        //add by si. 200811 - 서버 접속 끊기 테스트 버튼
        Button btOnServerDisconnect = (Button)findViewById(R.id.btServerDisconnectBtn);
        btOnServerDisconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                onDisconnectServerClick();
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

        //결제관련 테스트 버튼
        Button btReqPrepay = (Button)findViewById(R.id.btprepayreq);
        btReqPrepay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onPrepayReq();
            }
        });
        Button btReqRealPay = (Button)findViewById(R.id.btrealpayreq);
        btReqRealPay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRealpayReq();
            }
        });
        Button btReqCancelPay = (Button)findViewById(R.id.btcancelreq);
        btReqCancelPay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelpayReq();
            }
        });

        //http downlaod test btn
        Button btHttpDownload = (Button)findViewById(R.id.btndownhttp);
        btHttpDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHttpDownloadReq();
            }
        });

    }
    //http download test==========================
//    void onHttpDownloadReq(){
//        flowManager.startHttpDownload();
//    }
    void onHttpDownloadReq(){
//        byte destflg = 0x05;
//        uiManager.doMemberFileRead(destflg);
    }
    //============================================

    //신용카드 결제 test===========================
    void onPrepayReq(){
//        TL3500S tl3500S = flowManager.getTL3500S();
//        tl3500S.payReq_G(200,0,true,0);
    }
    void onRealpayReq(){
//        TL3500S tl3500S = flowManager.getTL3500S();
//        tl3500S.payReq_G(100,0,false,0);
    }
    void onCancelpayReq(){
//        TL3500S tl3500S = flowManager.getTL3500S();
//        tl3500S.cancelPrePay(0);
    }
    //============================================

    //add by si.210624, 차지비 충전기 종류 저장
    void onSelectChargerKind(){
        m_chargerkind = chargerkindSpinner.getSelectedItem().toString();
    }

    void onExitClick() {
        onHideKeyboard();

        this.setVisibility(View.INVISIBLE);
    }

    void saveCPConfig() {
        boolean oldUseWatchDog = flowManager.getCpConfig().useWatchDogTimer;
        flowManager.getCpConfig().stationID = tvStationID.getText().toString();
        flowManager.getCpConfig().stationID2 = tvStationID2.getText().toString();
        flowManager.getCpConfig().chargerID = tvCPID.getText().toString();
        flowManager.getCpConfig().chargerID2 = tvCPID2.getText().toString();
        flowManager.getCpConfig().serverIP = tvServerIP.getText().toString();
        flowManager.getCpConfig().serverPort = Integer.parseInt(tvServerPort.getText().toString());

        flowManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        flowManager.getCpConfig().useTL3500BS = switchSettingUseTL3500BS.isChecked();
        flowManager.getCpConfig().useSehanRF = switchSettingUseSehanRF.isChecked();
        flowManager.getCpConfig().useACMRF = switchSettingUseAMCRF.isChecked();
        flowManager.getCpConfig().chargerKind = m_chargerkind;
        flowManager.getCpConfig().saveConfig(mainActivity);

        flowManager.onSettingChanged();
        if ( oldUseWatchDog != flowManager.getCpConfig().useWatchDogTimer ) {
            if (flowManager.getCpConfig().useWatchDogTimer != false) flowManager.stopWatdogTimer();
        }
    }

    void loadCPConfig() {
        flowManager.getCpConfig().loadConfig(mainActivity);

        tvStationID.setText(flowManager.getCpConfig().stationID);
        tvStationID2.setText(flowManager.getCpConfig().stationID2);
        tvCPID.setText(flowManager.getCpConfig().chargerID);
        tvCPID2.setText(flowManager.getCpConfig().chargerID2);
        tvServerIP.setText(flowManager.getCpConfig().serverIP);
        tvServerPort.setText("" + flowManager.getCpConfig().serverPort);

        tvPassword.setText(flowManager.getCpConfig().settingPassword);
        switchSkipAuth.setChecked(flowManager.getCpConfig().isAuthSkip);
        switchSettingWatchDogUse.setChecked(flowManager.getCpConfig().useWatchDogTimer);
        switchSettingUseTL3500BS.setChecked(flowManager.getCpConfig().useTL3500BS);
        switchSettingUseSehanRF.setChecked(flowManager.getCpConfig().useSehanRF);
        switchSettingUseAMCRF.setChecked(flowManager.getCpConfig().useACMRF);
        if (flowManager.getCpConfig().chargerKind.equals("OP")) chargerkindSpinner.setSelection(0);
        else if (flowManager.getCpConfig().chargerKind.equals("CL"))
            chargerkindSpinner.setSelection(1);
        else if (flowManager.getCpConfig().chargerKind.equals("CV"))
            chargerkindSpinner.setSelection(2);
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
        flowManager.showJoasDspMonitor();
    }

    public void onDspDebugViewShow() {
        flowManager.showJoasDebugView();
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

    public void onViewOcppConfig() {
        //String content = uiManager.getOcppSessionManager().getOcppConfiguration().getListAsString();
        //tvContentView.setText(content);
        //layoutContentView.setVisibility(VISIBLE);

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
