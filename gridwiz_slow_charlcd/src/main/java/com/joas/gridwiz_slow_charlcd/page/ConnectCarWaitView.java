/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 3:33
 *
 */

package com.joas.gridwiz_slow_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.gridwiz_slow_charlcd.R;
import com.joas.gridwiz_slow_charlcd.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectCarWaitView extends LinearLayout implements com.joas.gridwiz_slow_charlcd.page.PageActivateListener {
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

        count = 30;

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                tvCounter.setText(""+count);
                if ( count == 0 )  {
                    stopTimer();
                    count = flowManager.getChargeData().authTimeout;
                    flowManager.onPageCommonEvent(com.joas.gridwiz_slow_charlcd.page.PageEvent.GO_HOME);
                    tvCounter.setText(""+count);
                }
            }
        });
    }

    void initComponents() {
        tvCounter = (TextView) findViewById(R.id.tvConnectCarCounter);

        // Test Code
        Button btNext= (Button) findViewById(R.id.btConnectCarWaitNext);
        btNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextClick();
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

    // for Test!! // To..Do.. Disable
    public void onNextClick() {
        flowManager.onDspChargingStartEvent();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        count = 30;
        tvCounter.setText(""+count);
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
