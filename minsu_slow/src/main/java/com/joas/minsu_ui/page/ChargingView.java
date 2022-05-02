/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joas.minsu_comm.MinsuChargerInfo;
import com.joas.minsu_ui.CPConfig;
import com.joas.minsu_ui.ChargeData;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.TypeDefine;
import com.joas.minsu_ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvChargeCost;
    TextView tvMsgFinishing;
    TextView tvRemainTime;
    TextView tvChargingRemainTimeLabel;

    TextView tvPercent;
    ProgressBar progressBarCharge;
    int socAminationVal = 0;

    public ChargingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_charging, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                updateDispInfo();
            }
        });
    }

    void initComponents() {
        tvMsgFinishing = (TextView)findViewById(R.id.tvMsgFinishing);

        tvChargeKwhVal = (TextView)findViewById(R.id.tvChargeKwhVal);
        tvChargeTime = (TextView)findViewById(R.id.tvChargeTime);
        tvChargeCost = (TextView)findViewById(R.id.tvChargeCost);
        tvRemainTime = (TextView)findViewById(R.id.tvRemainTime);

        tvPercent = (TextView)findViewById(R.id.tvPercentCharge);
//        progressBarCharge = (ProgressBar)findViewById(R.id.progressBarCharge);
//        tvChargingRemainTimeLabel = (TextView)findViewById(R.id.tvChargingRemainTimeLabel);

        ImageButton btChargingStop = (ImageButton)findViewById(R.id.btChargingStop);
        btChargingStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChargingStopReq();
            }
        });


    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.isTestMode){}
        else{
            ChargeData cData = flowManager.getChargeData();

            // 충전량 표시
            String measureVal = String.format("%.2f", (double)(cData.measureWh / 1000.0) );
            tvChargeKwhVal.setText( measureVal);

            // 충전 시간 표시
            int timeSec = (int)(cData.chargingTime / 1000);
            String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
            tvChargeTime.setText(strTime);



            // Percent, SOC
            if ( cData.connectorType == TypeDefine.ConnectorType.AC3 ) { // soc, remaintime 삭제
                progressBarCharge.setProgress(socAminationVal);
                socAminationVal += 20;
                if ( socAminationVal > 100) socAminationVal = 0;
                tvPercent.setText("");
            }
            else {
//                progressBarCharge.setProgress(cData.soc);
                tvPercent.setText(""+cData.soc+" %");

                //충전경과시간 10초일 경우 시작 soc 저장
                int sec = (timeSec%60);
                if(sec==10){
                    MinsuChargerInfo minfo = flowManager.getMinsuChargerInfo();
                    minfo.start_socBatt = (byte)cData.soc;
                }

                // 남은시간
                timeSec = (int)(cData.remainTime);
                strTime = String.format("%02d:%02d:%02d", timeSec / 60, timeSec % 60, 0 );
                tvRemainTime.setText(strTime);
            }

            tvChargeCost.setText((int)cData.chargingCost+"");
        }

    }

    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        ChargeData cData = flowManager.getChargeData();
        if ( cData.connectorType == TypeDefine.ConnectorType.AC3 ) { // soc, remaintime 삭제
            tvRemainTime.setVisibility(INVISIBLE);
            tvChargingRemainTimeLabel.setVisibility(INVISIBLE);
            socAminationVal = 0;

            updateDispInfo();
        }
        else {
            tvRemainTime.setVisibility(VISIBLE);
//            tvChargingRemainTimeLabel.setVisibility(VISIBLE);
        }
        tvMsgFinishing.setVisibility(INVISIBLE);


        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();

        tvChargeKwhVal.setText("0 kWh");
        tvChargeTime.setText("00 : 00 : 00");
        tvPercent.setText("0 %");
        tvRemainTime.setText("00 : 00 : 00");
//
//        progressBarCharge.setProgress(0);
    }

    void onStopClickTag() {
        flowManager.onCardTagEvent("1010010071615448", true);
    }

    void onChargingStopReq(){
        flowManager.onChargingStop();
    }


    //충전중지중 안내 메시지 표시 함수
    public void onChargingStop() {

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMsgFinishing.setVisibility(VISIBLE);
            }
        });
    }
}
