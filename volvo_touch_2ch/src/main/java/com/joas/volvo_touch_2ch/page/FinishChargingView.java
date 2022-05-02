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
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;


public class FinishChargingView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 10;

    TextView tvFinishChargingTime;
    TextView tvFinishChargingAmount;
    TextView tvFinishChargingCost;

    public FinishChargingView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_finish_charging, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                if ( count == 0 )  {
                    stopTimer();
                    flowManager.onPageCommonEvent(PageEvent.GO_HOME);
                }
            }
        });
    }
    void startTimer() {
        stopTimer();
        timer.start();
    }

    public void stopTimer() {
        if ( timer != null ) timer.cancel();
    }

    void initComponents() {
        tvFinishChargingTime = (TextView)findViewById(R.id.tvchgtime_finish);
        tvFinishChargingAmount = (TextView)findViewById(R.id.tvchgpwr_finish);
        tvFinishChargingCost = (TextView)findViewById(R.id.tvChgCost_finish);

        Button btHome = (Button) findViewById(R.id.btHome_finishchg);

        btHome.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onHomeClick();
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
        tvFinishChargingAmount.setText(measureVal);

        // 충전 시간 표시
        int timeSec = (int) (cData.chargingTime / 1000);
        String strTime = String.format("%02d:%02d:%02d", timeSec / 3600, (timeSec % 3600) / 60, (timeSec % 60));
        tvFinishChargingTime.setText(strTime);

        //충전요금 표시
        Volvo2chChargerInfo pinfo = flowManager.getVolvoChargerInfo();
        String chargingcost = "";
        chargingcost = String.valueOf(pinfo.curChargingCost);
        tvFinishChargingCost.setText("*충전금액 : " + chargingcost + "원");
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        updateDispInfo();
        count = 20;     //20초뒤 사라짐
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

    public void onHomeClick() {
        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
    }
}
