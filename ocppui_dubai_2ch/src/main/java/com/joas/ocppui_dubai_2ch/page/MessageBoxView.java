/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 13 오후 1:38
 *
 */

package com.joas.ocppui_dubai_2ch.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.joas.ocppui_dubai_2ch.R;
import com.joas.ocppui_dubai_2ch.UIFlowManager;
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
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            viewThis .setVisibility(INVISIBLE);
                        }
                    });
                    if ( timer != null ) timer.cancel();
                }
            }
        });
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
        timerCount = flowManager.getChargeData().messageBoxTimeout;
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