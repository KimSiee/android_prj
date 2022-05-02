/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.volvo_touch_2ch.ChargeData;
import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;

public class ChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    Activity mainActivity;
    ChargeData cData;

    TextView tvChargeKwhVal;
    TextView tvChargeTime;
    TextView tvRemainTime;
    Button btStopTag;


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
        tvChargeKwhVal = (TextView)findViewById(R.id.tvchgpwr);
        tvChargeTime = (TextView)findViewById(R.id.tvchgtime);
        tvRemainTime = (TextView)findViewById(R.id.tvremainTime);

        cData = flowManager.getChargeData();
        btStopTag = (Button)findViewById(R.id.btChgStop);
        btStopTag.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onChargingStop();
            }
        });
    }

    /**
     * 화면 정보를 업데이트한다.
     */
    void updateDispInfo() {
        ChargeData cData = flowManager.getChargeData();

        // 충전량 표시
        String measureVal = String.format("%.2f", (double) (cData.measureWh / 1000.0));
        tvChargeKwhVal.setText(measureVal);

        // 충전 시간 표시
        int timeSec = (int) (cData.chargingTime / 1000);
        String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60));
        tvChargeTime.setText(strTime);

        //add by si.20200528 - 차지비 충전 남은시간 표시
        int remainTimeSec = (int) (cData.chargingRemainTime / 1000);
        String strRemTime = String.format("%02d:%02d:%02d", remainTimeSec / 3600, (remainTimeSec % 3600) / 60, (remainTimeSec % 60));
        tvRemainTime.setText(strRemTime);
//        // 충전요금 - test
//        tvChargingCost.setText(""+cData.chargingCost+" 원");
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
        updateDispInfo();
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
        tvChargeKwhVal.setText("0.00");
        tvChargeTime.setText("00:00:00");
        tvRemainTime.setText("00:00:00");
    }

    public void onChargingStop() {
        //2채널 충전기의 경우 충전중지 버튼 눌렀을 때 해당 채널에 카드태깅화면 띄우기 위해 필요
        flowManager.setStateChargingStopOrNot();
    }
}
