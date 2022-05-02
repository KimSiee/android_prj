/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd.page;

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
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.payment.tl3500s.TL3500S;
import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.R;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class SettingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer = null;
    String m_chargerkind = "";

    TextView tvStationID;
    TextView tvCPID;
    TextView tvServerIP;
    TextView tvServerPort;
    TextView tvPassword;
    TextView tvKakaoQRCost;
    TextView tvKakaoCreditCost;
    Switch switchUseBasicAuth;

    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingUseTL3500BS;
    Switch switchSettingUseSehanRF;
    Switch switchSettingUseAMCRF;
    Switch switchSettingUseKakaoNavi;
    Switch switchSettingUseKakaoCost;
    Spinner chargerkindSpinner;

    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;

    TextView tvResrvinfo;

    RadioGroup rgChargerType;
    int slowChargerType = 0;//default

    //ocpp
    Switch switchUseOCPP;
    Switch switchUseOCPPSSL;
    Switch switchUseOCPPBasicAuth;
    TextView ocppChargerID;
    TextView ocppServerURL;
    TextView ocppBasicAuthId;
    TextView ocppBasicAuthPw;
    TextView ocppChargerPointSerialNum;



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
        tvStationID = (TextView)findViewById(R.id.textSettingStationID);
        tvCPID      = (TextView)findViewById(R.id.textSettingChargerID);
        tvServerIP  = (TextView)findViewById(R.id.textSettingServerIP);
        tvServerPort  = (TextView)findViewById(R.id.textSettingServerPort);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);
        tvKakaoQRCost = (TextView)findViewById(R.id.textSettingQRCost);
        tvKakaoCreditCost = (TextView)findViewById(R.id.textSettingCreditCost);

        tvVersion = (TextView)findViewById(R.id.tvUIversion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);

        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);
        switchSettingUseTL3500BS = (Switch)findViewById(R.id.switchSettingTL3500Use);
        switchSettingUseSehanRF = (Switch)findViewById(R.id.switchSettingSehanRFUse);
        switchSettingUseAMCRF = (Switch)findViewById(R.id.switchSettingACMRFUse);
        switchSettingUseKakaoNavi = (Switch)findViewById(R.id.switchSettingKakaoUse);
        switchSettingUseKakaoCost = (Switch)findViewById(R.id.switchSettingUseKakaoCost);

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

        rgChargerType = (RadioGroup)findViewById(R.id.rgChargerType);
        rgChargerType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rg_btn0) slowChargerType = 0;          //빌트인 2ch(14kw)
                else if(checkedId == R.id.rg_btn1) slowChargerType = 1;     //스탠드 동시 2ch(14kw)
                else if(checkedId == R.id.rg_btn2) slowChargerType = 2;     //스탠드 분산 2ch(14kw)
                else if(checkedId == R.id.rg_btn3) slowChargerType = 3;     //스탠드 분산 2ch(17.6kw)
                else if(checkedId == R.id.rg_btn4) slowChargerType = 4;     //스탠드 1ch(14kw)
                else if(checkedId == R.id.rg_btn5) slowChargerType = 5;     //스탠드 1ch(17.6kw)
                else if(checkedId == R.id.rg_btn6) slowChargerType = 6;     //스탠드 1ch(22kw)
                else if(checkedId == R.id.rg_btn7) slowChargerType = 7;     //JC-92C1-7-01[4.3"LCD + TOUCH / 7kW B OR C TYPE] *탈착형 디자인
                else if(checkedId == R.id.rg_btn8) slowChargerType = 8;     //JC-92B1-7-01[8"LCD + TOUCH / 7kW C TYPE] *빌트인 디자인
                else if(checkedId == R.id.rg_btn9) slowChargerType = 9;     //JC-92B1-11-01[4.3"LCD + TOUCH / 11kW C TYPE] *탈착형 디자인
            }
        });

        //ocpp관련
        switchUseOCPP = (Switch)findViewById(R.id.switchSettingUseOcpp);
        switchUseOCPPSSL = (Switch)findViewById(R.id.switchSettingUseOcppSSL);
        switchUseOCPPBasicAuth = (Switch)findViewById(R.id.swUseOcppBasicAuth);
        ocppChargerID = (TextView)findViewById(R.id.textSettingOcppChargerID);
        ocppServerURL = (TextView)findViewById(R.id.textSettingOcppServerURI);
        ocppBasicAuthId = (TextView)findViewById(R.id.textSettingBasicAuthID);
        ocppBasicAuthPw = (TextView)findViewById(R.id.textSettingBasicAuthPassword);
        ocppChargerPointSerialNum = (TextView)findViewById(R.id.textSettingChargerPointSerialNum);

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
                onFwRemoteOnClick();
            }
        });

        Button btOnFWRemoteTestOff = (Button)findViewById(R.id.btRemoteFWFotaOFFBtn);
        btOnFWRemoteTestOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onFwRemoteOffClick();
            }
        });

        //add by si. 200811 - 서버 접속 끊기 테스트 버튼
        Button btOnServerDisconnect = (Button)findViewById(R.id.btServerDisconnectBtn);
        btOnServerDisconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onDisconnectServerClick();
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

        //부분취소 테스트 관련
        Button btReqPartialCancel = (Button)findViewById(R.id.btpartialCancel);
        btReqPartialCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPartialCancelPayReq();
            }
        });
        Button btReqNonCardCancel = (Button)findViewById(R.id.btNoCardCancel);
        btReqNonCardCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onNonCardCancelPayReq();
            }
        });
        Button btTermReady = (Button)findViewById(R.id.btTermReady);
        btTermReady.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onTermReadyReq();
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
        byte destflg = 0x05;
        flowManager.doMemberFileRead(destflg);
    }
    //============================================

    //신용카드 결제 test===========================
    void onPrepayReq(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.payReq_G(1000,0,true,0);
    }
    void onRealpayReq(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.payReq_G(100,0,false,0);
    }
    void onCancelpayReq(){
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.cancelPrePay(0);
    }
    void onPartialCancelPayReq() {
        //부분취소
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.cancelPay_Partial(0, 900, 0,
                pinfo.paymentPreDealApprovalNo, pinfo.paymentPreDealApprovalTime, pinfo.paymentPreDealSerialNo, 5);
    }
    void onNonCardCancelPayReq(){
        //무카드취소(선결제 전체 취소)
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.cancelPay_Partial(0, 1000, 0,
                pinfo.paymentPreDealApprovalNo, pinfo.paymentPreDealApprovalTime, pinfo.paymentPreDealSerialNo, 4);
    }
    void onTermReadyReq(){
        //단말기 대기
        TL3500S tl3500S = flowManager.getTL3500S();
        tl3500S.termReadyReq();
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
        flowManager.getCpConfig().chargerID = tvCPID.getText().toString();
        flowManager.getCpConfig().serverIP = tvServerIP.getText().toString();
        flowManager.getCpConfig().serverPort = Integer.parseInt(tvServerPort.getText().toString());

        flowManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        flowManager.getCpConfig().useTL3500BS = switchSettingUseTL3500BS.isChecked();
        flowManager.getCpConfig().useSehanRF = switchSettingUseSehanRF.isChecked();
        flowManager.getCpConfig().useACMRF = switchSettingUseAMCRF.isChecked();
        flowManager.getCpConfig().useKakaoNavi = switchSettingUseKakaoNavi.isChecked();
        flowManager.getCpConfig().useKakaoCost = switchSettingUseKakaoCost.isChecked();
        flowManager.getCpConfig().chargerKind = m_chargerkind;
        flowManager.getCpConfig().kakaoQRCost = Integer.parseInt(tvKakaoQRCost.getText().toString());
        flowManager.getCpConfig().kakaoCreditCost = Integer.parseInt(tvKakaoCreditCost.getText().toString());
        flowManager.getCpConfig().slowChargerType = slowChargerType;

        //ocpp관련
        flowManager.getCpConfig().useOcpp = switchUseOCPP.isChecked();
        flowManager.getCpConfig().useSSL = switchUseOCPPSSL.isChecked();
        flowManager.getCpConfig().useBasicAuth = switchUseOCPPBasicAuth.isChecked();
        flowManager.getCpConfig().ocpp_chargerID = ocppChargerID.getText().toString();
        flowManager.getCpConfig().ocpp_serverURI = ocppServerURL.getText().toString();
        flowManager.getCpConfig().httpBasicAuthID = ocppBasicAuthId.getText().toString();
        flowManager.getCpConfig().httpBasicAuthPassword = ocppBasicAuthPw.getText().toString();
        flowManager.getCpConfig().ocpp_chargePointSerialNumber = ocppChargerPointSerialNum.getText().toString();

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
        switchSettingUseTL3500BS.setChecked(flowManager.getCpConfig().useTL3500BS);
        switchSettingUseSehanRF.setChecked(flowManager.getCpConfig().useSehanRF);
        switchSettingUseAMCRF.setChecked(flowManager.getCpConfig().useACMRF);
        switchSettingUseKakaoNavi.setChecked(flowManager.getCpConfig().useKakaoNavi);
        switchSettingUseKakaoCost.setChecked(flowManager.getCpConfig().useKakaoCost);
        if (flowManager.getCpConfig().chargerKind.equals("OP")) chargerkindSpinner.setSelection(0);
        else if (flowManager.getCpConfig().chargerKind.equals("CL"))
            chargerkindSpinner.setSelection(1);
        else if (flowManager.getCpConfig().chargerKind.equals("CV"))
            chargerkindSpinner.setSelection(2);
        tvKakaoQRCost.setText("" + flowManager.getCpConfig().kakaoQRCost);
        tvKakaoCreditCost.setText("" + flowManager.getCpConfig().kakaoCreditCost);

        //load slowo charger type
        switch(flowManager.getCpConfig().slowChargerType){
            case 0:
                rgChargerType.check(R.id.rg_btn0);
                break;
            case 1:
                rgChargerType.check(R.id.rg_btn1);
                break;
            case 2:
                rgChargerType.check(R.id.rg_btn2);
                break;
            case 3:
                rgChargerType.check(R.id.rg_btn3);
                break;
            case 4:
                rgChargerType.check(R.id.rg_btn4);
                break;
            case 5:
                rgChargerType.check(R.id.rg_btn5);
                break;
            case 6:
                rgChargerType.check(R.id.rg_btn6);
                break;
            case 7:
                rgChargerType.check(R.id.rg_btn7);
                break;
            case 8:
                rgChargerType.check(R.id.rg_btn8);
                break;
            case 9:
                rgChargerType.check(R.id.rg_btn9);
                break;
        }

        //ocpp관련정보 load
        switchUseOCPP.setChecked(flowManager.getCpConfig().useOcpp);
        switchUseOCPPSSL.setChecked(flowManager.getCpConfig().useSSL);
        switchUseOCPPBasicAuth.setChecked(flowManager.getCpConfig().useBasicAuth);
        ocppChargerID.setText(flowManager.getCpConfig().ocpp_chargerID);
        ocppServerURL.setText(flowManager.getCpConfig().ocpp_serverURI);
        ocppBasicAuthId.setText(flowManager.getCpConfig().httpBasicAuthID);
        ocppBasicAuthPw.setText(flowManager.getCpConfig().httpBasicAuthPassword);
        ocppChargerPointSerialNum.setText(flowManager.getCpConfig().ocpp_chargePointSerialNumber);
    }

    void statusUpdate() {
        String localIP = NetUtil.getLocalIpAddress();
        if (localIP != null) tvSettingLocalIP.setText(localIP);
    }

    void onHideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    /**
     * add by si. 200609 200번지 15번 비트 F/W원격 업데이트 True/Flase 전환 함수
     */
    public void onFwRemoteOnClick(){
        flowManager.setFWRemoteOn();
    }
    public void onFwRemoteOffClick(){
        flowManager.setFWRemoteOff();
    }

    //add by si. 200811 - server socket disconnect test
    public void onDisconnectServerClick(){
        flowManager.doServerDisconnect();
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
        PoscoChargerInfo pinfo = flowManager.getPoscoChargerInfo();
        String uiver = pinfo.chg_sw_version;
        tvVersion.setText(uiver);
    }

    @Override
    public void onPageDeactivate() {
        if ( timer != null ) timer.cancel();
    }
}
