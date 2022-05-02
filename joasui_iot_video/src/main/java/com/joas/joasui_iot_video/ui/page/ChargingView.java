/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 12. 20 오전 8:38
 *
 */

package com.joas.joasui_iot_video.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.joasui_iot_video.ui.ChargeData;
import com.joas.joasui_iot_video.ui.R;
import com.joas.joasui_iot_video.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvChargeCost;

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
        tvChargeKwhVal = (TextView)findViewById(R.id.tvChargeKwhVal);
        tvChargeTime = (TextView)findViewById(R.id.tvChargeTime);
        tvChargeCost = (TextView)findViewById(R.id.tvChargeCost);

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

        tvChargeCost.setText((int)cData.chargingCost+"원");
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
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

    void onStopClick() {
        flowManager.onChargingStop();
    }
}
