/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 8. 31 오후 3:55
 *
 */

package com.joas.kepco.ui_charlcd.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui_charlcd.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

public class AuthPayWaitView extends LinearLayout implements PageActivateListener {
    UIFlowManager flowManager;
    TimeoutTimer timer = null;
    int count = 0;
    TextView tvCounter;
    TextView tvAuthPayWaitLabel;
    Activity mainActivity;
    ImageView imageAuthWait;

    Animation animAuthWait;

    public AuthPayWaitView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_auth_pay_wait, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                tvCounter.setText(""+count);
                if ( count == 0 )  {
                    timer.cancel();
                    count = flowManager.getChargeData().authTimeout;
                    flowManager.onAuthPayTimeout();
                    tvCounter.setText(""+count);
                }
            }
        });

        count = flowManager.getChargeData().authTimeout;
    }

    void initComponents() {
        tvCounter = (TextView) findViewById(R.id.tvAuthPayWaitCounter);
        imageAuthWait = (ImageView) findViewById(R.id.imageAuthPayWait);
        tvAuthPayWaitLabel = findViewById(R.id.tvAuthPayWaitLabel);

        animAuthWait = AnimationUtils.loadAnimation(
                getContext(), // 현재 화면의 제어권자
                R.anim.ani_waiting);    // 설정한 에니메이션 파일

    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        count = flowManager.getChargeData().connectCarTimeout;
        tvCounter.setText(""+count);
        if ( flowManager.getChargeData().isAuthCredit ) {
            tvAuthPayWaitLabel.setText("신용카드 승인 중입니다.");
        }
        else {
            tvAuthPayWaitLabel.setText("회원 인증 중입니다");
        }
        timer.start();
        imageAuthWait.startAnimation(animAuthWait);
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
        animAuthWait.cancel();
    }
}
