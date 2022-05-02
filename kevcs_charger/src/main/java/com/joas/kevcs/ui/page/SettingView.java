/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.SoundManager;
import com.joas.kevcs.ui.TypeDefine;
import com.joas.kevcs.ui.UIFlowManager;
import com.joas.kevcscomm.KevcsChargerInfo;
import com.joas.kevcscomm.KevcsComm;
import com.joas.kevcscomm.KevcsCostInfo;
import com.joas.kevcscomm.KevcsCostManager;
import com.joas.kevcscomm.KevcsProtocol;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.NetUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.TimeUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class SettingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    Activity mainActivity;
    TimeoutTimer timer;

    TextView tvChargerID;
    TextView tvAuthKey;
    TextView tvServerIP;
    TextView tvServerPort;
    TextView tvPassword;
    Switch switchSkipAuth;
    Switch switchSettingWatchDogUse;
    Switch switchSettingIsFaster;
    Switch switchSettingUsePayTerminal;
    Switch switchSettingUseSound;

    TextView tvVersion;
    TextView tvLastErrorMsg;
    TextView tvSettingLocalIP;
    TextView tvSettingPingTestResult;
    TextView tvSettingMeter;

    FrameLayout framePaymInty;
    Button btPaymIntyChk;
    Button btPaymIntyClose;
    TextView tvPaymIntyContent;
    TextView tvPayTerminalVer;
    TextView tvPayTerminalTID;
    TextView tvControlBoardVer;
    Spinner spinCommKind;
    Spinner spinPowerCtlValue;
    Spinner spinSoundVol;
    Button btSettingPowerCtlTest;
    EditText editSettingVolume;
    Button btVolumeSet;
    TextView tvCostUnitInfo;
    TextView tvCableOrgPos;
    TextView tvCurTime;

    public SettingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_setting, this, true);

        initComponents();
    }

    void initComponents() {
        tvChargerID = (TextView)findViewById(R.id.textSettingChargerID);
        tvAuthKey      = (TextView)findViewById(R.id.textSettingAuthKey);
        tvServerIP  = (TextView)findViewById(R.id.textSettingServerIP);
        tvServerPort  = (TextView)findViewById(R.id.textSettingServerPort);

        switchSkipAuth = (Switch)findViewById(R.id.switchSettingSkipAuth);
        tvPassword    = (TextView)findViewById(R.id.textSettingPassword);
        switchSettingWatchDogUse = (Switch)findViewById(R.id.switchSettingWatchDogUse);

        tvVersion = (TextView)findViewById(R.id.textSettingVersion);
        tvLastErrorMsg = (TextView)findViewById(R.id.textSettingLastError);
        tvSettingPingTestResult = (TextView)findViewById(R.id.tvSettingPingTestResult);

        tvSettingLocalIP = (TextView)findViewById(R.id.textSettingLocalIP);
        switchSettingIsFaster = (Switch)findViewById(R.id.switchSettingIsFaster);
        switchSettingUsePayTerminal = (Switch)findViewById(R.id.switchSettingUsePayTerminal);
        switchSettingUseSound = findViewById(R.id.switchSettingUseSound);

        tvSettingMeter = findViewById(R.id.tvSettingMeter);

        tvVersion.setText(TypeDefine.SW_VERSION+" "+TypeDefine.SW_RELEASE_DATE);

        framePaymInty = findViewById(R.id.framePaymInty);
        btPaymIntyChk = findViewById(R.id.btPaymIntyChk);
        btPaymIntyClose = findViewById(R.id.btPaymIntyClose);
        tvPaymIntyContent = findViewById(R.id.tvPaymIntyContent);
        tvPayTerminalVer = findViewById(R.id.tvPayTerminalVer);
        tvPayTerminalTID = findViewById(R.id.tvPayTerminalTID);
        tvCostUnitInfo = findViewById(R.id.tvCostUnitInfo);
        tvControlBoardVer = findViewById(R.id.tvControlBoardVer);
        tvCableOrgPos = findViewById(R.id.tvCableOrgPos);
        tvCurTime = findViewById(R.id.tvCurTime);

        TextView tvBootTime = findViewById(R.id.tvBootTime);
        tvBootTime.setText(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss"));
        tvBootTime.invalidate();

        spinCommKind = findViewById(R.id.spinCommKind);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                KevcsProtocol.KevcsCmd.names());
        spinCommKind.setAdapter(arrayAdapter);


        spinPowerCtlValue = findViewById(R.id.spinPowerCtlValue);
        String[] ctlKind = { "0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%" };
        ArrayAdapter<String> arrayAdapter2 = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                ctlKind);
        spinPowerCtlValue.setAdapter(arrayAdapter2);
        btSettingPowerCtlTest = findViewById(R.id.btSettingPowerCtlTest);
        editSettingVolume = findViewById(R.id.editSettingVolume);
        btVolumeSet = findViewById(R.id.btVolumeSet);

        spinSoundVol = findViewById(R.id.spinSoundVol);
        String[] kind = { "0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%" };
        ArrayAdapter<String> arrayAdapter3 = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                kind);
        spinSoundVol.setAdapter(arrayAdapter3);

        spinSoundVol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                flowManager.getSoundManager().playSound(SoundManager.SoundKind.Ready, ((float)position)/10.0f);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        initButtonEvent();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateStatus();
            }
        });
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

        Button  btSettingCommTest = (Button) findViewById(R.id.btSettingCommTest);
        btSettingCommTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommTest();
            }
        });

        Button btFinishApp = (Button) findViewById(R.id.btFinishApp);
        btFinishApp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCommFinishApp();
            }
        });

        Button  btPaymIntyChkShow = (Button) findViewById(R.id.btPaymIntyChkShow);
        btPaymIntyChkShow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                framePaymInty.setVisibility(VISIBLE);
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

        Button  btPingTest = (Button)findViewById(R.id.btPingTest);
        btPingTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onPingTest();
            }
        });

        Button btUpdateUSB = findViewById(R.id.btUpdateUSB);
        btUpdateUSB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onUpdateUsb();
            }
        });

        btPaymIntyChk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onPaymInitChk();
            }
        });
        btPaymIntyClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                framePaymInty.setVisibility(GONE);
            }
        });

        btSettingPowerCtlTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onPowerCtlTest();
            }
        });

        btVolumeSet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onVolumeSetTest();
            }
        });
    }

    void onExitClick() {
        onHideKeyboard();

        flowManager.onSettingViewExit();
    }

    void saveCPConfig() {

        flowManager.getCpConfig().chargerID = tvChargerID.getText().toString();
        flowManager.getCpConfig().serverAuthKey = tvAuthKey.getText().toString();
        flowManager.getCpConfig().serverIP = tvServerIP.getText().toString();
        flowManager.getCpConfig().serverPort = Integer.parseInt(tvServerPort.getText().toString());

        flowManager.getCpConfig().isAuthSkip = switchSkipAuth.isChecked();
        flowManager.getCpConfig().settingPassword = tvPassword.getText().toString();
        flowManager.getCpConfig().useWatchDogTimer = switchSettingWatchDogUse.isChecked();
        flowManager.getCpConfig().isFastCharger = switchSettingIsFaster.isChecked();
        flowManager.getCpConfig().usePayTerminal = switchSettingUsePayTerminal.isChecked();
        flowManager.getCpConfig().useSound = switchSettingUseSound.isChecked();
        flowManager.getCpConfig().soundVol = spinSoundVol.getSelectedItemPosition();

        flowManager.getCpConfig().saveConfig(mainActivity);

        flowManager.onSettingChanged();
    }

    void loadCPConfig() {
        flowManager.getCpConfig().loadConfig(mainActivity);

        tvChargerID.setText(flowManager.getCpConfig().chargerID);
        tvAuthKey.setText(flowManager.getCpConfig().serverAuthKey);
        tvServerIP.setText(flowManager.getCpConfig().serverIP);
        tvServerPort.setText(""+flowManager.getCpConfig().serverPort);

        tvPassword.setText(flowManager.getCpConfig().settingPassword);
        switchSkipAuth.setChecked(flowManager.getCpConfig().isAuthSkip);
        switchSettingWatchDogUse.setChecked(flowManager.getCpConfig().useWatchDogTimer);
        switchSettingIsFaster.setChecked(flowManager.getCpConfig().isFastCharger);
        switchSettingUsePayTerminal.setChecked(flowManager.getCpConfig().usePayTerminal);
        switchSettingUseSound.setChecked(flowManager.getCpConfig().useSound);
        spinSoundVol.setSelection(flowManager.getCpConfig().soundVol);
    }

    void statusUpdate() {
        String localIP = NetUtil.getLocalIpAddress();
        String mac = NetUtil.getEthernetMacAddress();
        if (localIP != null) {
            tvSettingLocalIP.setText(localIP+", MAC:"+mac);
        }
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

    public void onCommTest() {
        String strCmd = spinCommKind.getSelectedItem().toString();
        KevcsProtocol.KevcsCmd cmd = KevcsProtocol.KevcsCmd.getValue(strCmd);
        if ( flowManager.getKevcsComm().sendTestCmd(cmd) ) {
            Toast.makeText(getContext(), "통신 명령 " + strCmd + " 명령을 전송했습니다.", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getContext(), "통신 명령 " + strCmd + " 명령은 전송할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
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

    public void onPaymInitChk() {
        flowManager.getKevcsComm().sendGlobPaymInty_66(false);
    }

    public void onPingTest() {
        final EditText ipAddress = new EditText(this.getContext());
        final Context context = this.getContext();
        ipAddress.setInputType(InputType.TYPE_CLASS_TEXT);

        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(ipAddress, 1);

        new AlertDialog.Builder(context)
                .setTitle("Ping Test")
                .setMessage("상대편 IP 주소를 입력하세요.")
                .setView(ipAddress)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String ip= ipAddress.getText().toString();
                        tvSettingPingTestResult.setText("Ping 진행중:"+ip);
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                doPingTest(ip);
                            }
                        }, 100);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public void doPingTest(String ip) {
        boolean ret = NetUtil.pingTest(ip);
        String retStr = "Ping to "+ip+" 결과: " + (ret ? "성공":"실패");
        Toast.makeText(mainActivity, retStr , Toast.LENGTH_LONG).show();
        tvSettingPingTestResult.setText(retStr);
    }

    public void onUpdateUsb() {
        boolean isSuccess = false;
        String updatePath = TypeDefine.USB_MEMERY_PATH;
        try {
            File file = new File(updatePath + "/" + KevcsComm.FILE_UPDATE_FIRMWARE_APK);
            if (file.exists()) {
                Toast.makeText(getContext(), "USB 메모리로부터 업데이트를 시작합니다.", Toast.LENGTH_SHORT).show();
                RemoteUpdater updater = new RemoteUpdater(mainActivity, updatePath, KevcsComm.FILE_UPDATE_FIRMWARE_APK);
                updater.doUpdateFromApk("com.joas.smartcharger");
                isSuccess = true;
            }
        }
        catch (Exception e) {
            LogWrapper.e("SettingView", "onUpdateUsb err:"+e.toString());
        }
        if ( isSuccess == false ) {
            Toast.makeText(getContext(), "업데이트가 실패하였습니다. USB ROOT에 update.apk를 확인하세요.", Toast.LENGTH_LONG).show();
        }
    }

    void onPowerCtlTest() {
        int idx = spinPowerCtlValue.getSelectedItemPosition();

        flowManager.setPowerControl(idx*10);
    }

    void onVolumeSetTest() {
        AudioManager am = (AudioManager) mainActivity.getSystemService(Context.AUDIO_SERVICE);
        int vol = Integer.parseInt(editSettingVolume.getText().toString());
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                vol,
                0);
        flowManager.playVoiceState(UIFlowManager.UIFlowState.UI_START);
    }

    void updateStatus() {
        tvLastErrorMsg.setText(LogWrapper.lastErrorMessage);

        tvSettingMeter.setText(""+flowManager.getLastMeterValue());

        if (framePaymInty.getVisibility() == VISIBLE) {
            updatePaymInty();
        }

        if ( flowManager.getCpConfig().usePayTerminal ) {
            KevcsChargerInfo info = flowManager.getKevcsComm().getChargerInfo();
            String str = info.payRxInfo.paym_model_no+", "+info.payRxInfo.paym_fw_ver+", "+info.payRxInfo.m2m_fw_ver;
            tvPayTerminalVer.setText(str);
            tvPayTerminalVer.invalidate();

            tvPayTerminalTID.setText(info.payRxInfo.paym_tid);
            tvPayTerminalTID.invalidate();
        }

        tvControlBoardVer.setText(flowManager.getKevcsComm().getChargerInfo().board_ver);
        tvControlBoardVer.invalidate();

        KevcsCostInfo costInfo  = flowManager.getKevcsComm().getCostManager().getCurrentCostInfo();

        tvCostUnitInfo.setText(String.format("단가:%.2f원, 버전:%s",flowManager.getKevcsComm().getCurrentCostUnit(),costInfo.version));
        tvCostUnitInfo.invalidate();

        tvCableOrgPos.setText(flowManager.getIsPlugOrgPosition() ? "원위치" : "빠짐");
        tvCableOrgPos.invalidate();

        tvCurTime.setText(TimeUtil.getCurrentTimeAsString("yyyy-MM-dd HH:mm:ss"));
        tvCurTime.invalidate();
    }

    void updatePaymInty() {
        KevcsChargerInfo info = flowManager.getKevcsComm().getChargerInfo();

        String str = "";
        try {
            for (int i = 0; i < info.payRxInfo.paym_inty_chk_ret.length; i++) {
                str += "무결성 검사 " + (i + 1) + " : " + info.payRxInfo.paym_inty_chk_ret[i] + "\r\n";
                str += "검사 일시 " + (i + 1) + " : " + info.payRxInfo.paym_inty_chk_date[i] + "\r\n";
                str += "검사 시점 " + (i + 1) + " : " + info.payRxInfo.paym_inty_chk_type[i] + "\r\n";
            }
            str += "모델명 : " + info.payRxInfo.paym_reg_model;
        } catch (Exception e) {
            str = "무결성 검사 오류: 결과 없음";
        }
        tvPaymIntyContent.setText(str);
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        loadCPConfig();
        statusUpdate();
        onHideKeyboard();
        timer.start();
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
    }
}
