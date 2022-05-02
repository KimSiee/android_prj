/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 30 오후 4:19
 *
 */

package com.joas.ocppui_dubai_slow.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.ocppui_dubai_slow.R;
import com.joas.ocppui_dubai_slow.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectCarWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    TextView tvCounter;
    Activity mainActivity;

    public ConnectCarWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connect_car_wait, this, true);

        initComponents();

        count = flowManager.getOcppConfigConnectorTimeout();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                tvCounter.setText(""+count);
                if ( count == 0 )  {
                    stopTimer();
                    count = flowManager.getChargeData().authTimeout;
                    flowManager.onPageCommonEvent(PageEvent.GO_HOME);
                    tvCounter.setText(""+count);
                }
            }
        });
    }

    void initComponents() {
        tvCounter = (TextView) findViewById(R.id.tvConnectCarCounter);

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
        count = flowManager.getOcppConfigConnectorTimeout();
        tvCounter.setText(""+count);
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
