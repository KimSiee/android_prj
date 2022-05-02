/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 7 오후 5:03
 *
 */

package com.joas.certtest;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.hw.dsp.DSPControl;
import com.joas.hw.dsp.DSPControlListener;
import com.joas.hw.dsp.DSPRxData;
import com.joas.hw.dsp.DSPTxData;
import com.joas.utils.EMINetworkTest;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class CertTestActivity extends AppCompatActivity implements DSPControlListener {
    public final static String CP_CONFIG_FILE_NAME = "CertTestConfig.txt";

    DSPControl dspControl;
    Timer timerSec = null;

    //EMI Test 통신
    EMINetworkTest emiNetworkTest;

    TextView tvCertTestStatus;
    TextView tvCertTestInput;
    TextView tvCertTestVoltageInput;
    TextView tvCertTestVoltageDSP;
    TextView tvCertTestCurrentInput;
    TextView tvCertTestCurrentDSP;
    Button btCertTestInput;
    FrameLayout frameEmergency;

    String inputValue = "";
    boolean isInputVoltage = true;
    float voltOut = 0;
    float currentOut = 0;
    boolean runFlag = false;
    int select = 0;
    boolean isEmergency = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideNavBar();
        setContentView(R.layout.activity_cert_test);

        initComponent();

        dspControl = new DSPControl(1, "/dev/ttyS2", DSPControl.DSP_VER_REG17_SIZE, DSPControl.DSP_VER_TXREG_DEFAULT_SIZE, 1,this);
        dspControl.start();

        loadConfig();

        // 1초 타이머 시작
        startPeroidTimerSec();

        emiNetworkTest = new EMINetworkTest("192.168.0.100", emiNetworkTest.DEFAULT_PORT);
        emiNetworkTest.startComm();
    }
    void initComponent() {
        tvCertTestStatus = (TextView) findViewById(R.id.tvCertTestStatus);
        tvCertTestInput = (TextView) findViewById(R.id.tvCertTestInput);
        tvCertTestVoltageInput = (TextView) findViewById(R.id.tvCertTestVoltageInput);
        tvCertTestVoltageDSP = (TextView) findViewById(R.id.tvCertTestVoltageDSP);
        tvCertTestCurrentInput = (TextView) findViewById(R.id.tvCertTestCurrentInput);
        tvCertTestCurrentDSP = (TextView) findViewById(R.id.tvCertTestCurrentDSP);

        btCertTestInput = (Button)findViewById(R.id.btCertTestInput);
        frameEmergency = findViewById(R.id.frameEmergency);

        tvCertTestInput.setText("");
    }

    void startPeroidTimerSec() {
        timerSec = new Timer();
        timerSec.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerProcessSec();
                    }
                });
            }
        }, 1000, 1000);
    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수핸한다.
     */
    public void timerProcessSec() {
        DSPRxData dspRxData = dspControl.getDspRxData(0);
        dspRxData.decode();

        if (runFlag == true) {
            float volCmd = Float.parseFloat((String) tvCertTestVoltageInput.getText());
            float ampCmd = Float.parseFloat((String) tvCertTestCurrentInput.getText());

            dspControl.setVoltageCmd(0, volCmd);
            dspControl.setAmpareCmd(0, ampCmd);

            tvCertTestVoltageDSP.setText(String.format("%.2f", dspRxData.voltageOut));
            tvCertTestCurrentDSP.setText(String.format("%.2f", dspRxData.ampareOut));
        } else {
            dspControl.setVoltageCmd(0, 0);
            dspControl.setAmpareCmd(0, 0);

            tvCertTestVoltageDSP.setText("0");
            tvCertTestCurrentDSP.setText("0");
        }

        // 비상버턴 이벤트
        if ( dspRxData.get400Reg(DSPRxData.STATUS400.FAULT) && ((dspRxData.fault406 & 0x10) == 0x10) ) {
            if ( isEmergency == false ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCertTestStatus.setBackgroundColor(0xffcc0000);
                        tvCertTestStatus.setText("비상 정지 폴트");
                        frameEmergency.setVisibility(View.VISIBLE);

                    }
                });
                isEmergency = true;
                runFlag = false;
            }
        }
        else if ( isEmergency == true ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvCertTestStatus.setBackgroundColor(0xff0099cc);
                    tvCertTestStatus.setText("정지");
                    frameEmergency.setVisibility(View.INVISIBLE);
                }
            });

            isEmergency = false;
        }
    }

    public void onClickSlowBStart(View v) {
        select = DSPTxData.CHARGER_SELECT_SLOW_BTYPE;
        dspControl.setConnectorSelect(0, DSPTxData.CHARGER_SELECT_SLOW_BTYPE);
        dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(0, DSPTxData.STATUS200.READY, true);
        dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, true);

        runFlag = true;
        tvCertTestStatus.setText("완속 B 시작");
        saveConfig();
    }

    public void onClickSlowCStart(View v) {
        select = DSPTxData.CHARGER_SELECT_SLOW_CTYPE;
        dspControl.setConnectorSelect(0, DSPTxData.CHARGER_SELECT_SLOW_CTYPE);
        dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(0, DSPTxData.STATUS200.READY, true);
        dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, true);

        runFlag = true;
        tvCertTestStatus.setText("완속 C 시작");
        saveConfig();
    }

    public void onClickComboStart(View v) {
        dspControl.setConnectorSelect(0, DSPTxData.CHARGER_SELECT_FAST_DCCOMBO_TEST);
        dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(0, DSPTxData.STATUS200.READY, true);
        dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, true);

        runFlag = true;
        tvCertTestStatus.setText("콤보 시작");
        saveConfig();
    }

    public void onClickChademoStart(View v) {
        dspControl.setConnectorSelect(0, DSPTxData.CHARGER_SELECT_FAST_DCCHADEMO_TEST);
        dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(0, DSPTxData.STATUS200.READY, true);
        dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, true);

        runFlag = true;
        tvCertTestStatus.setText("차데모 시작");
        saveConfig();
    }

    public void onClickStop(View v) {
        runFlag = false;
        dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, false);
        dspControl.setState200(0, DSPTxData.STATUS200.READY, false);
        dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, true);
        tvCertTestStatus.setText("중지");
        select = 0;
        saveConfig();
    }

    public void onClickDoor(View v) {
        dspControl.setState200(0, DSPTxData.STATUS200.DOOR_OPEN, true);
    }

    public void onClickInput(View v) {
        isInputVoltage = !isInputVoltage;

        if ( isInputVoltage ) {
            btCertTestInput.setText("전압입력");
        }
        else {
            btCertTestInput.setText("전류입력");
        }
    }

    public void onClickInputOK(View v) {
        if ( isInputVoltage ) {
            if (tvCertTestInput.getText().length() == 0)
            {
                tvCertTestVoltageInput.setText("0");
            }
            else {
                tvCertTestVoltageInput.setText(tvCertTestInput.getText());
            }
        }
        else {
            if (tvCertTestInput.getText().length() == 0)
            {
                tvCertTestCurrentInput.setText("0");
            }
            else {
                tvCertTestCurrentInput.setText(tvCertTestInput.getText());
            }
        }
        saveConfig();
    }

    public void onClickInputClear(View v) {
        inputValue = "";
        tvCertTestInput.setText(inputValue);
    }

    public void onClickExit(View v) {
        this.finish();
    }

    void AddNumber(String num) {
        inputValue += num;
        tvCertTestInput.setText(inputValue);
    }

    public void onClickNum1(View v) { AddNumber("1"); }
    public void onClickNum2(View v) { AddNumber("2"); }
    public void onClickNum3(View v) { AddNumber("3"); }
    public void onClickNum4(View v) { AddNumber("4"); }
    public void onClickNum5(View v) { AddNumber("5"); }
    public void onClickNum6(View v) { AddNumber("6"); }
    public void onClickNum7(View v) { AddNumber("7"); }
    public void onClickNum8(View v) { AddNumber("8"); }
    public void onClickNum9(View v) { AddNumber("9"); }
    public void onClickNum0(View v) { AddNumber("0"); }

    public void hideNavBar() {
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {
            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDspStatusChange(int channel, DSPRxData.STATUS400 idx, boolean val) {
        switch (idx) {
            case READY:
                break;

            case AVAL_CHARGE:
                break;

            case STATE_PLUG:
                break;

            case STATE_DOOR:
                if ( val == false ) { // 도어 오픈
                    //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
                    dspControl.setState200(0, DSPTxData.STATUS200.DOOR_OPEN, false);
                }
                break;

            case CHARGE_RUN:
                if ( val == true ) {
                    dspControl.setState200(0, DSPTxData.STATUS200.START_CHARGE, false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvCertTestStatus.setText("충전중");
                        }
                    });
                }
                break;

            case FINISH_CHARGE:
                if ( val == true ) {
                    dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
                    runFlag = false;
                    if (isEmergency == false) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvCertTestStatus.setText("중지");
                            }
                        });
                    }
                }
                break;

            case FAULT:
                break;

            case STATE_RESET:
                break;

            case CONNECTOR_LOCK_A:
                break;
        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {

    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {

    }

    @Override
    protected void onDestroy() {
        dspControl.interrupt();
        dspControl.stopThread();
        emiNetworkTest.stopComm();
        emiNetworkTest.interrupt();
        super.onDestroy();
    }

    public void loadConfig() {
        String loadString = null;
        try {
            loadString = FileUtil.getStringFromFile( Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH+"/"+CP_CONFIG_FILE_NAME);
        } catch (Exception ex) {
            loadString = null;
        }
        if (loadString == null ) {
        }
        else {
            int voltage = 0;
            int ampare = 0;
            boolean isStart = false;
            int select = 0;

            try {
                JSONObject obj = new JSONObject(loadString);
                voltage = obj.getInt("voltage");
                ampare = obj.getInt("ampare");
                isStart = obj.getBoolean("isStart");
                select = obj.getInt("select");

                if ( isStart ) {
                    dspControl.setConnectorSelect(0, select);

                    if ( select == DSPTxData.CHARGER_SELECT_SLOW_CTYPE ) {
                        tvCertTestStatus.setText("완속 C 시작");
                    }
                    else if ( select == DSPTxData.CHARGER_SELECT_SLOW_BTYPE ) {
                        tvCertTestStatus.setText("완속 B 시작");
                    }

                    dspControl.setState200(0, DSPTxData.STATUS200.FINISH_CHARGE, false);
                    dspControl.setState200(0, DSPTxData.STATUS200.READY, true);

                    tvCertTestVoltageInput.setText(""+voltage);
                    tvCertTestCurrentInput.setText(""+ampare);

                    float volCmd = Float.parseFloat((String) ""+voltage);
                    float ampCmd = Float.parseFloat((String) ""+ampare);

                    dspControl.setVoltageCmd(0, volCmd);
                    dspControl.setAmpareCmd(0, ampCmd);
                    runFlag = isStart;
                }
            } catch (Exception ex) {
                LogWrapper.e("CertTest" , "Json Parse Err:"+ex.toString());
            }
        }
    }

    public void saveConfig() {
        JSONObject obj = new JSONObject();
        try {
            int volCmd = Integer.parseInt((String) tvCertTestVoltageInput.getText());
            int ampCmd = Integer.parseInt((String) tvCertTestCurrentInput.getText());
            obj.put("voltage", volCmd);
            obj.put("ampare", ampCmd);
            obj.put("isStart", runFlag);
            obj.put("select", select);
        } catch (Exception ex) {
            LogWrapper.e("CertTest" , "Json Make Err:"+ex.toString());
        }

        FileUtil.stringToFile(Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, CP_CONFIG_FILE_NAME, obj.toString(), false);
    }
}
