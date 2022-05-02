/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:14
 *
 */

package com.joas.joasui_mobile_charger.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.joasui_mobile_charger.ui.R;
import com.joas.joasui_mobile_charger.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class MessageBoxView extends FrameLayout {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    UIFlowManager flowManager;

    public MessageBoxView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_message_box, this, true);

        final View viewThis = this;
        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount == 0 ) {
                    viewThis .setVisibility(INVISIBLE);
                }
            }
        });
        timerCount = flowManager.getChargeData().messageBoxTimeout;
    }

    void initMessageBox() {
        TextView tvMessageBoxTitle = findViewById(R.id.tvMessageBoxTitle);
        tvMessageBoxTitle.setText(flowManager.getChargeData().messageBoxTitle);
        TextView tvMessageBoxContent = findViewById(R.id.tvMessageBoxContent);
        tvMessageBoxContent.setText(flowManager.getChargeData().messageBoxContent );
        startTimer();
    }

    void startTimer() {
        if ( timer != null ) timer.cancel();
        timer.start();

    }

    // 화면에 나타날때 처리함
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ( visibility == VISIBLE ) {
            initMessageBox();
        }
        else {
            if ( timer != null ) timer.cancel();
        }
    }

}