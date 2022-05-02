/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 17 오후 12:04
 *
 */

package com.joas.kevcs.ui.page;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.joas.kevcs.ui.R;
import com.joas.kevcs.ui.TypeDefine;
import com.joas.kevcs.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class UnPlugView extends LinearLayout implements PageActivateListener {

    UIFlowManager flowManager;
    Activity mainActivity;
    private AnimationDrawable frameAnimation;
    private ImageView view;
    TimeoutTimer timer = null;
    int count = TypeDefine.UI_UNPLUG_TO_HOME_TIMEOUT;

    public UnPlugView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        flowManager = manager;
        mainActivity = activity;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_unplug, this, true);

        initComponents();

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                count--;
                boolean isUnplug = (flowManager.getIsDspPlug() == false);
                if ( flowManager.getChargeData().connectorType != TypeDefine.ConnectorType.BTYPE) {
                    isUnplug = isUnplug && (flowManager.getIsPlugOrgPosition() == true);
                }
                if ( count == 0 || isUnplug)  {
                    timer.cancel();
                    flowManager.onPageCommonEvent(PageEvent.COMMON_GO_HOME);
                }
            }
        });
    }

    void initComponents() {

        // 컨트롤 ImageView 객체를 가져온다
        view = (ImageView) findViewById(R.id.imageUnPlug);
        // 이미지를 동작시키기위해  AnimationDrawable 객체를 가져온다.
        frameAnimation = (AnimationDrawable) view.getBackground();
    }

    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        frameAnimation.start();
        count = TypeDefine.UI_UNPLUG_TO_HOME_TIMEOUT;
        timer.start();
    }

    @Override
    public void onPageDeactivate() {
        frameAnimation.stop();
        timer.cancel();
    }
}
