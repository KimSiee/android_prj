/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:14
 *
 */

package com.joas.joasui_mobile_charger.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.joas.joasui_mobile_charger.ui.ChargeData;
import com.joas.joasui_mobile_charger.ui.R;
import com.joas.joasui_mobile_charger.ui.TypeDefine;
import com.joas.joasui_mobile_charger.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvRemainTime;
    TextView tvPercent;
    TextView tvChargingRemainTimeLabel;
    TextView tvMsgFinishing;
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
        tvPercent = (TextView)findViewById(R.id.tvPercentCharge);
        tvRemainTime = (TextView)findViewById(R.id.tvChargeRemainTime);
        tvChargingRemainTimeLabel = (TextView)findViewById(R.id.tvChargingRemainTimeLabel);

        progressBarCharge = (ProgressBar)findViewById(R.id.progressBarCharge);

        // Test Code
        Button btStop = (Button) findViewById(R.id.btChargingStop);
        btStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopClick();
            }
        });
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f kWh", (double)(cData.measureWh / 1000.0) );
        tvChargeKwhVal.setText( measureVal);

        // 충전 시간 표시
        int timeSec = (int)(cData.chargingTime / 1000);
        String strTime = String.format("%02d : %02d : %02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60) );
        tvChargeTime.setText(strTime);

        // Percent, SOC
        if ( cData.connectorType == TypeDefine.ConnectorType.AC3 ) { // soc, remaintime 삭제
            progressBarCharge.setProgress(socAminationVal);
            socAminationVal += 20;
            if ( socAminationVal > 100) socAminationVal = 0;
            tvPercent.setText("AC3상");
        }
        else {
            progressBarCharge.setProgress(cData.soc);
            tvPercent.setText(""+cData.soc+" %");
        }

        // 남은시간
        timeSec = (int)(cData.remainTime);
        strTime = String.format("%02d : %02d : %02d", timeSec / 60, timeSec % 60, 0 );
        tvRemainTime.setText(strTime);

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
            tvChargingRemainTimeLabel.setVisibility(VISIBLE);
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

        progressBarCharge.setProgress(0);
    }

    void onStopClick() {
        flowManager.onChargingStop();
        tvMsgFinishing.setVisibility(VISIBLE);

    }
}
