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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.volvo_touch_2ch.R;
import com.joas.volvo_touch_2ch.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

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

        count = flowManager.getChargeData().authTimeout;
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
        count = flowManager.getChargeData().connectCarTimeout;
        tvCounter.setText(""+count);
        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }
}
