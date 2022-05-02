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
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joas.minsu_ui.CPConfig;
import com.joas.minsu_ui.R;
import com.joas.minsu_ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class ConnectCarWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    int animationCnt = 0;
//    TextView tvCounter;
    ImageView imageViewConnectorwait;
    Activity mainActivity;

    public ConnectCarWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connect_car_wait, this, true);

        initComponents();
        CPConfig cpConfig = flowManager.getCpConfig();

        count = 30;

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                doConnectorImageChange();
//                tvCounter.setText(""+count);
                if ( count == 0 )  {
                    stopTimer();
                    if(cpConfig.isTestMode){
                        flowManager.onDspChargingStartEvent();      //test mode일경우 5초뒤 충전시작화면으로 이동
                    }
                    else{
                        count = flowManager.getChargeData().authTimeout;
                        flowManager.onPageCommonEvent(PageEvent.GO_HOME);
                    }

//                    tvCounter.setText(""+count);
                }
            }
        });
    }

    void initComponents() {
//        tvCounter = (TextView) findViewById(R.id.tvConnectCarCounter);
        imageViewConnectorwait = (ImageView) findViewById(R.id.imgview_connectorwait);
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
        CPConfig cpConfig = flowManager.getCpConfig();
        if(cpConfig.isTestMode) count = 5;
        else count = 30;

        startTimer();
    }

    @Override
    public void onPageDeactivate() {
        stopTimer();
    }

    public void doConnectorImageChange(){
        if(++animationCnt > 2) animationCnt = 0;

        switch (animationCnt)
        {
            case 0:
                imageViewConnectorwait.setBackground(ContextCompat.getDrawable(this.getContext(),R.drawable.icon_04_01));
                break;
            case 1:
                imageViewConnectorwait.setBackground(ContextCompat.getDrawable(this.getContext(),R.drawable.icon_04_02));
                break;
            case 2:
                imageViewConnectorwait.setBackground(ContextCompat.getDrawable(this.getContext(),R.drawable.icon_04_03));
                break;
        }
    }
}
