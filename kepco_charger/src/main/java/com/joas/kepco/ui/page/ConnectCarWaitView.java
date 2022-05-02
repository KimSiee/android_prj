/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 17 오전 11:07
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.TypeDefine;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectCarWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    TextView tvCounter;
    TextView tvConnectCarWaitBType;
    Activity mainActivity;
    ImageView imageAuthWait;
    Animation animAuthWait;

    public ConnectCarWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_connect_car_wait, this, true);

        count = flowManager.getChargeData().authTimeout;

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                tvCounter.setText(""+count);
                if ( count == 0 )  {
                    timer.cancel();
                    count = flowManager.getChargeData().authTimeout;
                    flowManager.onConnectCarWaitTimeout();
                    tvCounter.setText(""+count);
                }
            }
        });
    }

    void initComponents() {
        tvCounter = (TextView) findViewById(R.id.tvConnectCarWaitCounter);
        tvConnectCarWaitBType = (TextView) findViewById(R.id.tvConnectCarWaitBType);
        imageAuthWait = (ImageView) findViewById(R.id.imageConnectCarWait);

        animAuthWait = AnimationUtils.loadAnimation(
                getContext(), // 현재 화면의 제어권자
                R.anim.ani_waiting);    // 설정한 에니메이션 파일
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        count = flowManager.getChargeData().connectCarTimeout;
        tvCounter.setText(""+count);
        timer.start();
        imageAuthWait.startAnimation(animAuthWait);

        if (flowManager.getChargeData().connectorType == TypeDefine.ConnectorType.BTYPE) {
            tvConnectCarWaitBType.setVisibility(VISIBLE);
        }
        else {
            tvConnectCarWaitBType.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
        animAuthWait.cancel();
        tvConnectCarWaitBType.setVisibility(INVISIBLE);
    }
}
