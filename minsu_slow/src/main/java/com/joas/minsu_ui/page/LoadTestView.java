/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 9. 13. 오전 9:46
 *
 */

package com.joas.minsu_ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.minsu_ui.CPConfig;
import com.joas.minsu_ui.ChargeData;
import com.joas.minsu_ui.PageManger;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.UIFlowManager;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class LoadTestView extends FrameLayout {
    Activity mainActivity;
    UIFlowManager uiManager;
    TimeoutTimer timer = null;

    //components
    TextView tvStat;
    Button btComboStart;
    Button btChgStop;
    TextView tvPowerMeter;
    TextView tvUsePower;
    TextView tvUsepay;
    TextView tvinputVoltageCmd;
    TextView tvoutputVoltage;
    TextView tvinputCurrCmd;
    TextView tvoutputCurr;
    TextView tvCostInfo;
    Button btloadCostInfo;
    Button bthideKeyboard;
    Button btExit;
    Button btreqVoltageCmd;
    Button btreqCurrCmd;

    boolean runFlag = false;
    long lastMeterval = 0;
    String voltageCmdValue = "0";
    String currCmdValue = "0";

    public LoadTestView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        uiManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_loadtest, this, true);

        initComponents();

        timer = new TimeoutTimer(200, new TimeoutHandler() {
            @Override
            public void run() {
                updateDispInfo();
            }
        });
    }

    void initComponents() {
        tvStat = (TextView)findViewById(R.id.tvChgStatus);
        tvPowerMeter = (TextView)findViewById(R.id.tvpowermeterval);
        tvUsePower = (TextView)findViewById(R.id.tvUsepower);
        tvUsepay = (TextView)findViewById(R.id.tvPrice);
        tvinputVoltageCmd = (TextView)findViewById(R.id.edittext_voltageReqVal);
        tvoutputVoltage = (TextView)findViewById(R.id.tvOutputVoltage);
        tvinputCurrCmd = (TextView)findViewById(R.id.edittext_currReqVal);
        tvoutputCurr = (TextView)findViewById(R.id.tvOutputCurrent);
        tvCostInfo = (TextView)findViewById(R.id.tvCostInfo);

        btComboStart = (Button)findViewById(R.id.btStartCombo);
        btComboStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onComboTestStart();
            }
        });
        btChgStop = (Button)findViewById(R.id.btStop);
        btChgStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChargeStop();
            }
        });
        btloadCostInfo = (Button)findViewById(R.id.btGetCostInfo);
        btloadCostInfo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadCostInfo();
            }
        });
        bthideKeyboard = (Button)findViewById(R.id.bthideKeyboard);
        bthideKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHideKeyboard();
            }
        });
        btExit = (Button)findViewById(R.id.btExit);
        btExit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onExitLoadTest();
            }
        });
        btreqVoltageCmd = (Button)findViewById(R.id.btreqVoltageOrder);
        btreqVoltageCmd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetVoltageCmd();
            }
        });
        btreqCurrCmd = (Button)findViewById(R.id.btreqCurrOrder);
        btreqCurrCmd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetCurrCmd();
            }
        });
    }
    void onLoadCostInfo() {
        try{
            ChargeData chgdata = uiManager.getChargeData();
            CPConfig cpconfig = uiManager.getCpConfig();
            chgdata.chargingUnitCost = cpconfig.settingUnitCost;
            String dispstr = "단가정보 : " + "" + chgdata.chargingUnitCost + "원/kWh";
            tvCostInfo.setText(dispstr);
        }catch (Exception e){}

    }
    void onSetVoltageCmd() {
        try{
            if(runFlag){
                DSPControl2 dspControl2 = uiManager.getDspControl();
                //전압지령 전송
                voltageCmdValue = tvinputVoltageCmd.getText().toString();
                //전압지령
                float voltcmdval = Float.parseFloat(voltageCmdValue);
                dspControl2.setVoltageCmd(0, voltcmdval);

                Toast.makeText(mainActivity.getApplicationContext(), "전압지령 전송 성공 : " + voltageCmdValue, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(mainActivity.getApplicationContext(), "전압지령 전송 실패(충전중에만 가능)", Toast.LENGTH_SHORT).show();

        }catch (Exception e){
            LogWrapper.d("LOAD_TEST", "Voltage set cmd error occured!");
        }

    }
    void onSetCurrCmd() {
        try{
            if(runFlag){
                DSPControl2 dspControl2 = uiManager.getDspControl();
                //전류지령 전송
                currCmdValue = tvinputCurrCmd.getText().toString();
                //전류지령
                float currcmdval = Float.parseFloat(currCmdValue);
                dspControl2.setAmpareCmd(0, currcmdval);
                Toast.makeText(mainActivity.getApplicationContext(), "전류지령 전송 성공: " + currCmdValue, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(mainActivity.getApplicationContext(), "전류지령 전송 실패(충전중에만 가능)", Toast.LENGTH_SHORT).show();

        }catch (Exception e){
            LogWrapper.d("LOAD_TEST", "Ampare set cmd error occured!");
        }

    }

    void onChargeStop(){
        DSPControl2 dspctrl = uiManager.getDspControl();
        ChargeData chgdata = uiManager.getChargeData();

        runFlag = false;
        dspctrl.setState200(0, DSPTxData2.STATUS200.START_CHARGE, false);
        dspctrl.setState200(0, DSPTxData2.STATUS200.READY, false);
        dspctrl.setState200(0, DSPTxData2.STATUS200.FINISH_CHARGE, true);

        //message 표시
        tvStat.setText("중지");
        Toast.makeText(mainActivity.getApplicationContext(), "충전중지",Toast.LENGTH_SHORT).show();
    }

    void onComboTestStart(){
        DSPControl2 dspctrl = uiManager.getDspControl();
        ChargeData chgdata = uiManager.getChargeData();

        dspctrl.setConnectorSelect(chgdata.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCOMBO_TEST);
        dspctrl.setState200(chgdata.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspctrl.setState200(chgdata.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspctrl.setState200(chgdata.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);

        chgdata.measureWh = 0;
        chgdata.chargingCost = 0;
        lastMeterval = chgdata.meterVal;
        runFlag = true;

        //message 표시
        tvStat.setText("콤보시작");
        Toast.makeText(mainActivity.getApplicationContext(), "콤보시작",Toast.LENGTH_SHORT).show();

    }

    void onExitLoadTest(){
        PageManger pm = uiManager.getPageManager();
        pm.hideLoadTestView();
    }

    void onHideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            init();
        }
        else {
            //타이머 중지
            stopTimer();
            //test flag 초기화
            DSPControl2 dspctrl = uiManager.getDspControl();
            ChargeData chgdata = uiManager.getChargeData();
            dspctrl.setState200(chgdata.dspChannel, DSPTxData2.STATUS200.UI_LOAD_TEST,false);
        }
    }


    void init() {
        runFlag = false;
        DSPControl2 dspControl2 = uiManager.getDspControl();
        ChargeData chgdata = uiManager.getChargeData();

        dspControl2.setState200(chgdata.dspChannel, DSPTxData2.STATUS200.UI_LOAD_TEST, true);
        tvStat.setText("중지");
        tvUsePower.setText("충전량 : " + chgdata.measureWh + " kWh");
        tvUsepay.setText("충전요금 : " + chgdata.chargingCost + " 원");
        startTimer();
    }
    void startTimer() {
        stopTimer();
        timer.start();
    }
    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        try {
            ChargeData chargeData = uiManager.getChargeData();
            DSPControl2 dspcontrol2 = uiManager.getDspControl();

            //전력량계 값
            String str_meterval = "전력량계 : " + "" + String.format("%.2f kWh", (double) (chargeData.meterVal / 1000.0));
            tvPowerMeter.setText(str_meterval);

            if (runFlag) {        //충전중일때
                getMetervalProcess();
                //사용량
                String usepwr = "충전량 : " + "" + String.format("%.2f kWh", (double) (chargeData.measureWh / 1000.0));
                tvUsePower.setText(usepwr);

                //사용요금
                chargeData.chargingCost = ((double) chargeData.measureWh / 1000.0) * (double) chargeData.chargingUnitCost;
                int final_chargingcost = (int)chargeData.chargingCost;
                String dispStr = String.format("충전요금: %d원", final_chargingcost);
                tvUsepay.setText(dispStr);

                //전압지령
                float voltcmdval = Float.parseFloat(voltageCmdValue);
                dspcontrol2.setVoltageCmd(chargeData.dspChannel, voltcmdval);

                //전류지령
                float currcmdval = Float.parseFloat(currCmdValue);
                dspcontrol2.setAmpareCmd(chargeData.dspChannel, currcmdval);

            } else {
                //전압지령(205,206) 초기화
                dspcontrol2.setVoltageCmd(chargeData.dspChannel, 0);
                //전류지령(207,208) 초기화
                dspcontrol2.setAmpareCmd(chargeData.dspChannel, 0);
            }

            //출력전압 표시
            String str_outputvolt = String.format("출력전압(DC) : %.1f V",chargeData.outputVoltage);
            tvoutputVoltage.setText(str_outputvolt);

            //출력전류 표시
            String str_outputcurr = String.format("출력전류(DC) : %.1f A",chargeData.outputCurr);
            tvoutputCurr.setText(str_outputcurr);
        } catch (Exception e) {
            LogWrapper.e("LOAD_TEST", e.toString());
        }

    }

    void getMetervalProcess(){
        try {
            ChargeData chgdata = uiManager.getChargeData();
            if (chgdata.meterVal >= 0) {
                if (runFlag == true) {
                    if (lastMeterval > 0) {
                        int gapMeter = (int) (chgdata.meterVal - lastMeterval);

                        if (gapMeter > 0) {
                            chgdata.measureWh += gapMeter;
                        }
                    }
                }
                lastMeterval = chgdata.meterVal;
            } else {
                // Meter Error !!!
                LogWrapper.d("LOAD_TEST", "Meter Read ERROR");
            }
        }catch (Exception e){}
    }
}
