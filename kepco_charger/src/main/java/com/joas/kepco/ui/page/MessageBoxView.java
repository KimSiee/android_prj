/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 7. 13 오후 3:36
 *
 */

package com.joas.kepco.ui.page;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.joas.kepco.ui.R;
import com.joas.kepco.ui.UIFlowManager;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.util.Timer;
import java.util.TimerTask;

public class MessageBoxView extends FrameLayout  implements PageActivateListener {
    TimeoutTimer timer = null;
    Activity mainActivity;
    int timerCount = 0;
    UIFlowManager flowManager;
    ImageButton btOk;
    TextView tvRetryMsg;

    public MessageBoxView(Context context, UIFlowManager manager, Activity activity) {
        super(context);
        mainActivity = activity;
        flowManager = manager;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.page_message_box, this, true);

        btOk = (ImageButton)findViewById(R.id.btMsgBoxOK);
        final View viewThis = this;
        btOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                viewThis.setVisibility(INVISIBLE);
                flowManager.playVoiceCurState();
                timer.cancel();
            }
        });

        tvRetryMsg =findViewById(R.id.tvRetryMsg);

        timer = new TimeoutTimer(1000, new TimeoutHandler() {
            @Override
            public void run() {
                timerCount--;
                if ( timerCount <= 0 ) {
                    viewThis.setVisibility(INVISIBLE);
                    if ( timer != null ) timer.cancel();
                    flowManager.playVoiceCurState();
                }
            }
        });

    }

    void initMessageBox() {
        TextView tvMessageBoxTitle = findViewById(R.id.tvMessageBoxTitle);
        tvMessageBoxTitle.setText(flowManager.getChargeData().messageBoxTitle);
        TextView tvMessageBoxContent = findViewById(R.id.tvMessageBoxContent);
        tvMessageBoxContent.setText(flowManager.getChargeData().messageBoxContent );

        if ( flowManager.getChargeData().messageBoxOkBtUse == false ) {
            btOk.setVisibility(INVISIBLE);
        }
        else {
            btOk.setVisibility(VISIBLE);
        }

        if ( flowManager.getChargeData().messageBoxRetryVisible == false) {
            tvRetryMsg.setVisibility(INVISIBLE);
        }
        else {
            tvRetryMsg.setVisibility(VISIBLE);
        }

        startTimer();
    }

    void startTimer() {
        timerCount = flowManager.getChargeData().messageBoxTimeout;
        timer.start();
    }


    // 화면에 나타날때 처리함
    @Override
    public void onPageActivate() {
        initMessageBox();
    }

    @Override
    public void onPageDeactivate() {
        timer.cancel();
    }
}